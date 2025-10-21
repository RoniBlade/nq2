//package org.example.service.view;
//
//import org.example.dto.view.AccountAttributeDto;
//import org.example.entity.view.AccountAttributeEntity;
//import org.example.mapper.AccountAttributeMapper;
//import org.example.model.filter.FilterRequest;
//import org.example.repository.hibernate.view.AccountAttributeRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.*;
//import org.springframework.data.domain.*;
//import org.springframework.data.jpa.domain.Specification;
//
//import java.util.Collections;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//class AccountAttributeServiceTest {
//
//    @Mock
//    private AccountAttributeRepository repository;
//
//    @Mock
//    private AccountAttributeMapper mapper;
//
//    @InjectMocks
//    private AccountAttributeService service;
//
//    @BeforeEach
//    void setUp() {
//        MockitoAnnotations.openMocks(this);
//    }
//
//    @Test
//    void getAccountAttributes_shouldReturnMappedPage() {
//        // Arrange
//        Pageable pageable = PageRequest.of(0, 10);
//        AccountAttributeEntity entity = mock(AccountAttributeEntity.class);
//
//        AccountAttributeDto expectedDto = new AccountAttributeDto();
//        expectedDto.setId(1L);
//        expectedDto.setAttrName("Department");
//        expectedDto.setAttrValue("Finance");
//
//        when(repository.findAll(any(Specification.class), eq(pageable)))
//                .thenReturn(new PageImpl<>(List.of(entity)));
//        when(mapper.toDto(entity)).thenReturn(expectedDto);
//
//        // Act
//        Page<AccountAttributeDto> result = service.getAccountAttributes(pageable, "en");
//
//        // Assert
//        assertEquals(1, result.getTotalElements());
//        AccountAttributeDto actual = result.getContent().get(0);
//        assertEquals(1L, actual.getId());
//        assertEquals("Department", actual.getAttrName());
//        assertEquals("Finance", actual.getAttrValue());
//
//        verify(repository).findAll(any(Specification.class), eq(pageable));
//        verify(mapper).toDto(entity);
//    }
//
//    @Test
//    void searchAccountAttributes_shouldReturnTrimmedMappedPage() {
//        // Arrange
//        Pageable pageable = PageRequest.of(0, 5);
//
//        FilterRequest request = new FilterRequest();
//        request.setFilters(Collections.emptyList());
//        request.setFields(List.of("id", "attrName"));        // include
//        request.setExcludeFields(List.of("attrValue"));      // exclude
//
//        AccountAttributeEntity entity = mock(AccountAttributeEntity.class);
//
//        AccountAttributeDto fullDto = new AccountAttributeDto();
//        fullDto.setId(2L);
//        fullDto.setAttrName("Team");
//        fullDto.setAttrValue("HR");
//
//        when(repository.findAll(any(Specification.class), eq(pageable)))
//                .thenReturn(new PageImpl<>(List.of(entity)));
//        when(mapper.toDto(entity)).thenReturn(fullDto);
//
//        // Act
//        Page<AccountAttributeDto> result = service.searchAccountAttributes(request, pageable, "en");
//
//        // Assert
//        assertEquals(1, result.getTotalElements());
//        AccountAttributeDto trimmed = result.getContent().get(0);
//
//        assertEquals(2L, trimmed.getId());
//        assertEquals("Team", trimmed.getAttrName());
//        assertNull(trimmed.getAttrValue(), "attrValue должен быть исключён триммером");
//
//        verify(repository).findAll(any(Specification.class), eq(pageable));
//        verify(mapper).toDto(entity);
//    }
//}
