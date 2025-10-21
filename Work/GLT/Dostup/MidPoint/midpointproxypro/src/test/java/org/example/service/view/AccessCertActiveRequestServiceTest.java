//package org.example.service.view;
//
//import org.example.dto.view.AccessCertActiveRequestDto;
//import org.example.entity.view.AccessCertActiveRequestEntity;
//import org.example.mapper.AccessCertActiveRequestMapper;
//import org.example.model.filter.FilterRequest;
//import org.example.repository.hibernate.view.AccessCertActiveRequestRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.*;
//import org.springframework.data.domain.*;
//
//import org.springframework.data.jpa.domain.Specification;
//
//import java.util.Collections;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//class AccessCertActiveRequestServiceTest {
//
//    @Mock
//    private AccessCertActiveRequestRepository repository;
//
//    @Mock
//    private AccessCertActiveRequestMapper mapper;
//
//    @InjectMocks
//    private AccessCertActiveRequestService service;
//
//    @BeforeEach
//    void setUp() {
//        MockitoAnnotations.openMocks(this);
//    }
//
//    @Test
//    void getAccessCertActiveRequests_shouldReturnMappedPage() {
//        // Arrange
//        String language = "ru";
//        Pageable pageable = PageRequest.of(0, 10);
//
//        AccessCertActiveRequestEntity entity = mock(AccessCertActiveRequestEntity.class);
//
//        AccessCertActiveRequestDto expectedDto = new AccessCertActiveRequestDto();
//        expectedDto.setId(123L);
//        expectedDto.setObjectName("Имя объекта");
//        expectedDto.setCertName("Сертификация");
//
//        Page<AccessCertActiveRequestEntity> entityPage = new PageImpl<>(List.of(entity));
//
//        when(repository.findAll(pageable)).thenReturn(entityPage);
//        when(mapper.toDto(entity, language)).thenReturn(expectedDto);
//
//        // Act
//        Page<AccessCertActiveRequestDto> result = service.getAccessCertActiveRequests(pageable, language);
//
//        // Assert
//        assertEquals(1, result.getTotalElements());
//
//        AccessCertActiveRequestDto actualDto = result.getContent().get(0);
//        assertEquals(123L, actualDto.getId());
//        assertEquals("Имя объекта", actualDto.getObjectName());
//        assertEquals("Сертификация", actualDto.getCertName());
//
//        verify(repository).findAll(pageable);
//        verify(mapper).toDto(entity, language);
//    }
//
//
//    @Test
//    void searchAccessCertActiveRequests_shouldReturnTrimmedMappedPage() {
//        // Arrange
//        String language = "en";
//        Pageable pageable = PageRequest.of(0, 5);
//
//        FilterRequest request = new FilterRequest();
//        request.setFilters(Collections.emptyList());
//        request.setFields(List.of("id", "objectName", "certName"));  // включаемые поля
//        request.setExcludeFields(List.of("objectDisplayName"));      // исключаем
//
//        AccessCertActiveRequestEntity entity = mock(AccessCertActiveRequestEntity.class);
//
//        AccessCertActiveRequestDto fullDto = new AccessCertActiveRequestDto();
//        fullDto.setId(123L);
//        fullDto.setObjectName("Sample Object");
//        fullDto.setCertName("Certification X");
//        fullDto.setObjectDisplayName("Should be trimmed");
//
//        Page<AccessCertActiveRequestEntity> entityPage = new PageImpl<>(List.of(entity));
//
//        when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(entityPage);
//        when(mapper.toDto(entity, language)).thenReturn(fullDto);
//
//        // Act
//        Page<AccessCertActiveRequestDto> result = service.searchAccessCertActiveRequests(request, pageable, language);
//
//        // Assert
//        AccessCertActiveRequestDto trimmed = result.getContent().get(0);
//        assertEquals(123L, trimmed.getId());
//        assertEquals("Sample Object", trimmed.getObjectName());
//        assertEquals("Certification X", trimmed.getCertName());
//        assertNull(trimmed.getObjectDisplayName(), "objectDisplayName должно быть обнулено");
//
//        verify(repository).findAll(any(Specification.class), eq(pageable));
//        verify(mapper).toDto(entity, language);
//    }
//
//}
