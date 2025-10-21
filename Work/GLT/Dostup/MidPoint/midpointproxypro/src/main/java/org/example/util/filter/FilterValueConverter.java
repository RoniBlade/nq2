package org.example.util.filter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class FilterValueConverter {

    private static final Map<Class<?>, Function<Object, Object>> CONVERTERS = new HashMap<>();

    static {
        CONVERTERS.put(Long.class, v -> Long.parseLong(v.toString()));
        CONVERTERS.put(long.class, v -> Long.parseLong(v.toString()));
        CONVERTERS.put(Integer.class, v -> Integer.parseInt(v.toString()));
        CONVERTERS.put(int.class, v -> Integer.parseInt(v.toString()));
        CONVERTERS.put(LocalDate.class, v -> LocalDate.parse(v.toString()));
        CONVERTERS.put(LocalDateTime.class, v -> LocalDateTime.parse(v.toString()));
        CONVERTERS.put(UUID.class, v -> UUID.fromString(v.toString()));
    }

    @SuppressWarnings("unchecked")
    public static Object convert(Object value, Class<?> targetType) {
        if (value == null) return null;

        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }

        if (targetType.isEnum()) {
            return Enum.valueOf((Class<Enum>) targetType, value.toString().toUpperCase());
        }

        Function<Object, Object> converter = CONVERTERS.get(targetType);
        if (converter != null) {
            return converter.apply(value);
        }

        return value.toString(); // fallback
    }
}
