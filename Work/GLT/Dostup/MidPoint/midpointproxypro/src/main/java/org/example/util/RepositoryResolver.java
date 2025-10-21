package org.example.util;

import jakarta.persistence.Column;
import lombok.RequiredArgsConstructor;
import org.example.dto.view.RoleProfileDto;
import org.example.dto.view.RoleProfileExtDto;
import org.example.dto.view.UserProfileDto;
import org.example.dto.view.UserProfileExtDto;
import org.example.entity.*;
import org.example.entity.view.*;
import org.example.model.filter.FilterNode;
import org.example.model.filter.FilterOperation;
import org.example.repository.hibernate.*;
import org.example.repository.hibernate.view.*;
import org.example.repository.jdbc.*;
import org.example.v1.entity.DMenuParamEntity;
import org.example.v1.entity.EnumValueEntity;
import org.example.v1.entity.ObjectTypeFieldEntity;
import org.example.v1.repository.DMenuParamRepository;
import org.example.v1.repository.EnumValueRepository;
import org.example.v1.repository.ObjectTypeFieldRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RepositoryResolver {

    private final ConfigParamRepository configParamRepository;
    private final EnumValueRepository enumValueRepository;
    private final ObjectArchetypeFieldRepository objectArchetypeFieldRepository;
    private final ObjectTypeFieldRepository objectTypeFieldRepository;

    private final AccessCertActiveRequestRepository accessCertActiveRequestRepository;
    private final AccountAssociationViewRepository accountAssociationViewRepository;
    private final AccountAttributeRepository accountAttributeRepository;
    private final AccountsRepository accountsRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalRoleInfoRepository approvalRoleInfoRepository;
    private final AssociationObjectViewRepository associationObjectViewRepository;
    private final CaseInfoRepository caseInfoRepository;
    private final ConfigParamViewRepository configParamViewRepository;
    private final DelegationRepository delegationRepository;
    private final DMenuParamRepository dMenuParamRepository;
    private final MyRequestRepository myRequestRepository;
    private final ObjectInfoLiteRepository objectInfoLiteRepository;
    private final OrgProfileExtRepository orgProfileExtRepository;
    private final OrgProfileRepository orgProfileRepository;
    private final UserAssignmentsGrafRepository userAssignmentsGrafRepository;
    private final UserAssignmentsRepository userAssignmentsRepository;
    private final UserPersonasRepository userPersonasRepository;
    private final UserProfileExtRepository userProfileExtRepository;
    private final UserProfileRepository userProfileRepository;
    private final RoleProfileRepository roleProfileRepository;
    private final RoleProfileExtRepository roleProfileExtRepository;

    private final AccessCertActiveRequestJdbcRepository accessCertActiveRequestJdbcRepository;
    private final UserProfileExtJdbcRepository userProfileExtJdbcRepository;
    private final UserProfileJdbcRepository userProfileJdbcRepository;
    private final RoleProfileJdbcRepository roleProfileJdbcRepository;
    private final RoleProfileExtJdbcRepository roleProfileExtJdbcRepository;
    private final AccountAttributeJdbcRepository accountAttributeJdbcRepository;
    private final AccountsJdbcRepository accountsJdbcRepository;
    private final ApprovalRequestJdbcRepository approvalRequestJdbcRepository;
    private final ApprovalRoleInfoJdbcRepository approvalRoleInfoJdbcRepository;
    private final AssignRoleInfoJdbcRepository assignRoleInfoJdbcRepository;
    private final DelegationsJdbcRepository delegationsJdbcRepository;
    private final MyRequestJdbcRepository myRequestJdbcRepository;
    private final ObjectInfoLiteJdbcRepository objectInfoLiteJdbcRepository;
    private final OrgProfileExtJdbcRepository orgProfileExtJdbcRepository;
//    private final OrgProfileJdbc
    private final UserAssignmentsJdbcRepository userAssignmentsJdbcRepository;
    private final UserAssignmentsGrafJdbcRepository userAssignmentsGrafJdbcRepository;
    private final UserPersonasJdbcRepository userPersonasJdbcRepository;

    public List<?> executeQueryForViews(String tableName, Specification<?> spec, Pageable pageable, List<FilterNode> filters) {
        return switch (tableName) {
            case "config_param" -> configParamRepository.findAll((Specification<ConfigParamEntity>) spec, pageable).getContent();
            case "object_archetype_field" -> objectArchetypeFieldRepository.findAll((Specification<ObjectArchetypeFieldEntity>) spec, pageable).getContent();
            case "object_type_field" -> objectTypeFieldRepository.findAll((Specification<ObjectTypeFieldEntity>) spec, pageable).getContent();
            case "access_cert_active_request" -> accessCertActiveRequestRepository.findAll((Specification<AccessCertActiveRequestEntity>) spec, pageable).getContent();
            case "account_attribute" -> accountAttributeRepository.findAll((Specification<AccountAttributeEntity>) spec, pageable).getContent();
            case "accounts" -> accountsRepository.findAll((Specification<AccountsEntity>) spec, pageable).getContent();
            case "approval_request" -> approvalRequestRepository.findAll((Specification<ApprovalRequestEntity>) spec, pageable).getContent();
            case "approval_role_info" -> approvalRoleInfoRepository.findAll((Specification<ApprovalRoleInfoEntity>) spec, pageable).getContent();
            case "case_info" -> caseInfoRepository.findAll((Specification<CaseInfoEntity>) spec, pageable).getContent();
            case "config_param_view" -> configParamViewRepository.findAll((Specification<ConfigParamViewEntity>) spec, pageable).getContent();
            case "delegation" -> delegationRepository.findAll((Specification<DelegationEntity>) spec, pageable).getContent();
            case "menu_param", "d_menu_param" -> dMenuParamRepository.findAll((Specification<DMenuParamEntity>) spec, pageable).getContent();
            case "my_request" -> myRequestRepository.findAll((Specification<MyRequestEntity>) spec, pageable).getContent();
            case "object_info_lite" -> objectInfoLiteRepository.findAll((Specification<ObjectInfoLiteEntity>) spec, pageable).getContent();
            case "org_profile", "glt_get_org_profile" -> orgProfileRepository.findAll((Specification<OrgProfileEntity>) spec, pageable).getContent();
            case "org_profile_ext", "glt_get_org_profile_ext" -> orgProfileExtRepository.findAll((Specification<OrgProfileExtEntity>) spec, pageable).getContent();
            case "user_assignments_graf", "glt_user_assignments_graf_v" -> userAssignmentsGrafRepository.findAll((Specification<UserAssignmentsGrafEntity>) spec, pageable).getContent();
            case "user_assignments", "glt_user_assignments_v" -> userAssignmentsRepository.findAll((Specification<UserAssignmentsEntity>) spec, pageable).getContent();
            case "user_personas", "glt_user_personas_v" -> userPersonasRepository.findAll((Specification<UserPersonasEntity>) spec, pageable).getContent();
            case "user_profile", "glt_get_user_profile" -> userProfileJdbcRepository.findAll(pageable, filters).getContent();
            case "user_profile_ext", "glt_get_user_profile_ext" -> userProfileExtJdbcRepository.findAll(pageable, filters).getContent();
            case "glt_get_role_profile" -> roleProfileJdbcRepository.findAll(pageable, filters).getContent();
            case "glt_get_role_profile_ext" -> roleProfileExtJdbcRepository.findAll(pageable, filters).getContent();
            default -> throw new IllegalArgumentException("Неизвестная таблица: " + tableName);
        };
    }

    public Class<?> resolveEntityClass(String tableName) {
        return switch (tableName) {
            case "config_param" -> ConfigParamEntity.class;
            case "enum_value" -> EnumValueEntity.class;
            case "object_archetype_field" -> ObjectArchetypeFieldEntity.class;
            case "object_type_field" -> ObjectTypeFieldEntity.class;
            case "access_cert_active_request" -> AccessCertActiveRequestEntity.class;
            case "account_association_view" -> AccountAssociationViewEntity.class;
            case "account_attribute" -> AccountAttributeEntity.class;
            case "accounts" -> AccountsEntity.class;
            case "approval_request" -> ApprovalRequestEntity.class;
            case "approval_role_info" -> ApprovalRoleInfoEntity.class;
            case "association_object_view" -> AssociationObjectViewEntity.class;
            case "case_info" -> CaseInfoEntity.class;
            case "config_param_view" -> ConfigParamViewEntity.class;
            case "delegation" -> DelegationEntity.class;
            case "menu_param", "d_menu_param" -> MenuParamEntity.class;
            case "my_request" -> MyRequestEntity.class;
            case "object_info_lite" -> ObjectInfoLiteEntity.class;
            case "org_profile", "glt_get_org_profile" -> OrgProfileEntity.class;
            case "org_profile_ext", "glt_get_org_profile_ext" -> OrgProfileExtEntity.class;
            case "user_assignments_graf", "glt_user_assignments_graf_v" -> UserAssignmentsGrafEntity.class;
            case "user_assignments", "glt_user_assignments_v" -> UserAssignmentsEntity.class;
            case "user_personas", "glt_user_personas_v" -> UserPersonasEntity.class;
            case "user_profile", "glt_get_user_profile" -> UserProfileDto.class;
            case "user_profile_ext", "glt_get_user_profile_ext" -> UserProfileExtDto.class;
            case "glt_get_role_profile" -> RoleProfileDto.class;
            case "glt_get_role_profile_ext" -> RoleProfileExtDto.class;
            default -> throw new IllegalArgumentException("Неизвестная таблица для сущности: " + tableName);
        };
    }

    public String resolveTableName(Class<?> entityClass) {
        return switch (entityClass.getSimpleName()) {
            case "ConfigParamEntity" -> "config_param";
            case "EnumValueEntity" -> "enum_value";
            case "ObjectArchetypeFieldEntity" -> "object_archetype_field";
            case "ObjectTypeFieldEntity" -> "object_type_field";
            case "AccessCertActiveRequestEntity" -> "access_cert_active_request";
            case "AccountAssociationViewEntity" -> "account_association_view";
            case "AccountAttributeEntity" -> "account_attribute";
            case "AccountsEntity" -> "accounts";
            case "ApprovalRequestEntity" -> "approval_request";
            case "ApprovalRoleInfoEntity" -> "approval_role_info";
            case "AssociationObjectViewEntity" -> "association_object_view";
            case "CaseInfoEntity" -> "case_info";
            case "ConfigParamViewEntity" -> "config_param_view";
            case "DelegationEntity" -> "delegation";
            case "MenuParamEntity" -> "d_menu_param";
            case "MyRequestEntity" -> "my_request";
            case "ObjectInfoLiteEntity" -> "object_info_lite";
            case "OrgProfileEntity" -> "glt_get_org_profile";
            case "OrgProfileExtEntity" -> "glt_get_org_profile_ext";
            case "UserAssignmentsGrafEntity" -> "glt_user_assignments_graf_v";
            case "UserAssignmentsEntity" -> "glt_user_assignments_v";
            case "UserPersonasEntity" -> "glt_user_personas_v";
            case "UserProfileDto" -> "glt_get_user_profile";
            case "UserProfileExtDto" -> "glt_get_user_profile_ext";
            case "RoleProfileDto" -> "glt_get_role_profile";
            case "RoleProfileExtDto" -> "glt_get_role_profile_ext";

            default -> throw new IllegalArgumentException("Неизвестный entity-класс: " + entityClass.getSimpleName());
        };
    }

    public Specification<?> buildWhereClauseSpec(String whereClause, Class<?> entityClass) {
        return (root, query, cb) -> {
            if (whereClause == null || whereClause.isBlank()) {
                return cb.conjunction();
            }

            try {
                String[] parts = whereClause.split("=");
                if (parts.length != 2) {
                    throw new IllegalArgumentException("Неподдерживаемый формат whereClause: " + whereClause);
                }

                String rawField = parts[0].trim();
                String valueRaw = parts[1].trim();
                String valueStr = valueRaw.replaceAll("^['\"]|['\"]$", "");

                String resolvedField = resolveFieldName(rawField, entityClass);
                Class<?> fieldType = root.get(resolvedField).getJavaType();

                Object value;
                if (fieldType.equals(UUID.class)) {
                    value = UUID.fromString(valueStr);
                } else if (fieldType.equals(Long.class)) {
                    value = Long.parseLong(valueStr);
                } else if (fieldType.equals(Integer.class)) {
                    value = Integer.parseInt(valueStr);
                } else if (fieldType.equals(Boolean.class)) {
                    value = Boolean.parseBoolean(valueStr);
                } else {
                    value = valueStr;
                }

                return cb.equal(root.get(resolvedField), value);

            } catch (Exception e) {
                throw new RuntimeException("Ошибка при парсинге whereClause: " + whereClause, e);
            }
        };
    }

    public List<FilterNode> buildWhereClause(String whereClause) {
        if (whereClause == null || whereClause.isBlank()) return List.of();

        List<FilterNode> result = new ArrayList<>();

        // Допустим, условия разделены "AND"
        String[] expressions = whereClause.split("(?i)\\s+AND\\s+");

        for (String expr : expressions) {
            expr = expr.trim();

            // Примитивный парсинг. Можно расширить потом
            FilterOperation op;
            String field, value;

            if (expr.contains("!=")) {
                op = FilterOperation.NOT_EQUAL;
                String[] parts = expr.split("!=");
                field = parts[0].trim();
                value = unquote(parts[1].trim());
            } else if (expr.contains("=")) {
                op = FilterOperation.EQUAL;
                String[] parts = expr.split("=");
                field = parts[0].trim();
                value = unquote(parts[1].trim());
            } else if (expr.toUpperCase().contains("ILIKE")) {
                op = FilterOperation.SUBSTRING;
                String[] parts = expr.split("(?i)ILIKE");
                field = parts[0].trim();
                value = unquote(parts[1].trim()).replace("%", "");
            } else {
                throw new IllegalArgumentException("Неподдерживаемое условие: " + expr);
            }

            result.add(new FilterNode(field, op, value));
        }

        return result;
    }

    private static String unquote(String str) {
        return str.replaceAll("^['\"]|['\"]$", "");
    }


    private String resolveFieldName(String rawField, Class<?> entityClass) {
        for (Field field : entityClass.getDeclaredFields()) {
            // Сравнение с именем Java-поля
            if (field.getName().equalsIgnoreCase(rawField)) {
                return field.getName();
            }

            // Сравнение с @Column(name = ...)
            Column column = field.getAnnotation(Column.class);
            if (column != null && column.name().equalsIgnoreCase(rawField)) {
                return field.getName();
            }
        }

        throw new RuntimeException("Поле '" + rawField + "' не найдено в " + entityClass.getSimpleName());
    }

    public Page<?> executeQueryPaged(String tableName, Specification<?> spec, Pageable pageable, List<FilterNode> filters) {
        return switch (tableName) {
            case "config_param" -> configParamRepository.findAll((Specification<ConfigParamEntity>) spec, pageable);
            case "object_archetype_field" -> objectArchetypeFieldRepository.findAll((Specification<ObjectArchetypeFieldEntity>) spec, pageable);
            case "object_type_field" -> objectTypeFieldRepository.findAll((Specification<ObjectTypeFieldEntity>) spec, pageable);
            case "access_cert_active_request" -> accessCertActiveRequestRepository.findAll((Specification<AccessCertActiveRequestEntity>) spec, pageable);
            case "account_attribute" -> accountAttributeRepository.findAll((Specification<AccountAttributeEntity>) spec, pageable);
            case "accounts" -> accountsRepository.findAll((Specification<AccountsEntity>) spec, pageable);
            case "approval_request" -> approvalRequestRepository.findAll((Specification<ApprovalRequestEntity>) spec, pageable);
            case "approval_role_info" -> approvalRoleInfoRepository.findAll((Specification<ApprovalRoleInfoEntity>) spec, pageable);
            case "case_info" -> caseInfoRepository.findAll((Specification<CaseInfoEntity>) spec, pageable);
            case "config_param_view" -> configParamViewRepository.findAll((Specification<ConfigParamViewEntity>) spec, pageable);
            case "delegation" -> delegationRepository.findAll((Specification<DelegationEntity>) spec, pageable);
            case "menu_param", "d_menu_param" -> dMenuParamRepository.findAll((Specification<DMenuParamEntity>) spec, pageable);
            case "my_request" -> myRequestRepository.findAll((Specification<MyRequestEntity>) spec, pageable);
            case "object_info_lite" -> objectInfoLiteRepository.findAll((Specification<ObjectInfoLiteEntity>) spec, pageable);
            case "org_profile", "glt_get_org_profile" -> orgProfileRepository.findAll((Specification<OrgProfileEntity>) spec, pageable);
            case "org_profile_ext", "glt_get_org_profile_ext" -> orgProfileExtRepository.findAll((Specification<OrgProfileExtEntity>) spec, pageable);
            case "user_assignments_graf", "glt_user_assignments_graf_v" -> userAssignmentsGrafRepository.findAll((Specification<UserAssignmentsGrafEntity>) spec, pageable);
            case "user_assignments", "glt_user_assignments_v" -> userAssignmentsRepository.findAll((Specification<UserAssignmentsEntity>) spec, pageable);
            case "user_personas", "glt_user_personas_v" -> userPersonasRepository.findAll((Specification<UserPersonasEntity>) spec, pageable);
            case "user_profile", "glt_get_user_profile" -> userProfileJdbcRepository.findAll(pageable, filters);
            case "user_profile_ext", "glt_get_user_profile_ext" -> userProfileExtJdbcRepository.findAll(pageable, filters);
            case "glt_get_role_profile" -> roleProfileJdbcRepository.findAll(pageable, filters);
            case "glt_get_role_profile_ext" -> roleProfileExtJdbcRepository.findAll(pageable, filters);
            default -> throw new IllegalArgumentException("Неизвестная таблица: " + tableName);
        };
    }

    public Page<Map<String, Object>> executeQueryJdbcPaged(String tableName, Pageable pageable){
        return switch (tableName){
            case "cert-requests" -> accessCertActiveRequestJdbcRepository.findAll(pageable);
            case "accounts-attribute" -> accountAttributeJdbcRepository.findAll(pageable);
            case "accounts" -> accountsJdbcRepository.findAll(pageable);
            case "approval-requests" -> approvalRequestJdbcRepository.findAll(pageable);
//            case "approval-role" -> approvalRoleInfoJdbcRepository.findAll(pageable);
            case "assign-role-info" -> assignRoleInfoJdbcRepository.findAll(pageable);
            case "delegations" -> delegationsJdbcRepository.findAll(pageable);
            case "my-requests" -> myRequestJdbcRepository.findAll(pageable);
            case "object-info" -> objectInfoLiteJdbcRepository.findAll(pageable);
//            case "org-profile" -> org.findAll(pageable);
            case "org-profile-ext" -> orgProfileExtJdbcRepository.findAll(pageable);
//            case "role-profile" -> roleProfileJdbcRepository.findAll(pageable);
//            case "role-profile-ext" -> roleProfileExtJdbcRepository.findAll(pageable);
            case "user-assignments" -> userAssignmentsJdbcRepository.findAll(pageable);
//            case "user-assignments-graf" -> userAssignmentsGrafJdbcRepository.findAll(pageable);
            case "user-personas" -> userPersonasJdbcRepository.findAll(pageable);
//            case "user-profile" -> userProfileJdbcRepository.findAll(pageable);

            default -> throw new IllegalArgumentException("Неизвестная таблица: " + tableName);
        };
    }

    public Page<Map<String, Object>> executeQueryJdbcFilteredPaged(String tableName, Pageable pageable, List<FilterNode> filters){
        return switch (tableName){
            case "cert-requests" -> accessCertActiveRequestJdbcRepository.findByFilters(filters, pageable);
            case "accounts-attribute" -> accountAttributeJdbcRepository.findByFilters(filters, pageable);
            case "accounts" -> accountsJdbcRepository.findByFilters(filters, pageable);
            case "approval-requests" -> approvalRequestJdbcRepository.findByFilters(filters, pageable);
//            case "approval-role" -> approvalRoleInfoJdbcRepository.findAll();
            case "assign-role-info" -> assignRoleInfoJdbcRepository.findByFilters(filters, pageable);
            case "delegations" -> delegationsJdbcRepository.findByFilters(filters, pageable);
            case "my-requests" -> myRequestJdbcRepository.findByFilters(filters, pageable);
            case "object-info" -> objectInfoLiteJdbcRepository.findByFilters(filters, pageable);
//            case "org-profile" -> org.findAll(pageable);
            case "org-profile-ext" -> orgProfileExtJdbcRepository.findByFilters(filters, pageable);
//            case "role-profile" -> roleProfileJdbcRepository.findAll(pageable, filters);
//            case "role-profile-ext" -> roleProfileExtJdbcRepository.findAll(pageable);
            case "user-assignments" -> userAssignmentsJdbcRepository.findByFilters(filters, pageable);
//            case "user-assignments-graf" -> userAssignmentsGrafJdbcRepository.findAll(pageable);
            case "user-personas" -> userPersonasJdbcRepository.findByFilters(filters, pageable);
//            case "user-profile" -> userProfileJdbcRepository.findAll(pageable);

            default -> throw new IllegalArgumentException("Неизвестная таблица: " + tableName);
        };
    }


    @SuppressWarnings("unchecked")
    public <T> Page<T> executeQueryPaged(Class<T> entityClass, Specification<T> spec, Pageable pageable, List<FilterNode> filters) {
        String tableName = resolveTableName(entityClass);
        return (Page<T>) executeQueryPaged(tableName, spec, pageable, filters);
    }


    @SuppressWarnings("unchecked")
    public <T> List<T> executeQueryForViews(Class<T> entityClass, Specification<T> spec, List<FilterNode> filters) {
        String tableName = resolveTableName(entityClass);
        Pageable unpaged = Pageable.unpaged();
        return (List<T>) executeQueryForViews(tableName, spec, unpaged, filters);
    }

    public List<?> executeUntypedQuery(Class<?> entityClass, Specification<?> spec, List<FilterNode> filters) {
        String tableName = resolveTableName(entityClass);
        return executeQueryForViews(tableName, spec, Pageable.unpaged(), filters);
    }

}
