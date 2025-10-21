//package org.example.service.view;
//
//import org.example.dto.view.DelegationDto;
//import org.example.entity.view.DelegationEntity;
//import org.example.mapper.DelegationMapper;
//import org.example.repository.hibernate.view.DelegationRepository;
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
//class DelegationServiceTest {
//    @InjectMocks
//    private DelegationService service;
//
//    private DelegationMapper mapper;
//    private DelegationRepository repository;
//
//    @BeforeEach
//    void setUp() {
//        mapper = Mockito.mock(DelegationMapper.class);
//        repository = Mockito.mock(DelegationRepository.class);
//        MockitoAnnotations.openMocks(this);
//    }
//
//    @Test
//    public void getRequestUserProfileService(){
//        Pageable pageable = PageRequest.of(0, 10);
//        String language = "en";
//        DelegationEntity entity = Mockito.mock(DelegationEntity.class);
//        DelegationDto dto = Mockito.mock(DelegationDto.class);
//        Page<DelegationEntity> entityPage = new PageImpl<>(List.of(entity));
//        when(repository.findAll(pageable)).thenReturn(entityPage);
//        when(mapper.toDto(entity, language)).thenReturn(dto);
//
//        Page<DelegationDto> result = service.getDelegations(pageable, language);
//
//        assertEquals(1, result.getTotalElements());
//        assertEquals(dto, result.getContent().get(0));
//        verify(repository).findAll(pageable);
//        verify(mapper).toDto(entity, language);
//    }
//
//}
