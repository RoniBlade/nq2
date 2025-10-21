package org.example.service.schema;

import lombok.extern.slf4j.Slf4j;
import org.example.model.filter.FilterRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SchemaService {

    private final CacheManager cacheManager;

    @Autowired
    public SchemaService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Возвращает отфильтрованную и отсортированную схему из кэша.
     */
    public Map<String, String> getSchemaFor(String objectType, FilterRequest request) {
        log.debug("Loading schema for {} from cache", objectType);

        Map<String, String> fullSchema = cacheManager.getCache("schema")
                .get(objectType, Map.class);

        if (fullSchema == null || fullSchema.isEmpty()) {
            log.warn("Schema for {} not found in cache", objectType);
            return Collections.emptyMap();
        }

        List<String> fields = request != null ? request.getFields() : null;
        List<String> excludeFields = request != null ? request.getExcludeFields() : null;

        // Фильтрация
        Map<String, String> filtered = fullSchema.entrySet().stream()
                .filter(entry -> fields == null || fields.contains(entry.getKey()))
                .filter(entry -> excludeFields == null || !excludeFields.contains(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        return filtered;
    }
}
