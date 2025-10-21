package org.example.service.view;

import org.example.dto.view.MyRequestDto;
import org.example.entity.view.MyRequestEntity;
import org.example.mapper.MyRequestMapper;
import org.example.model.filter.FilterRequest;
import org.example.repository.hibernate.view.MyRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MyRequestServiceTest {

    @Mock
    private MyRequestRepository repository;

    @Mock
    private MyRequestMapper mapper;

    @InjectMocks
    private MyRequestService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

//    @Test
//    void getAllRequests_shouldReturnMappedPage() {
//        Pageable pageable = PageRequest.of(0, 10);
//        String language = "en";
//
//        MyRequestEntity entity = mock(MyRequestEntity.class);
//
//        MyRequestDto dto = new MyRequestDto();
//        dto.setId(1L);
//        dto.setObjectName("Object A");
//
//        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(entity)));
//        when(mapper.toDto(entity, language)).thenReturn(dto);
//
//        Page<MyRequestDto> result = service.getAllRequests(pageable, language);
//
//        assertEquals(1, result.getTotalElements());
//        assertEquals(1L, result.getContent().get(0).getId());
//        assertEquals("Object A", result.getContent().get(0).getObjectName());
//
//        verify(repository).findAll(pageable);
//        verify(mapper).toDto(entity, language);
//    }

//    @Test
//    void getRequestsForUser_shouldReturnMappedPage() {
//        Pageable pageable = PageRequest.of(0, 10);
//        String userOid = "123e4567-e89b-12d3-a456-426614174000";
//        String language = "ru";
//
//        MyRequestEntity entity = mock(MyRequestEntity.class);
//
//        MyRequestDto dto = new MyRequestDto();
//        dto.setId(2L);
//        dto.setRequesterName("Ivanov");
//
//        when(repository.findByTargetOid(UUID.fromString(userOid), pageable))
//                .thenReturn(new PageImpl<>(List.of(entity)));
//        when(mapper.toDto(entity, language)).thenReturn(dto);
//
//        Page<MyRequestDto> result = service.getRequestsForUser(userOid, pageable, language);
//
//        assertEquals(1, result.getTotalElements());
//        assertEquals("Ivanov", result.getContent().get(0).getRequesterName());
//
//        verify(repository).findByTargetOid(UUID.fromString(userOid), pageable);
//        verify(mapper).toDto(entity, language);
//    }
//
//    @Test
//    void searchRequestsForUser_shouldReturnTrimmedMappedPage() {
//        Pageable pageable = PageRequest.of(0, 5);
//        String userOid = "123e4567-e89b-12d3-a456-426614174001";
//        String language = "en";
//
//        FilterRequest request = new FilterRequest();
//        request.setFilters(Collections.emptyList());
//        request.setFields(List.of("id", "requesterName"));
//        request.setExcludeFields(List.of("objectId"));
//
//        MyRequestEntity entity = mock(MyRequestEntity.class);
//
//        MyRequestDto dto = new MyRequestDto();
//        dto.setId(3L);
//        dto.setRequesterName("Petrov");
//        dto.setObjectId("OBJ-999");
//
//        when(repository.findAll(any(Specification.class), eq(pageable)))
//                .thenReturn(new PageImpl<>(List.of(entity)));
//        when(mapper.toDto(entity, language)).thenReturn(dto);
//
//        Page<MyRequestDto> result = service.searchRequestsForUser(userOid, request, pageable, language);
//
//        assertEquals(1, result.getTotalElements());
//        MyRequestDto trimmed = result.getContent().get(0);
//        assertEquals(3L, trimmed.getId());
//        assertEquals("Petrov", trimmed.getRequesterName());
//        assertNull(trimmed.getObjectId());
//
//        verify(repository).findAll(any(Specification.class), eq(pageable));
//        verify(mapper).toDto(entity, language);
//    }

//    @Test
//    void searchAllRequests_shouldReturnTrimmedMappedPage() {
//        Pageable pageable = PageRequest.of(0, 5);
//        String language = "ru";
//
//        FilterRequest request = new FilterRequest();
//        request.setFilters(Collections.emptyList());
//        request.setFields(List.of("id", "objectName"));
//        request.setExcludeFields(List.of("requesterPhone"));
//
//        MyRequestEntity entity = mock(MyRequestEntity.class);
//
//        MyRequestDto dto = new MyRequestDto();
//        dto.setId(4L);
//        dto.setObjectName("Заявка X");
//        dto.setRequesterPhone("88005553535");
//
//        when(repository.findAll(any(Specification.class), eq(pageable)))
//                .thenReturn(new PageImpl<>(List.of(entity)));
//        when(mapper.toDto(entity, language)).thenReturn(dto);
//
//        Page<MyRequestDto> result = service.searchAllRequests(request, pageable, language);
//
//        assertEquals(1, result.getTotalElements());
//        MyRequestDto trimmed = result.getContent().get(0);
//        assertEquals(4L, trimmed.getId());
//        assertEquals("Заявка X", trimmed.getObjectName());
//        assertNull(trimmed.getRequesterPhone());
//
//        verify(repository).findAll(any(Specification.class), eq(pageable));
//        verify(mapper).toDto(entity, language);
//    }
}
