package org.example.util;

import jakarta.annotation.PostConstruct;
import org.example.dto.view.*;
import org.example.entity.view.*;
import org.example.mapper.*;
import org.example.model.GeneralInformation;
import org.example.repository.hibernate.ConfigParamRepository;
import org.example.repository.hibernate.MenuParamRepository;
import org.example.repository.hibernate.view.*;
import org.example.repository.jdbc.DelegationsJdbcRepository;
import org.example.repository.jdbc.UserPersonasJdbcRepository;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ViewInfoSchema {
    public HashMap<String, String> viewAndPath = new HashMap<>();
    public HashMap<Object, Object> entityAndDto = new HashMap<>();
    public HashMap<String, GeneralInformation> nameAndValuesMap = new HashMap<>();
    public HashMap<String, Object> queryAndRepo = new HashMap<>();


    @PostConstruct
    void SetNameAndValues() {
        nameAndValuesMap.put("user-profile", // TODO -
                new GeneralInformation(UserProfileEntity.class, UserProfileDto.class, UserProfileMapper.class, UserProfileRepository.class));
        nameAndValuesMap.put("org-profile",
                new GeneralInformation(OrgProfileEntity.class, OrgProfileDto.class, OrgProfileMapper.class, OrgProfileRepository.class));
        nameAndValuesMap.put("delegations",
                new GeneralInformation(DelegationEntity.class, DelegationDto.class, DelegationMapper.class, DelegationRepository.class));
        nameAndValuesMap.put("user-assignments",
                new GeneralInformation(UserAssignmentsEntity.class, UserAssignmentsDto.class, UserAssignmentsMapper.class, UserAssignmentsRepository.class));
        nameAndValuesMap.put("my-request",
                new GeneralInformation(MyRequestEntity.class, MyRequestDto.class, MyRequestMapper.class, MyRequestRepository.class));
        nameAndValuesMap.put("approval-role",
                new GeneralInformation(ApprovalRoleInfoEntity.class, ApprovalRoleInfoDto.class, ApprovalRoleInfoMapper.class, ApprovalRoleInfoRepository.class));
        nameAndValuesMap.put("case-info",
                new GeneralInformation(CaseInfoEntity.class, CaseInfoDto.class, CaseInfoMapper.class, CaseInfoRepository.class));
        nameAndValuesMap.put("org-profile-ext",
                new GeneralInformation(OrgProfileExtDto.class, OrgProfileExtEntity.class, OrgProfileExtMapper.class, OrgProfileExtRepository.class));
        nameAndValuesMap.put("object-info",
                new GeneralInformation(ObjectInfoLiteEntity.class, ObjectInfoLiteDto.class, ObjectInfoLiteMapper.class, ObjectInfoLiteRepository.class));
        nameAndValuesMap.put("approval-request",
                new GeneralInformation(ApprovalRequestEntity.class, ApprovalRequestDto.class, ApprovalRequestMapper.class, ApprovalRequestRepository.class));
        nameAndValuesMap.put("cert-requests",
                new GeneralInformation(AccessCertActiveRequestEntity.class, AccessCertActiveRequestDto.class, AccessCertActiveRequestMapper.class, AccessCertActiveRequestRepository.class));
        nameAndValuesMap.put("accounts",
                new GeneralInformation(AccountsEntity.class, AccountsDto.class, AccountsMapper.class, AccountsRepository.class));
        nameAndValuesMap.put("config-param",
                new GeneralInformation(ConfigParamEntity.class, ConfigParamDto.class, ConfigParamMapper.class, ConfigParamRepository.class));
        nameAndValuesMap.put("menu-param",
                new GeneralInformation(MenuParamEntity.class, MenuParamDto.class, MenuParamMapper.class, MenuParamRepository.class));
        nameAndValuesMap.put("account-attributes",
                new GeneralInformation(AccountAttributeEntity.class, AccountAttributeDto.class, AccountAttributeMapper.class, AccountAttributeRepository.class));
//        nameAndValuesMap.put("user-profile-ext",
//                new GeneralInformation(UserProfileExtEntity.class, .class, .class, .class));
        nameAndValuesMap.put("user-assignments-graf",
                new GeneralInformation(UserAssignmentsGrafEntity.class, UserAssignmentsGrafDto.class, UserAssignmentsGrafMapper.class, UserAssignmentsGrafRepository.class));
        nameAndValuesMap.put("user-assignments",
                new GeneralInformation(UserAssignmentsEntity.class, UserAssignmentsDto.class, UserAssignmentsMapper.class, UserAssignmentsRepository.class));
    }

    @PostConstruct
    void setValuesForView(){ // TODO НЕ удалять, скопировать, названия вью из http запроса
        viewAndPath.put("case-info", "glt_case_info_v");
        viewAndPath.put("org-profile-ext", "glt_get_org_profile_ext");
        viewAndPath.put("delegations", "glt_delegations_v"); // *
        viewAndPath.put("object-info", "glt_object_info_lite");
        viewAndPath.put("approval-role", "glt_approval_role_info");
        viewAndPath.put("approval-request", "glt_approval_request_v");
        viewAndPath.put("cert-requests", "glt_access_cert_active_request_v");
        viewAndPath.put("accounts", "glt_get_accounts_v");
        viewAndPath.put("config-param", "d_config_param_v");
        viewAndPath.put("menu-param", "d_menu_param_v");
        viewAndPath.put("account-attributes", "glt_get_accountattributes_v");
        viewAndPath.put("user-profile-ext", "glt_get_user_profile_ext");
        viewAndPath.put("my-requests", "glt_my_request_v");
        viewAndPath.put("org-profile", "glt_get_org_profile");
        viewAndPath.put("user-personas", "glt_user_personas_v");
        viewAndPath.put("user-assignments-graf", "glt_user_assignments_graf_v");
        viewAndPath.put("user-assignments", "glt_user_assignments_v");
        viewAndPath.put("user-profile", "glt_get_user_profile");
    }

    // TODO сделать отдельный класс который хранит маппер дто энтити и репо для каждого рест сервиса и получать из
    // TODO него данные на основании названия http запроса
    @PostConstruct
    void SetValuesForEntityAndDto(){
        entityAndDto.put(CaseInfoDto.class, CaseInfoEntity.class);
        entityAndDto.put(OrgProfileExtDto.class, OrgProfileExtEntity.class);
        entityAndDto.put(DelegationDto.class, DelegationEntity.class);
        entityAndDto.put(ObjectInfoLiteDto.class, ObjectInfoLiteEntity.class);
        entityAndDto.put(ApprovalRoleInfoDto.class, ApprovalRoleInfoEntity.class);
        entityAndDto.put(ApprovalRequestDto.class, ApprovalRequestEntity.class);
        entityAndDto.put(AccessCertActiveRequestDto.class, AccessCertActiveRequestEntity.class);
        entityAndDto.put(AccountsDto.class, AccountsEntity.class);
        entityAndDto.put(ConfigParamDto.class, ConfigParamEntity.class);
        entityAndDto.put(MenuParamDto.class, MenuParamEntity.class);
        entityAndDto.put(AccountAttributeDto.class, AccountAttributeEntity.class);
//        entityAndDto.put(UserProfileEx, );
        entityAndDto.put(MyRequestDto.class, MyRequestEntity.class);
        entityAndDto.put(OrgProfileDto.class, OrgProfileEntity.class);
        entityAndDto.put(UserPersonasDto.class, UserPersonasEntity.class);
        entityAndDto.put(UserAssignmentsGrafDto.class, UserAssignmentsGrafEntity.class);
        entityAndDto.put(UserAssignmentsDto.class, UserAssignmentsEntity.class);
        entityAndDto.put(UserProfileDto.class, UserProfileEntity.class);
    }

    @PostConstruct
    void setQueryAndRepo(){
        entityAndDto.put("user-personas", UserPersonasJdbcRepository.class);
        entityAndDto.put("delegations", DelegationsJdbcRepository.class);
    }

}
