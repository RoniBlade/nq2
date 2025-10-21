//package org.example.service.view;
//
//import org.example.dto.view.UserAssignmentsGrafDto;
//import org.example.entity.view.UserAssignmentsGrafEntity;
//import org.example.mapper.UserAssignmentsGrafMapper;
//import org.example.repository.hibernate.view.UserAssignmentsGrafRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mockito;
//import org.mockito.MockitoAnnotations;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//class UserAssignmentsGrafServiceTest {
//    @InjectMocks
//    private UserAssignmentsGrafService service;
//
//    private UserAssignmentsGrafMapper mapper;
//    private UserAssignmentsGrafRepository repository;
//
//    @BeforeEach
//    void setUp() {
//        mapper = Mockito.mock(UserAssignmentsGrafMapper.class);
//        repository = Mockito.mock(UserAssignmentsGrafRepository.class);
//        MockitoAnnotations.openMocks(this);
//    }
//
//    @Test
//    public void getRequestUserProfileService(){
//        Pageable pageable = PageRequest.of(0, 10);
//        String language = "en";
//        UserAssignmentsGrafEntity entity = Mockito.mock(UserAssignmentsGrafEntity.class);
//        UserAssignmentsGrafDto dto = Mockito.mock(UserAssignmentsGrafDto.class);
//        Page<UserAssignmentsGrafEntity> entityPage = new PageImpl<>(List.of(entity));
//        when(repository.findAll(pageable)).thenReturn(entityPage);
//        when(mapper.toDto(entity, language)).thenReturn(dto);
//
//        Page<UserAssignmentsGrafDto> result = service.getUserAssignmentsGraf(pageable, language);
//
//        assertEquals(1, result.getTotalElements());
//        assertEquals(dto, result.getContent().get(0));
//        verify(repository).findAll(pageable);
//        verify(mapper).toDto(entity, language);
//    }
//
//}
