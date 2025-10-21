package org.example.util.field;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class DtoFieldTrimmer {

    public static Map<String, Object> trimMap(Map<String, Object> row,
                                              List<String> includeFields,
                                              List<String> excludeFields) {
        if (row == null) return Map.of();

        Set<String> include = includeFields != null
                ? includeFields.stream().filter(f -> f != null && !f.isBlank()).collect(Collectors.toSet())
                : null;

        Set<String> exclude = excludeFields != null
                ? excludeFields.stream().filter(f -> f != null && !f.isBlank()).collect(Collectors.toSet())
                : Set.of();

        Map<String, Object> result = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (key == null) continue;
            if (include != null && !include.contains(key)) continue;
            if (exclude.contains(key)) continue;

            result.putIfAbsent(key, value); // сохранить null как значение — ОК
        }

        return result;
    }

    public static <T> T trim(T source, List<String> includedFields, List<String> excludedFields) {
        if (source == null) return null;

        try {
            Class<?> clazz = source.getClass();
            T trimmed = (T) clazz.getDeclaredConstructor().newInstance();
            Set<String> include = includedFields != null ? new HashSet<>(includedFields) : null;
            Set<String> exclude = excludedFields != null ? new HashSet<>(excludedFields) : Set.of();

            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                String name = field.getName();
                if ((include == null || include.contains(name)) && !exclude.contains(name)) {
                    Object value = field.get(source);
                    field.set(trimmed, value);
                }
            }
            return trimmed;
        } catch (Exception e) {
            throw new RuntimeException("Failed to trim DTO fields", e);
        }
    }

}
