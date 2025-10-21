// GltTestRestController.java (окончательная версия)
package com.glt.idm.rest.Test;

import com.evolveum.midpoint.rest.impl.AbstractRestController;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.RestAuthorizationAction;
import com.evolveum.midpoint.security.api.RestHandlerMethod;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.glt.idm.rest.dao.RequestViewDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/glt")
public class GltTestRestController extends AbstractRestController {

    private static final Set<String> ALLOWED_PARAMS = Set.of(
            "caseOID", "caseDateCreate", "objecttype", "objectName",
            "objectDisplayName", "objectDescription", "requesterName",
            "requesterFullName", "requestertitle", "requesterorganization",
            "requesteremail", "requesterPhone", "closetime", "state",
            "targetoid", "targetnamenorm"
    );

    @Autowired
    private RequestViewDao requestViewDao;

    @RestHandlerMethod(authorization = RestAuthorizationAction.GET_OBJECTS)
    @GetMapping(value = "/requests", produces = "application/json; charset=UTF-8")
    public ResponseEntity<?> getRequestsFromView(
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam Map<String, String> allParams) {

        Task task = initRequest();
        OperationResult result = createSubresult(task, "getRequestsFromView");

        try {
            String userOid = SecurityUtil.getPrincipalOidIfAuthenticated();
            if (userOid == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication required"));
            }

            Map<String, String> filters = allParams.entrySet().stream()
                    .filter(e -> ALLOWED_PARAMS.contains(e.getKey()) && StringUtils.hasText(e.getValue()))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue().trim()
                    ));

            List<Map<String, Object>> data = requestViewDao.getRequestsFromView(offset, size, filters, userOid);

            return ResponseEntity.ok()
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .body(Map.of(
                            "success", true,
                            "data", data,
                            "total", data.size()
                    ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Server error",
                            "details", e.getMessage()
                    ));
        } finally {
            result.recordSuccessIfUnknown();
            finishRequest(task, result);
        }
    }

    @PostMapping(value = "/requests", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> filterRequests(
            @RequestBody Map<String, Object> filter,
            @RequestParam(name = "offset") int offset,
            @RequestParam(name = "size") int size) {
        try {
            List<Map<String, Object>> data = requestViewDao.getRequestsFiltered(offset, size, filter);
            int total = requestViewDao.getFilteredCount(filter);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", data);
            response.put("total", total);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResp = new HashMap<>();
            errorResp.put("success", false);
            errorResp.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResp);
        }
    }

}
