//package org.example.service.view;
//
//import org.example.dto.view.CaseInfoDto;
//import org.example.entity.view.CaseInfoEntity;
//import org.example.mapper.CaseInfoMapper;
//import org.example.model.filter.FilterRequest;
//import org.example.repository.hibernate.view.CaseInfoRepository;
//import org.example.service.CaseService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.*;
//import org.springframework.data.domain.*;
//import org.springframework.data.jpa.domain.Specification;
//
//import java.time.LocalDateTime;
//import java.util.Collections;
//import java.util.List;
//import java.util.UUID;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//class CaseInfoServiceTest {
//
//    @Mock
//    private CaseInfoRepository repository;
//
//    @Mock
//    private CaseInfoMapper mapper;
//
//    @InjectMocks
//    private CaseService service;
//
//    @BeforeEach
//    void setUp() {
//        MockitoAnnotations.openMocks(this);
//    }
//
//    @Test
//    void getCaseInfo_shouldReturnMappedPage() {
//        // Arrange
//        Pageable pageable = PageRequest.of(0, 10);
//        String lang = "ru";
//
//        CaseInfoEntity entity = mock(CaseInfoEntity.class);
//
//        CaseInfoDto expectedDto = new CaseInfoDto();
//        expectedDto.setId(1L);
//        expectedDto.setCaseoid(UUID.randomUUID());
//        expectedDto.setCaseDateCreate(LocalDateTime.now());
//        expectedDto.setType("APPROVAL");
//
//        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(entity)));
//        when(mapper.toDto(entity, lang)).thenReturn(expectedDto);
//
//        // Act
//        Page<CaseInfoDto> result = service.getCaseInfo(pageable, lang);
//
//        // Assert
//        assertEquals(1, result.getTotalElements());
//        CaseInfoDto actual = result.getContent().get(0);
//        assertEquals(expectedDto.getId(), actual.getId());
//        assertEquals(expectedDto.getType(), actual.getType());
//
//        verify(repository).findAll(pageable);
//        verify(mapper).toDto(entity, lang);
//    }
//
//    @Test
//    void searchCaseInfo_shouldReturnTrimmedMappedPage() {
//        // Arrange
//        Pageable pageable = PageRequest.of(0, 5);
//        String lang = "en";
//
//        FilterRequest request = new FilterRequest();
//        request.setFilters(Collections.emptyList());
//        request.setFields(List.of("id", "type")); // включаемые поля
//        request.setExcludeFields(List.of("caseDateCreate")); // исключаем
//
//        CaseInfoEntity entity = mock(CaseInfoEntity.class);
//
//        CaseInfoDto fullDto = new CaseInfoDto();
//        fullDto.setId(2L);
//        fullDto.setType("DELEGATION");
//        fullDto.setCaseDateCreate(LocalDateTime.now());
//
//        when(repository.findAll(any(Specification.class), eq(pageable)))
//                .thenReturn(new PageImpl<>(List.of(entity)));
//        when(mapper.toDto(entity, lang)).thenReturn(fullDto);
//
//        // Act
//        Page<CaseInfoDto> result = service.searchCaseInfo(request, pageable, lang);
//
//        // Assert
//        assertEquals(1, result.getTotalElements());
//        CaseInfoDto trimmed = result.getContent().get(0);
//
//        assertEquals(2L, trimmed.getId());
//        assertEquals("DELEGATION", trimmed.getType());
//        assertNull(trimmed.getCaseDateCreate(), "Поле caseDateCreate должно быть исключено");
//
//        verify(repository).findAll(any(Specification.class), eq(pageable));
//        verify(mapper).toDto(entity, lang);
//    }
//}
