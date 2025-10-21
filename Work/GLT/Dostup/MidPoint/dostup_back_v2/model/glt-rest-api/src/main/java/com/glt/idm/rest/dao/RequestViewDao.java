package com.glt.idm.rest.dao;

import java.util.List;
import java.util.Map;

public interface RequestViewDao {
    List<Map<String, Object>> getRequestsFiltered(int offset, int size, Map<String, Object> filter);
    int getFilteredCount(Map<String, Object> filter);
    List<Map<String, Object>> getRequestsFromView(int offset, int size, Map<String, String> filters, String userOid);
}
