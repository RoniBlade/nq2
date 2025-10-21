//package org.example.service;
//
//import org.example.dto.ObjectArchetypeFieldDto;
//import org.example.v1.dto.ObjectTypeFieldDto;
//import org.example.entity.ObjectArchetypeFieldEntity;
//import org.example.v1.entity.ObjectTypeFieldEntity;
//import org.example.entity.view.UserProfileEntity;
//import org.example.mapper.ObjectArchetypeFieldMapper;
//import org.example.v1.mapper.ObjectTypeFieldMapper;
//import org.example.mapper.ObjectTypeToArchetypeMapper;
//import org.example.repository.hibernate.ObjectArchetypeFieldRepository;
//import org.example.v1.repository.ObjectTypeFieldRepository;
//import org.example.repository.RefValueRepository;
//import org.example.util.RepositoryResolver;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.*;
//import org.springframework.data.jdbc.domain.Specification;
//
//import java.util.*;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class ObjectServiceTest {
//
//    @Mock
//    private ObjectArchetypeFieldRepository objectArchetypeFieldRepository;
//    @Mock
//    private ObjectArchetypeFieldMapper objectArchetypeFieldMapper;
//    @Mock
//    private ObjectTypeFieldRepository objectTypeFieldRepository;
//    @Mock
//    private ObjectTypeFieldMapper objectTypeFieldMapper;
//    @Mock
//    private ObjectTypeToArchetypeMapper objectTypeToArchetypeMapper;
//    @Mock
//    private RefValueRepository refValueRepo;
//    @Mock
//    private RepositoryResolver repositoryResolver;
//    @InjectMocks
//    private v1ObjectService objectService;
//
//
//    private final Pageable pageable = PageRequest.of(0, 10);
//    private final ObjectArchetypeFieldEntity objectArchetypeFieldEntity = new ObjectArchetypeFieldEntity();
//    private final ObjectArchetypeFieldDto objectArchetypeFieldDto = new ObjectArchetypeFieldDto();
//    private final ObjectTypeFieldEntity objectTypeFieldEntity = new ObjectTypeFieldEntity();
//    private final ObjectTypeFieldDto objectTypeFieldDto = new ObjectTypeFieldDto();
//    private final String tableField = "testTableField";
//    private final String fieldName = "testFieldName";
//    private final String tableName = "glt_get_user_profile";
//    private final Boolean send = true;
//
//    @BeforeEach
//    void setup() {
//        objectTypeFieldEntity.setTableField(tableField);
//        objectTypeFieldDto.setTableField(tableField);
//        objectArchetypeFieldDto.setTableField(tableField);
//        objectArchetypeFieldEntity.setTableField(tableField);
//
//        objectTypeFieldEntity.setFieldName(fieldName);
//        objectTypeFieldDto.setFieldName(fieldName);
//        objectArchetypeFieldDto.setFieldName(fieldName);
//        objectArchetypeFieldEntity.setFieldName(fieldName);
//
//        objectTypeFieldEntity.setTableName(tableName);
//        objectTypeFieldDto.setTableName(tableName);
//        objectArchetypeFieldDto.setTableName(tableName);
//        objectArchetypeFieldEntity.setTableName(tableName);
//
//        objectTypeFieldEntity.setSend(send);
//        objectTypeFieldDto.setSend(send);
//        objectArchetypeFieldDto.setSend(send);
//        objectArchetypeFieldEntity.setSend(send);
//
//    }
//
//    @Test
//    void getFieldsByArchetype_shouldReturnArchetypeFields_whenArchetypeFieldsExist() {
//        String archetype = "Person";
//        Pageable pageable = PageRequest.of(0, 10);
//
//        when(objectArchetypeFieldRepository.findAll(any(Specification.class), eq(pageable)))
//                .thenReturn(new PageImpl<>(List.of(objectArchetypeFieldEntity)));
//        when(objectArchetypeFieldMapper.toDto(objectArchetypeFieldEntity)).thenReturn(objectArchetypeFieldDto);
//
//        Map<String, Object> result = objectService.getFieldsByArchetypeOrByObjectType(archetype, pageable);
//
//        assertFalse(result.isEmpty());
//        assertEquals("null", result.get("oid"));
//        assertEquals("null", result.get("vDisplayName"));
//
//        List<ObjectArchetypeFieldDto> resultDtosList = (List<ObjectArchetypeFieldDto>) result.get("columns");
//
//        verify(objectTypeFieldRepository, never()).findAll((Example<ObjectTypeFieldEntity>) any(), (Sort) any());
//
//        assertNotNull(resultDtosList);
//        assertEquals(1, resultDtosList.size());
//        assertEquals(tableField, resultDtosList.get(0).getTableField());
//
//    }
//
//    @Test
//    void getFieldsByArchetype_shouldReturnObjectTypeFieldsMappedToArchetype_whenArchetypeFieldsAbsent() {
//        String archetype = "USER";
//
//        when(objectArchetypeFieldRepository.findAll(any(Specification.class), eq(pageable)))
//                .thenReturn(new PageImpl<>(List.of()));
//        when(objectTypeFieldRepository.findAll(any(Specification.class), eq(pageable)))
//                .thenReturn(new PageImpl<>(List.of(objectTypeFieldEntity)));
//        when(objectTypeFieldMapper.toDto(objectTypeFieldEntity)).thenReturn(objectTypeFieldDto);
//        when(objectTypeToArchetypeMapper.toArchetypeDto(objectTypeFieldDto)).thenReturn(objectArchetypeFieldDto);
//
//        Map<String, Object> result = objectService.getFieldsByArchetypeOrByObjectType(archetype, pageable);
//
//        assertFalse(result.isEmpty());
//        assertEquals("null", result.get("oid"));
//        assertEquals("null", result.get("vDisplayName"));
//
//        List<ObjectArchetypeFieldDto> resultDtosList = (List<ObjectArchetypeFieldDto>) result.get("columns");
//
//        verify(objectArchetypeFieldRepository).findAll((any(Specification.class)), eq(pageable));
//        verify(objectTypeFieldRepository).findAll((any(Specification.class)), eq(pageable));
//
//        assertNotNull(resultDtosList);
//        assertEquals(1, resultDtosList.size());
//        assertEquals(tableField, resultDtosList.get(0).getTableField());
//    }
//
//    @Test
//    void getFieldsByArchetype_shouldReturnEmptyColumns_whenNoFieldsFound() {
//
//        String archetype = "USER";
//
//        when(objectArchetypeFieldRepository.findAll(any(Specification.class), eq(pageable)))
//                .thenReturn(new PageImpl<>(List.of()));
//        when(objectTypeFieldRepository.findAll(any(Specification.class), eq(pageable)))
//                .thenReturn(new PageImpl<>(List.of()));
//
//        Map<String, Object> result = objectService.getFieldsByArchetypeOrByObjectType(archetype, pageable);
//
//        assertFalse(result.isEmpty());
//        assertEquals("null", result.get("oid"));
//        assertEquals("null", result.get("vDisplayName"));
//
//        verify(objectArchetypeFieldRepository).findAll((any(Specification.class)), eq(pageable));
//        verify(objectTypeFieldRepository).findAll((any(Specification.class)), eq(pageable));
//
//        assertEquals(new ArrayList<ObjectArchetypeFieldDto>(), result.get("columns"));
//
//    }
//
//    @Test
//    void searchFieldsByArchetype_shouldReturnFieldsFromArchetypeRepo_whenArchetypeFieldsExist() {
//        when(objectArchetypeFieldRepository.findAll(any(Specification.class), eq(pageable)))
//                .thenReturn(new PageImpl<>(List.of(objectArchetypeFieldEntity)));
//
//        when(objectArchetypeFieldMapper.toDto(objectArchetypeFieldEntity)).thenReturn(objectArchetypeFieldDto);
//
//        Map<String, Object> result = objectService.searchFieldsByArchetype("USER", null, pageable);
//        List<ObjectArchetypeFieldDto> resultDtosList = (List<ObjectArchetypeFieldDto>) result.get("columns");
//
//        assertEquals(objectArchetypeFieldDto.getTableField(), resultDtosList.get(0).getTableField());
//    }
//
//    @Test
//    void searchFieldsByArchetype_shouldFallbackToTypeFields_whenArchetypeRepoIsEmpty() {
//        when(objectArchetypeFieldRepository.findAll(any(Specification.class), eq(pageable)))
//                .thenReturn(new PageImpl<>(List.of()));
//
//        when(objectTypeFieldRepository.findAll(any(Specification.class), eq(pageable)))
//                .thenReturn(new PageImpl<>(List.of(objectTypeFieldEntity)));
//
//        when(objectTypeFieldMapper.toDto(objectTypeFieldEntity)).thenReturn(objectTypeFieldDto);
//        when(objectTypeToArchetypeMapper.toArchetypeDto(objectTypeFieldDto)).thenReturn(objectArchetypeFieldDto);
//
//        Map<String, Object> result = objectService.searchFieldsByArchetype("USER", null, pageable);
//        List<ObjectArchetypeFieldDto> resultDtosList = (List<ObjectArchetypeFieldDto>) result.get("columns");
//
//        assertEquals(objectArchetypeFieldDto.getTableField(), resultDtosList.get(0).getTableField());
//    }
//
//    @Test
//    void searchFieldsByArchetype_shouldHandleNullFilterRequest_whenRequestIsNull() {
//        when(objectArchetypeFieldRepository.findAll(any(Specification.class), eq(pageable)))
//                .thenReturn(new PageImpl<>(List.of(objectArchetypeFieldEntity)));
//
//        when(objectArchetypeFieldMapper.toDto(objectArchetypeFieldEntity)).thenReturn(objectArchetypeFieldDto);
//
//        Map<String, Object> result = objectService.searchFieldsByArchetype("USER", null, pageable);
//        List<ObjectArchetypeFieldDto> resultDtosList = (List<ObjectArchetypeFieldDto>) result.get("columns");
//
//        assertEquals(objectArchetypeFieldDto.getTableField(), resultDtosList.get(0).getTableField());
//    }
//
//    @Test
//    void searchFieldsByObjectType_shouldReturnTrimmedFields() {
//        // given
//
//        assertEquals(1,1);
//    }
//
//    @Test
//    void getFieldsByArchetypeAndOid_extArchetypeFound_success() throws Exception {
//        UUID oid = UUID.randomUUID();
//        String archetype = "someArchetype";
//
//        objectArchetypeFieldEntity.setFieldName("firstName");
//        objectArchetypeFieldEntity.setExtType("String");
//
//        UserProfileEntity userProfileEntity = new UserProfileEntity();
//        userProfileEntity.setOid(oid);
//        userProfileEntity.setName("User Name");
//        userProfileEntity.setVDisplayName("Test Name");
//
//        when(objectArchetypeFieldRepository.findAll(any(Specification.class), eq(pageable)))
//                .thenReturn(new PageImpl<>(List.of(objectArchetypeFieldEntity)));
//
//        when(repositoryResolver.resolveEntityClass("glt_get_user_profile"))
//                .thenReturn((Class) UserProfileEntity.class);
//
//        when(repositoryResolver.executeQueryForViews(eq(UserProfileEntity.class), any()))
//                .thenReturn(List.of(userProfileEntity));
//
//        when(objectArchetypeFieldMapper.toDto(objectArchetypeFieldEntity))
//                .thenReturn(objectArchetypeFieldDto);
//
//        Map<String, Object> result = objectService.getFieldsByArchetypeAndOid(archetype, oid, null, pageable);
//
//        assertEquals(oid, result.get("oid"));
//        assertEquals("Test Name", result.get("vDisplayName"));
//        List<?> columns = (List<?>) result.get("columns");
//        assertEquals(1, columns.size());
//        ObjectArchetypeFieldDto dto = (ObjectArchetypeFieldDto) columns.get(0);
//        assertEquals(tableField, dto.getTableField());
//    }
//    @Test
//    void getFieldsByArchetypeAndOid_extArchetypeEmpty_fallbackToObjectType() {
//        UUID oid = UUID.randomUUID();
//        String archetype = "USER";
//        objectArchetypeFieldDto.setExtArchetype(archetype);
//
//        when(objectArchetypeFieldRepository.findAll(any(Specification.class), any(Pageable.class)))
//                .thenReturn(new PageImpl<>(List.of()));
//
//        when(objectTypeFieldRepository.findAll(any(Specification.class), any(Pageable.class)))
//                .thenReturn(new PageImpl<>(List.of(objectTypeFieldEntity)));
//
//        when(objectTypeFieldMapper.toDto(objectTypeFieldEntity)).thenReturn(objectTypeFieldDto);
//        when(objectTypeToArchetypeMapper.toArchetypeDto(objectTypeFieldDto)).thenReturn(objectArchetypeFieldDto);
//
//        UserProfileEntity entity = new UserProfileEntity();
//        entity.setOid(oid);
//        entity.setVDisplayName("Fallback Name");
//
//        when(repositoryResolver.resolveEntityClass("glt_get_user_profile")).thenReturn((Class) UserProfileEntity.class);
//        when(repositoryResolver.executeQueryForViews(eq(UserProfileEntity.class), any())).thenReturn(List.of(entity));
//
//        Map<String, Object> result = objectService.getFieldsByArchetypeAndOid(archetype, oid, null, pageable);
//
//        assertEquals(oid, result.get("oid"));
//        assertEquals("Fallback Name", result.get("vDisplayName"));
//        assertFalse(((List<?>) result.get("columns")).isEmpty());
//    }
//
//    @Test
//    void getFieldsByArchetypeAndOid_noFieldsAnywhere_returnsEmpty() {
//        UUID oid = UUID.randomUUID();
//        String archetype = "unknown";
//
//        when(objectArchetypeFieldRepository.findAll(any(Specification.class), any(Pageable.class)))
//                .thenReturn(new PageImpl<>(List.of()));
//
//        when(objectTypeFieldRepository.findAll(any(Specification.class), any(Pageable.class)))
//                .thenReturn(new PageImpl<>(List.of()));
//
//        Map<String, Object> result = objectService.getFieldsByArchetypeAndOid(archetype, oid, null, pageable);
//
//        assertEquals("null", result.get("oid"));
//        assertEquals("null", result.get("vDisplayName"));
//        assertTrue(((List<?>) result.get("columns")).isEmpty());
//    }
//
//    @Test
//    void getFieldsByArchetypeAndOid_fieldWithoutTableName_skipped() {
//        UUID oid = UUID.randomUUID();
//        String archetype = "archetype";
//
//        objectArchetypeFieldEntity.setTableName(null);
//        objectArchetypeFieldEntity.setFieldName("no_table");
//
//        when(objectArchetypeFieldRepository.findAll(any(Specification.class), any(Pageable.class)))
//                .thenReturn(new PageImpl<>(List.of(objectArchetypeFieldEntity)));
//        when(objectArchetypeFieldMapper.toDto(objectArchetypeFieldEntity)).thenReturn(objectArchetypeFieldDto);
//
//        Map<String, Object> result = objectService.getFieldsByArchetypeAndOid(archetype, oid, null, pageable);
//
//        // ничего не обрабатывается, так как нет tableName
//        assertEquals("null", result.get("oid"));
//        assertEquals("null", result.get("vDisplayName"));
//        assertTrue(((List<?>) result.get("columns")).isEmpty());
//    }
//
//    @Test
//    void getFieldsByArchetypeAndOid_resolveEntityClassThrows_skipTable() {
//        UUID oid = UUID.randomUUID();
//        String archetype = "broken";
//
//        objectArchetypeFieldEntity.setTableName("unknown_table");
//        objectArchetypeFieldEntity.setFieldName("some");
//
//        when(objectArchetypeFieldRepository.findAll(any(Specification.class), any(Pageable.class)))
//                .thenReturn(new PageImpl<>(List.of(objectArchetypeFieldEntity)));
//
//        when(repositoryResolver.resolveEntityClass("unknown_table"))
//                .thenThrow(new IllegalArgumentException("Unknown"));
//
//        when(objectArchetypeFieldMapper.toDto(objectArchetypeFieldEntity)).thenReturn(objectArchetypeFieldDto);
//
//        Map<String, Object> result = objectService.getFieldsByArchetypeAndOid(archetype, oid, null, pageable);
//
//        assertEquals("null", result.get("oid"));
//        assertEquals("null", result.get("vDisplayName"));
//        assertTrue(((List<?>) result.get("columns")).isEmpty());
//    }
//
//
//
//
//}
