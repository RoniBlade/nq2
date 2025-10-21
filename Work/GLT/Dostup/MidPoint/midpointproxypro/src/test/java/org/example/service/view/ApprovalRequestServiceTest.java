package org.example.service.view;

import org.example.dto.view.ApprovalRequestDto;
import org.example.entity.view.ApprovalRequestEntity;
import org.example.mapper.ApprovalRequestMapper;
import org.example.model.filter.FilterRequest;
import org.example.repository.hibernate.view.ApprovalRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApprovalRequestServiceTest {

    @Mock
    private ApprovalRequestRepository repository;

    @Mock
    private ApprovalRequestMapper mapper;

    @InjectMocks
    private ApprovalRequestService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

//    @Test
//    void getApprovalRequests_shouldReturnMappedPage() {
//        // Arrange
//        Pageable pageable = PageRequest.of(0, 10);
//        String language = "en";
//
//        ApprovalRequestEntity entity = mock(ApprovalRequestEntity.class);
//
//        ApprovalRequestDto expectedDto = new ApprovalRequestDto();
//        expectedDto.setId(1L);
//        expectedDto.setUserName("jdoe");
//        expectedDto.setObjectName("Contract A");
//
//        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(entity)));
//        when(mapper.toDto(entity, language)).thenReturn(expectedDto);
//
//        // Act
//        Page<ApprovalRequestDto> result = service.getApprovalRequests(pageable, language);
//
//        // Assert
//        assertEquals(1, result.getTotalElements());
//        ApprovalRequestDto actual = result.getContent().get(0);
//        assertEquals(1L, actual.getId());
//        assertEquals("jdoe", actual.getUserName());
//        assertEquals("Contract A", actual.getObjectName());
//
//        verify(repository).findAll(pageable);
//        verify(mapper).toDto(entity, language);
//    }
//
//    @Test
//    void searchApprovalRequests_shouldReturnTrimmedMappedPage() {
//        // Arrange
//        Pageable pageable = PageRequest.of(0, 5);
//        String language = "ru";
//
//        FilterRequest request = new FilterRequest();
//        request.setFilters(Collections.emptyList());
//        request.setFields(List.of("id", "userName", "objectName"));        // включаем
//        request.setExcludeFields(List.of("objectDisplayName", "state"));   // исключаем
//
//        ApprovalRequestEntity entity = mock(ApprovalRequestEntity.class);
//
//        ApprovalRequestDto fullDto = new ApprovalRequestDto();
//        fullDto.setId(2L);
//        fullDto.setUserName("ivanov");
//        fullDto.setObjectName("Проект B");
//        fullDto.setObjectDisplayName("Секретный объект");
//        fullDto.setState("IN_PROGRESS");
//
//        when(repository.findAll(any(Specification.class), eq(pageable)))
//                .thenReturn(new PageImpl<>(List.of(entity)));
//        when(mapper.toDto(entity, language)).thenReturn(fullDto);
//
//        // Act
//        Page<Map<String, Object>> result = service.searchApprovalRequestViaJdbc(request, pageable);
//
//        // Assert
//        assertEquals(1, result.getTotalElements());
//        ApprovalRequestDto trimmed = result.getContent().get(0);
//
//        assertEquals(2L, trimmed.getId());
//        assertEquals("ivanov", trimmed.getUserName());
//        assertEquals("Проект B", trimmed.getObjectName());
//        assertNull(trimmed.getObjectDisplayName(), "objectDisplayName должно быть исключено");
//        assertNull(trimmed.getState(), "state должно быть исключено");
//
//        verify(repository).findAll(any(Specification.class), eq(pageable));
//        verify(mapper).toDto(entity, language);
//    }
}
