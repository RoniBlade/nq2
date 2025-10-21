package org.example.util.filter;

import jakarta.persistence.Column;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.example.model.filter.FilterNode;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class FilterSpecificationBuilder {

    public static <T> Specification<T> build(Object userOid,
                                             List<FilterNode> filterNodes,
                                             Class<T> entityClass,
                                             String userOidFieldName) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (userOidFieldName != null) {
                predicates.add(cb.equal(root.get(userOidFieldName), userOid));
            }

            if (filterNodes != null) {
                for (FilterNode node : filterNodes) {
                    predicates.add(processNode(node, root, cb));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static <T> Predicate processNode(FilterNode node, Root<T> root, CriteriaBuilder cb) {
        if (node.isLeaf()) {
            String originalField = node.getCriterion().getField();

            // Валидируем по Java-полям и @Column-именам
            FilterFieldValidator.validateField(originalField, root.getJavaType());

            // Попробуем заменить SQL-имя на Java-поле, если нужно
            String resolvedField = resolveJavaFieldName(originalField, root.getJavaType());
            node.getCriterion().setField(resolvedField);

            return PredicateBuilder.buildPredicate(node.getCriterion(), root, cb);
        }

        if (node.getLogic() == null || node.getFilters() == null || node.getFilters().isEmpty()) {
            throw new IllegalArgumentException("FilterNode with logic=\"" + node.getLogic() + "\" has no children");
        }

        List<Predicate> childPredicates = node.getFilters().stream()
                .map(child -> processNode(child, root, cb))
                .toList();

        return switch (node.getLogic().toLowerCase()) {
            case "and" -> cb.and(childPredicates.toArray(new Predicate[0]));
            case "or" -> cb.or(childPredicates.toArray(new Predicate[0]));
            default -> throw new IllegalArgumentException("Unknown logic operator: " + node.getLogic());
        };
    }

    private static <T> String resolveJavaFieldName(String inputField, Class<T> entityClass) {
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.getName().equals(inputField)) {
                return inputField; // это уже Java-поле
            }
            Column column = field.getAnnotation(Column.class);
            if (column != null && column.name().equals(inputField)) {
                return field.getName(); // маппинг найден
            }
        }
        return inputField; // если не найден — пусть останется, Hibernate сам отловит
    }



}
