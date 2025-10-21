package com.glt.idm.rest.dto;

import java.util.HashMap;
import java.util.Map;

public class RequestViewFilterDto {
    private String caseOID;
    private String caseDateCreate;
    private String objecttype;
    private String objectName;
    private String objectDisplayName;
    private String objectDescription;
    private String requesterName;
    private String requesterFullName;
    private String requestertitle;
    private String requesterorganization;
    private String requesteremail;
    private String requesterPhone;
    private String closetime;
    private String state;
    private String targetoid;
    private String targetnamenorm;

    public Map<String, String> toFilterMap() {
        Map<String, String> map = new HashMap<>();
        if (caseOID != null) map.put("caseOID", caseOID);
        if (caseDateCreate != null) map.put("caseDateCreate", caseDateCreate);
        if (objecttype != null) map.put("objecttype", objecttype);
        if (objectName != null) map.put("objectName", objectName);
        if (objectDisplayName != null) map.put("objectDisplayName", objectDisplayName);
        if (objectDescription != null) map.put("objectDescription", objectDescription);
        if (requesterName != null) map.put("requesterName", requesterName);
        if (requesterFullName != null) map.put("requesterFullName", requesterFullName);
        if (requestertitle != null) map.put("requestertitle", requestertitle);
        if (requesterorganization != null) map.put("requesterorganization", requesterorganization);
        if (requesteremail != null) map.put("requesteremail", requesteremail);
        if (requesterPhone != null) map.put("requesterPhone", requesterPhone);
        if (closetime != null) map.put("closetime", closetime);
        if (state != null) map.put("state", state);
        if (targetoid != null) map.put("targetoid", targetoid);
        if (targetnamenorm != null) map.put("targetnamenorm", targetnamenorm);
        return map;
    }
}
