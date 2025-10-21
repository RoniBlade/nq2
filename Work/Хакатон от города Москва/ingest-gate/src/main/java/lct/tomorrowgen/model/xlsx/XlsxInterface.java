package lct.tomorrowgen.model.xlsx;

import java.lang.reflect.Field;

public interface XlsxInterface {
     default void setField(int index, Object value) {
        Field[] fields = this.getClass().getDeclaredFields();

        if (index >= fields.length || index < 0) {
            throw new IndexOutOfBoundsException("Index is out of bounds");
        }

        Field field = fields[index];
        field.setAccessible(true);

        try {
            if (value instanceof Number) {
                if (field.getType() == Integer.class) {
                    field.set(this, ((Number)value).intValue());
                } else if (field.getType() == Long.class) {
                    field.set(this, ((Number)value).longValue());
                } else if (field.getType() == Float.class) {
                    field.set(this, ((Number)value).floatValue());
                } else if (field.getType() == Double.class) {
                    field.set(this, ((Number)value).doubleValue());
                } else {
                    field.set(this, value);
                }
            } else if (value instanceof String) {
                if (field.getType() == Boolean.class) {
                    field.set(this, Boolean.parseBoolean((String)value));
                } else {
                    field.set(this, value);
                }
            } else {
                field.set(this, value);
            }
        } catch (IllegalAccessException | IllegalArgumentException ex) {
            throw new RuntimeException("Error while setting field with index " + index, ex);
        }
    }
}