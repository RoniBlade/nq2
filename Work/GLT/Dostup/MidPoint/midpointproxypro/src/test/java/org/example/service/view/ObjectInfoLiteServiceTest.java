package org.example.service.view;

import org.example.dto.view.ObjectInfoLiteDto;
import org.example.entity.view.ObjectInfoLiteEntity;
import org.example.mapper.ObjectInfoLiteMapper;
import org.example.repository.hibernate.view.ObjectInfoLiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ObjectInfoLiteServiceTest {
//
//    @InjectMocks
//    private ObjectInfoLiteService service;
//
//    private ObjectInfoLiteMapper mapper;
//    private ObjectInfoLiteRepository repository;
//
//    @BeforeEach
//    void setUp() {
//        mapper = Mockito.mock(ObjectInfoLiteMapper.class);
//        repository = Mockito.mock(ObjectInfoLiteRepository.class);
//        MockitoAnnotations.openMocks(this);
//    }

//    @Test
//    public void getRequestUserProfileService(){
//        Pageable pageable = PageRequest.of(0, 10);
//        String language = "en";
//        ObjectInfoLiteEntity entity = Mockito.mock(ObjectInfoLiteEntity.class);
//        ObjectInfoLiteDto dto = Mockito.mock(ObjectInfoLiteDto.class);
//        Page<ObjectInfoLiteEntity> entityPage = new PageImpl<>(List.of(entity));
//        when(repository.findAll(pageable)).thenReturn(entityPage);
//        when(mapper.toDto(entity, language)).thenReturn(dto);
//
//        Page<ObjectInfoLiteDto> result = service.getObjectInfoLite(pageable, language);
//
//        assertEquals(1, result.getTotalElements());
//        assertEquals(dto, result.getContent().get(0));
//        verify(repository).findAll(pageable);
//        verify(mapper).toDto(entity, language);
//    }

}
