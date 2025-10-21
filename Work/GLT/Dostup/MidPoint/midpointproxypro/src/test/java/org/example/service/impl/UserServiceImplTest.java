//package org.example.service.impl;
//
//import jakarta.persistence.Column;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import org.example.client.MidPointClient;
//import org.example.dto.UserFieldDto;
//import org.example.v1.entity.EnumValueEntity;
//import org.example.v1.entity.ObjectTypeFieldEntity;
//import org.example.entity.view.UserProfileEntity;
//import org.example.entity.view.UserProfileExtEntity;
//import org.example.v1.repository.EnumValueRepository;
//import org.example.v1.repository.ObjectTypeFieldRepository;
//import org.example.repository.RefValueRepository;
//import org.example.repository.hibernate.view.UserProfileExtRepository;
//import org.example.repository.hibernate.view.UserProfileRepository;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.*;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class UserServiceImplTest {
//
//    @Mock private ObjectTypeFieldRepository objectFieldRepo;
//    @Mock private UserProfileRepository userProfileRepo;
//    @Mock private UserProfileExtRepository userProfileExtRepo;
//    @Mock private RefValueRepository refValueRepo;
//    @Mock private EnumValueRepository enumRepo;
//    @Mock private MidPointClient midPointClient;
//
//    @InjectMocks private UserServiceImpl userService;
//
//    @Test
//    void getUserFieldTemplates_shouldReturn() {
//        ObjectTypeFieldEntity field = getObjectFieldEntity();
//        when(objectFieldRepo.findByObjectTypeAndSend("UserType", true)).thenReturn(List.of(field));
//        when(enumRepo.findByEnumType(any())).thenReturn(List.of());
//
//        Map<String, Object> result = userService.getUserFieldTemplates(null, null);
//
//        List<?> fields = (List<?>) result.get("columns");
//
//        assertEquals(1, fields.size());
//        UserFieldDto dto = (UserFieldDto) fields.get(0);
//        assertEquals("department", dto.getName());
//        assertEquals("ref", dto.getType());
//    }
//
//    @Test
//    void parseValue_shouldReturnList() {
//        String type = "List<PolyStringType>";
//        String rawValue = "[{\"o\":\"value\"},{\"o\":\"value1\"}]";
//        Object result = userService.parseValue(type, rawValue);
//        assertEquals(List.of("value", "value1"), result);
//    }
//
//    @Test
//    void parseValue_shouldReturnEmptyList() {
//        String type = "List<PolyStringType>";
//        String rawValue = "[]";
//        Object result = userService.parseValue(type, rawValue);
//        assertEquals(List.of(), result);
//    }
//
//    @Test
//    void getEnumValues() {
//        EnumValueEntity enum1 = new EnumValueEntity();
//        enum1.setEnumValue("ONE");
//        EnumValueEntity enum2 = new EnumValueEntity();
//        enum2.setEnumValue("TWO");
//        when(enumRepo.findByEnumType("TestEnum")).thenReturn(List.of(enum1, enum2));
//        List<String> result = userService.getEnumValues("someField", "TestEnum");
//        assertEquals(List.of("ONE", "TWO"), result);
//    }
//
//    @Test
//    void testGetUserFields() {
//        UUID uuid = UUID.randomUUID();
//        ObjectTypeFieldEntity field = getObjectFieldEntity();
//        when(objectFieldRepo.findByObjectTypeAndSend("UserType", true)).thenReturn(List.of(field));
//
//        UserProfileEntity profile = new UserProfileEntity();
//        profile.setId(1);
//        profile.setOid(uuid);
//        profile.setName("orig");
//        when(userProfileRepo.findByOid(uuid)).thenReturn(Optional.of(profile));
//
//        UserProfileExtEntity ext = new UserProfileExtEntity();
//        ext.setOid(uuid);
//        ext.setExtAttrName("department_id");
//        ext.setExtAttrValue("dep123");
//        when(userProfileExtRepo.findAllByOid(uuid)).thenReturn(List.of(ext));
//
//        when(refValueRepo.getDisplayName("dep123", "Departments")).thenReturn("displayName");
//        when(enumRepo.findByEnumType(any())).thenReturn(List.of());
//
//        Map<String, Object> result = userService.getUserFields(uuid, null, null);
//        List<UserFieldDto> fields = (List<UserFieldDto>) result.get("columns");
//
//        assertEquals(1, fields.size());
//        UserFieldDto dto = fields.get(0);
//        assertEquals("department", dto.getName());
//        assertEquals("ref", dto.getType());
//        assertEquals("dep123", dto.getValue());
//        assertEquals("displayName", dto.getDisplayNameValue());
//    }
//
//    @Test
//    void testGetUserFieldsOnlyForExt() {
//        UUID oid = UUID.randomUUID();
//
//        ObjectTypeFieldEntity field = getObjectFieldEntity();
//        field.setExtType("ref");
//        field.setExtObject("Teams");
//        field.setTableField("department_id");
//        when(objectFieldRepo.findByObjectTypeAndSend("UserType", true)).thenReturn(List.of(field));
//
//        UserProfileEntity emptyProfile = new UserProfileEntity();
//        when(userProfileRepo.findByOid(oid)).thenReturn(Optional.of(emptyProfile));
//
//        UserProfileExtEntity ext = new UserProfileExtEntity();
//        ext.setOid(oid);
//        ext.setExtAttrName("department_id");
//        ext.setExtAttrValue("teamX");
//        when(userProfileExtRepo.findAllByOid(oid)).thenReturn(List.of(ext));
//
//        when(refValueRepo.getDisplayName("teamX", "Teams")).thenReturn("Team X");
//        when(enumRepo.findByEnumType(any())).thenReturn(List.of());
//
//        Map<String, Object> result = userService.getUserFields(oid, null, null);
//        List<UserFieldDto> fields = (List<UserFieldDto>) result.get("columns");
//
//        assertEquals(1, fields.size());
//        assertEquals("Team X", fields.get(0).getDisplayNameValue());
//        assertEquals("teamX", fields.get(0).getValue());
//    }
//
//    @Test
//    void parseSortField() {
//        UserServiceImpl.SortField sf = userService.parseSortField("name,desc");
//        assertNotNull(sf);
//        assertEquals("name", sf.field());
//        assertTrue(sf.descending());
//
//        sf = userService.parseSortField("name");
//        assertNotNull(sf);
//        assertFalse(sf.descending());
//    }
//
//    @Test
//    void sortListByFields_nameAscending() {
//        UserFieldDto dto1 = new UserFieldDto();
//        dto1.setName("B");
//        UserFieldDto dto2 = new UserFieldDto();
//        dto2.setName("A");
//        List<UserFieldDto> list = new ArrayList<>(List.of(dto1, dto2));
//
//        userService.sortListByFields(list, List.of(new UserServiceImpl.SortField("name", false)), UserFieldDto.class);
//
//        assertEquals("A", list.get(0).getName());
//        assertEquals("B", list.get(1).getName());
//    }
//
//    @Test
//    void sortListByFields_nameDescending() {
//        UserFieldDto dto1 = new UserFieldDto();
//        dto1.setName("A");
//        UserFieldDto dto2 = new UserFieldDto();
//        dto2.setName("B");
//        List<UserFieldDto> list = new ArrayList<>(List.of(dto1, dto2));
//
//        userService.sortListByFields(list, List.of(new UserServiceImpl.SortField("name", true)), UserFieldDto.class);
//
//        assertEquals("B", list.get(0).getName());
//        assertEquals("A", list.get(1).getName());
//    }
//
//    private static ObjectTypeFieldEntity getObjectFieldEntity() {
//        ObjectTypeFieldEntity field = new ObjectTypeFieldEntity();
//        field.setFieldName("department");
//        field.setFieldType("ref");
//        field.setObjectType("UserType");
//        field.setTableName("m_user_ext");
//        field.setTableField("department_id");
//        field.setVisible(true);
//        field.setRead(false);
//        field.setSend(true);
//        field.setTabName("Main");
//        field.setExtOrder("1");
//        field.setExtType("ref");
//        field.setExtObject("Departments");
//        return field;
//    }
//
//    @Data
//    @AllArgsConstructor
//    static class MockEntity {
//        @Column(name = "v_name")
//        private String name;
//
//        @Column(name = "v_number")
//        private int number;
//    }
//}
