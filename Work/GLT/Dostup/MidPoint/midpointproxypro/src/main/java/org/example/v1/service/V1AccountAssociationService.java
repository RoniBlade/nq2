package org.example.v1.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.filter.FilterNode;
import org.example.model.filter.FilterRequest;
import org.example.util.field.DtoFieldTrimmer;
import org.example.v1.repository.JdbcRepository;
import org.slf4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class V1AccountAssociationService {
    private final String FUNCTION_NAME = "d_get_accounts_association";
    private final String targetFieldName = "array_field_names";
    private final JdbcRepository jdbcRepo;
    private final String fieldNameToChange = "data_json";

    private String targetValue;
    private final StructureService structureService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Page<Map<String, Object>> getAccountAssociation(Pageable pageable, String param, FilterRequest request, String arrayName) {

        List<FilterNode> filters = (request != null && request.getFilters() != null)
                ? request.getFilters() : Collections.emptyList();
        List<String> include = (request != null) ? request.getFields() : null;
        List<String> exclude = (request != null) ? request.getExcludeFields() : null;

        return structureService.fetch(FUNCTION_NAME, arrayName, param, filters, null, include, exclude, pageable);
    }

//    public Page<Map<String, Object>> getAccountAssociation(Pageable pageable, String param) {
//
//        List<Map<String, Object>> row = jdbcRepo.getOneFromFunction(FUNCTION_NAME, param);
//
//        List<Map<String, Object>> content = row.stream()
//                .map(r -> DtoFieldTrimmer.trimMap(r, null, null))
//                .toList();
//        return new PageImpl<>(content, pageable, content.size());
//    }


//    public Page<Map<String, Object>> getAccountAssociationTest(Pageable pageable, String param){
//
//        List<Map<String, Object>> row = jdbcRepo.getOneFromFunction(FUNCTION_NAME, param);
//        List<Map<String, Object>> result = new ArrayList<>();
//        try {
//
//            Map<String, Object> newEntry = new HashMap<>();
//
//            for(Map.Entry<String, Object> map : row.get(0).entrySet()){
//
//                if(map.getKey().equals(targetFieldName)){
//                    targetValue = map.getValue().toString();
//                    log.info("TARGET VALUE  :  " + targetValue);
//                }
//            }
//            for(Map.Entry<String, Object> map : row.get(0).entrySet()){
//                if(!map.getKey().equals(fieldNameToChange)){
//                    newEntry.put(map.getKey(), map.getValue());
//                }
//                else {
//                    String resultValueField = map.getValue().toString();
//                    String resulNameField = map.getKey();
//                    String jsonStr = resultValueField;
//
//                    ObjectMapper mapper = new ObjectMapper();
//                    JsonNode rootNode = mapper.readTree(jsonStr);
//
//                    JsonNode groupsNode = rootNode.get("groups");
//
//                    if (groupsNode != null) {
//
//                        newEntry.put("oid", groupsNode.get("oid").asText());
//                        newEntry.put("relation", groupsNode.get("relation").asText());
//                        newEntry.put("type", groupsNode.get("type").asText());
//                    }
//                }
//            }
//            result.add(newEntry);
//        }
//        catch (JsonProcessingException ex){
//
//        }
//        List<Map<String, Object>> content = result.stream()
//                .map(r -> DtoFieldTrimmer.trimMap(r, null, null))
//                .toList();
//
//        return new PageImpl<>(content, pageable, content.size());
//    }

}
