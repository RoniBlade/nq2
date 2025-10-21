package org.example.service.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.GroupDto;
import org.example.entity.view.AccountAssociationViewEntity;
import org.example.entity.view.AssociationObjectViewEntity;
import org.example.repository.hibernate.view.AccountAssociationViewRepository;
import org.example.repository.hibernate.view.AssociationObjectViewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class AssociationService {

    private static final Logger log = LoggerFactory.getLogger(AssociationService.class);

    private final AccountAssociationViewRepository accountRepo;
    private final AssociationObjectViewRepository objectViewRepo;
    private final ObjectMapper objectMapper;

    public AssociationService(AccountAssociationViewRepository accountRepo,
                              AssociationObjectViewRepository objectViewRepo) {
        this.accountRepo = accountRepo;
        this.objectViewRepo = objectViewRepo;
        this.objectMapper = new ObjectMapper();
    }

    public List<GroupDto> getGroupsByOid(UUID oid) {
        log.info("[getGroupsByOid] Requested OID: {}", oid);

        AccountAssociationViewEntity entity = accountRepo.findByOid(oid)
                .orElseThrow(() -> {
                    log.warn("[getGroupsByOid] No association found for OID: {}", oid);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Account association not found for oid: " + oid);
                });

        String rawJson = entity.getEntitlements();
        if (rawJson == null || rawJson.isBlank()) {
            log.info("[getGroupsByOid] Empty entitlements JSON");
            return Collections.emptyList();
        }

        Map<UUID, String> oidToGroupName = new HashMap<>();
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            Iterator<Map.Entry<String, JsonNode>> fields = root.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String rawGroupName = entry.getKey(); // e.g. "#Departments"
                String groupName = rawGroupName.replaceFirst("^#", "");
                JsonNode value = entry.getValue();

                if (value.isArray()) {
                    for (JsonNode node : value) {
                        JsonNode oidNode = node.get("oid");
                        if (oidNode != null && oidNode.isTextual()) {
                            UUID groupOid = UUID.fromString(oidNode.asText());
                            oidToGroupName.put(groupOid, groupName);
                        }
                    }
                } else if (value.has("oid")) {
                    UUID groupOid = UUID.fromString(value.get("oid").asText());
                    oidToGroupName.put(groupOid, groupName);
                }
            }
        } catch (Exception e) {
            log.error("[getGroupsByOid] Failed to parse entitlements JSON: {}", rawJson, e);
            throw new RuntimeException("Failed to parse entitlements JSON", e);
        }

        if (oidToGroupName.isEmpty()) {
            log.info("[getGroupsByOid] No group oids extracted");
            return Collections.emptyList();
        }

        List<UUID> groupOids = new ArrayList<>(oidToGroupName.keySet());
        List<AssociationObjectViewEntity> objects = objectViewRepo.findAllByOidIn(groupOids);

        List<GroupDto> result = new ArrayList<>();
        for (AssociationObjectViewEntity object : objects) {
            UUID groupOid = object.getOid();
            String groupName = oidToGroupName.get(groupOid);
            result.add(new GroupDto(groupOid, object.getDisplayName(), groupName));
        }

        log.info("[getGroupsByOid] Result size: {}", result.size());
        return result;
    }
}
