//package org.example.service.view;
//
//import org.example.dto.view.ApprovalRoleInfoDto;
//import org.example.entity.view.ApprovalRoleInfoEntity;
//import org.example.mapper.ApprovalRoleInfoMapper;
//import org.example.model.filter.FilterRequest;
//import org.example.repository.hibernate.view.ApprovalRoleInfoRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.*;
//import org.springframework.data.domain.*;
//import org.springframework.data.jpa.domain.Specification;
//
//import java.util.Collections;
//import java.util.List;
//import java.util.UUID;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//class ApprovalRoleInfoServiceTest {
//
//    @Mock
//    private ApprovalRoleInfoRepository repository;
//
//    @Mock
//    private ApprovalRoleInfoMapper mapper;
//
//    @InjectMocks
//    private ApprovalRoleInfoService service;
//
//    @BeforeEach
//    void setUp() {
//        MockitoAnnotations.openMocks(this);
//    }
//
//    @Test
//    void getApprovalRoleInfo_shouldReturnMappedPage() {
//        // Arrange
//        Pageable pageable = PageRequest.of(0, 10);
//        ApprovalRoleInfoEntity entity = mock(ApprovalRoleInfoEntity.class);
//
//        ApprovalRoleInfoDto expectedDto = new ApprovalRoleInfoDto();
//        expectedDto.setId(1L);
//        expectedDto.setObjectName("Test Object");
//        expectedDto.setObjectDisplayName("Test Display Name");
//
//        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(entity)));
//        when(mapper.toDto(entity, "en")).thenReturn(expectedDto);
//
//        // Act
//        Page<ApprovalRoleInfoDto> result = service.getApprovalRoleInfo(pageable, "en");
//
//        // Assert
//        assertEquals(1, result.getTotalElements());
//        ApprovalRoleInfoDto actual = result.getContent().get(0);
//        assertEquals(1L, actual.getId());
//        assertEquals("Test Object", actual.getObjectName());
//        assertEquals("Test Display Name", actual.getObjectDisplayName());
//
//        verify(repository).findAll(pageable);
//        verify(mapper).toDto(entity, "en");
//    }
//
//    @Test
//    void searchApprovalRoleInfo_shouldReturnTrimmedMappedPage() {
//        // Arrange
//        Pageable pageable = PageRequest.of(0, 5);
//        FilterRequest request = new FilterRequest();
//        request.setFilters(Collections.emptyList());
//        request.setFields(List.of("id", "objectName"));
//        request.setExcludeFields(List.of("objectDisplayName"));
//
//        ApprovalRoleInfoEntity entity = mock(ApprovalRoleInfoEntity.class);
//
//        ApprovalRoleInfoDto fullDto = new ApprovalRoleInfoDto();
//        fullDto.setId(42L);
//        fullDto.setObjectName("ObjName");
//        fullDto.setObjectDisplayName("Should be trimmed");
//        fullDto.setOwneroid(UUID.randomUUID());
//
//        when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of(entity)));
//        when(mapper.toDto(entity, "en")).thenReturn(fullDto);
//
//        // Act
//        Page<ApprovalRoleInfoDto> result = service.searchApprovalRoleInfo(request, pageable, "en");
//
//        // Assert
//        assertEquals(1, result.getTotalElements());
//        ApprovalRoleInfoDto actual = result.getContent().get(0);
//
//        assertEquals(42L, actual.getId());
//        assertEquals("ObjName", actual.getObjectName());
//        assertNull(actual.getObjectDisplayName(), "objectDisplayName должен быть исключён");
//
//        verify(repository).findAll(any(Specification.class), eq(pageable));
//        verify(mapper).toDto(entity, "en");
//    }
//}
