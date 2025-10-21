//package org.example.service.view;
//
//import org.example.dto.view.UserProfileDto;
//import org.example.entity.view.UserProfileEntity;
//import org.example.mapper.UserProfileMapper;
//import org.example.repository.hibernate.view.UserProfileRepository;
//import org.example.util.filter.FilterSpecificationBuilder;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.Mockito;
//import org.mockito.MockitoAnnotations;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jdbc.domain.Specification;
//
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.AdditionalMatchers.eq;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.isNull;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//class UserProfileServiceTest {
//    @InjectMocks
//    private UserProfileService service;
//
//    private FilterSpecificationBuilder builder;
//    private UserProfileMapper mapper;
//    private UserProfileRepository repository;
//
//    @Mock
//    private Specification<UserProfileEntity> specification;
//
//    @BeforeEach
//    void setUp() {
//        builder = Mockito.mock(FilterSpecificationBuilder.class);
//        mapper = Mockito.mock(UserProfileMapper.class);
//        repository = Mockito.mock(UserProfileRepository.class);
//        MockitoAnnotations.openMocks(this);
//    }
//
//    @Test
//    public void getRequestUserProfileService(){
//        Pageable pageable = PageRequest.of(0, 10);
//        String language = "en";
//        UserProfileEntity entity = Mockito.mock(UserProfileEntity.class);
//        UserProfileDto dto = Mockito.mock(UserProfileDto.class);
//        Page<UserProfileEntity> entityPage = new PageImpl<>(List.of(entity));
//        when(repository.findAll(pageable)).thenReturn(entityPage);
//        when(mapper.toDto(entity, language)).thenReturn(dto);
//
//
//        Page<UserProfileDto> result = service.getUserProfile(pageable, language);
//
//        assertEquals(1, result.getTotalElements());
//        assertEquals(dto, result.getContent().get(0));
//        verify(repository).findAll(pageable);
//        verify(mapper).toDto(entity, language);
//    }
//
////    @Test
////    public void getSearchUserProfileService(){
////        Pageable pageable = PageRequest.of(0, 10);
////        String language = "en";
////        UserProfileEntity entity = Mockito.mock(UserProfileEntity.class);
////        UserProfileDto dto = Mockito.mock(UserProfileDto.class);
////        List<String> fields; // target fields
////
////        Specification<UserProfileEntity> spec = (root, query, criteriaBuilder) -> {
////            return criteriaBuilder.conjunction();
////        };
////        Page<UserProfileEntity> entityPage = new PageImpl<>(List.of(entity));
////        when(FilterSpecificationBuilder.build(any(), eq(filters), eq(UserProfileEntity.class), isNull())).thenReturn(spec);
////        when(repository.findAll(pageable)).thenReturn(entityPage);
////        when(mapper.toDto(entity, language)).thenReturn(dto);
////
////        Page<UserProfileDto> result = service.searchUserProfile();
////
////        assertEquals(1, result.getTotalElements());
////        assertEquals(dto, result.getContent().get(0));
////        verify(repository).findAll(pageable);
////        verify(mapper).toDto(entity, language);
////    }
//}
