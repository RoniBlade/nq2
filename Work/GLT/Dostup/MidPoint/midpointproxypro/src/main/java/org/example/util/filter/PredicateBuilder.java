package org.example.util.filter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.example.model.filter.FilterCriterion;
import org.example.model.filter.FilterOperation;

import java.util.Collection;

public class PredicateBuilder {

    public static <T> Predicate buildPredicate(FilterCriterion criterion, Root<T> root, CriteriaBuilder cb) {
        String field = criterion.getField();
        FilterOperation op = criterion.getOp();
        Object value = criterion.getValue();
        Class<?> fieldType = root.get(field).getJavaType();

        System.out.println("DEBUG: field=" + field + ", op=" + op + ", value=" + value + ", type=" + (value == null ? "null" : value.getClass()));

        boolean isString = String.class.isAssignableFrom(fieldType);

        return switch (op) {
            case EQUAL -> {
                Object converted = convertValue(value, fieldType);
                System.out.println("[PREDICATE] field: " + field + ", op: " + op +
                        ", value: " + value + " (" + (value == null ? "null" : value.getClass().getSimpleName()) + ")" +
                        ", fieldType: " + fieldType.getSimpleName() +
                        ", converted: " + converted + " (" + (converted == null ? "null" : converted.getClass().getSimpleName()) + ")");

                if (fieldType.isEnum()) {
                    // Для enum, явно кастим поле к строке, чтобы Postgres не ругался на objecttype = character varying
                    yield cb.equal(root.get(field).as(String.class), converted.toString());
                }
                yield isString
                        ? cb.equal(cb.lower(root.get(field)), converted.toString().toLowerCase())
                        : cb.equal(root.get(field), converted);
            }
            case NOT_EQUAL -> {
                Object converted = convertValue(value, fieldType);
                yield isString
                        ? cb.notEqual(cb.lower(root.get(field)), converted.toString().toLowerCase())
                        : cb.notEqual(root.get(field), converted);
            }
            case GREATER_THAN -> createComparablePredicate(root, cb, field, fieldType, value, true);
            case LESS_THAN -> createComparablePredicate(root, cb, field, fieldType, value, false);
            case GREATER_OR_EQUAL -> createBoundedPredicate(root, cb, field, fieldType, value, true);
            case LESS_OR_EQUAL -> createBoundedPredicate(root, cb, field, fieldType, value, false);
            case IN -> buildInPredicate(root, cb, field, fieldType, value);
            case NOT_IN -> cb.not(buildInPredicate(root, cb, field, fieldType, value));
            case SUBSTRING -> {
                Object converted = convertValue(value, fieldType);
                yield cb.like(cb.lower(root.get(field)), "%" + converted.toString().toLowerCase() + "%");
            }
            case NOT_SUBSTRING -> {
                Object converted = convertValue(value, fieldType);
                yield cb.notLike(cb.lower(root.get(field)), "%" + converted.toString().toLowerCase() + "%");
            }
            case STARTS_WITH -> {
                Object converted = convertValue(value, fieldType);
                yield cb.like(cb.lower(root.get(field)), converted.toString().toLowerCase() + "%");
            }
            case ENDS_WITH -> {
                Object converted = convertValue(value, fieldType);
                yield cb.like(cb.lower(root.get(field)), "%" + converted.toString().toLowerCase());
            }
            case NOT_NULL -> {
                yield cb.isNotNull(root.get(field));
            }
            case IS_NULL -> {
                yield cb.isNull(root.get(field));
            }
            default -> throw new UnsupportedOperationException("Операция " + op + " не поддерживается");
        };

    }

    private static Object convertValue(Object value, Class<?> targetType) {
        return FilterValueConverter.convert(value, targetType);
    }

    private static <T> Predicate createComparablePredicate(Root<T> root,
                                                           CriteriaBuilder cb,
                                                           String field,
                                                           Class<?> fieldType,
                                                           Object value,
                                                           boolean isGreater) {
        validateComparable(field, fieldType);
        Comparable<Object> compValue = toComparable(value, fieldType);
        return isGreater
                ? cb.greaterThan(root.get(field), compValue)
                : cb.lessThan(root.get(field), compValue);
    }

    private static <T> Predicate createBoundedPredicate(Root<T> root,
                                                        CriteriaBuilder cb,
                                                        String field,
                                                        Class<?> fieldType,
                                                        Object value,
                                                        boolean isGreaterOrEqual) {
        validateComparable(field, fieldType);
        Comparable<Object> compValue = toComparable(value, fieldType);
        return isGreaterOrEqual
                ? cb.greaterThanOrEqualTo(root.get(field), compValue)
                : cb.lessThanOrEqualTo(root.get(field), compValue);
    }

    private static <T> Predicate buildInPredicate(Root<T> root,
                                                  CriteriaBuilder cb,
                                                  String field,
                                                  Class<?> fieldType,
                                                  Object value) {
        var inClause = cb.in(root.get(field));
        if (value instanceof Collection<?> col) {
            for (Object item : col) {
                inClause.value(convertValue(item, fieldType));
            }
        } else {
            inClause.value(convertValue(value, fieldType));
        }
        return inClause;
    }

    private static void validateComparable(String field, Class<?> fieldType) {
        if (!Comparable.class.isAssignableFrom(fieldType)) {
            throw new IllegalArgumentException("Поле " + field + " не поддерживает сравнение");
        }
    }

    @SuppressWarnings("unchecked")
    private static Comparable<Object> toComparable(Object value, Class<?> fieldType) {
        return (Comparable<Object>) convertValue(value, fieldType);
    }
}
