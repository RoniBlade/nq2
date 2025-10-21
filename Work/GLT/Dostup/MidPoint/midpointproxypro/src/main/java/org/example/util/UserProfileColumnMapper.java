package org.example.util;

import jakarta.persistence.Column;
import org.example.dto.view.UserProfileDto;
import org.example.entity.view.UserProfileEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class UserProfileColumnMapper {

    private static final Map<String, String> columnToDtoFieldMap = new HashMap<>();

    static {
        buildMapping();
    }

    private static void buildMapping() {
        Map<String, String> entityColumnToField = new HashMap<>();

        for (Field field : UserProfileEntity.class.getDeclaredFields()) {
            Column column = field.getAnnotation(Column.class);
            if (column != null) {
                entityColumnToField.put(column.name(), field.getName());
            }
        }

        for (Field dtoField : UserProfileDto.class.getDeclaredFields()) {
            String dtoFieldName = dtoField.getName();
            if (entityColumnToField.containsValue(dtoFieldName)) {
                String columnName = entityColumnToField.entrySet().stream()
                        .filter(e -> e.getValue().equals(dtoFieldName))
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse(null);
                if (columnName != null) {
                    columnToDtoFieldMap.put(columnName, dtoFieldName);
                }
            }
        }
    }

    public static Pageable mapSortFromSqlColumn(Pageable pageable) {
        Sort mappedSort = Sort.by(pageable.getSort().stream()
                .map(order -> {
                    String dtoField = UserProfileColumnMapper.resolveDtoFieldFromColumn(order.getProperty());
                    if (dtoField == null) {
                        throw new IllegalArgumentException("Поле сортировки '" + order.getProperty() +
                                "' не поддерживается или отсутствует в DTO");
                    }
                    return new Sort.Order(order.getDirection(), dtoField);
                })
                .collect(Collectors.toList()));

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), mappedSort);
    }

    public static String resolveDtoFieldFromColumn(String columnName) {
        return columnToDtoFieldMap.get(columnName);
    }
}
