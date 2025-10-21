//package org.example.service.view;
//
//import org.example.dto.view.AccountsDto;
//import org.example.entity.view.AccountsEntity;
//import org.example.mapper.AccountsMapper;
//import org.example.model.filter.FilterRequest;
//import org.example.repository.hibernate.view.AccountsRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.*;
//import org.springframework.data.domain.*;
//import org.springframework.data.jdbc.domain.Specification;
//
//import java.time.LocalDateTime;
//import java.util.Collections;
//import java.util.List;
//import java.util.UUID;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//class AccountsServiceTest {
//
//    @Mock
//    private AccountsRepository repository;
//
//    @Mock
//    private AccountsMapper mapper;
//
//    @InjectMocks
//    private AccountsService service;
//
//    @BeforeEach
//    void setUp() {
//        MockitoAnnotations.openMocks(this);
//    }
//
//    @Test
//    void getAccounts_shouldReturnMappedPage() {
//        // Arrange
//        Pageable pageable = PageRequest.of(0, 10);
//        AccountsEntity entity = mock(AccountsEntity.class);
//
//        AccountsDto expectedDto = new AccountsDto();
//        expectedDto.setId(1L);
//        expectedDto.setNameOrig("admin01");
//        expectedDto.setObjectType("User");
//        expectedDto.setDead(false);
//
//        when(repository.findAll(pageable))
//                .thenReturn(new PageImpl<>(List.of(entity)));
//        when(mapper.toDto(entity, "en")).thenReturn(expectedDto);
//
//        // Act
//        Page<AccountsDto> result = service.getAccounts(pageable, "en");
//
//        // Assert
//        assertEquals(1, result.getTotalElements());
//        AccountsDto actual = result.getContent().get(0);
//        assertEquals(1L, actual.getId());
//        assertEquals("admin01", actual.getNameOrig());
//        assertEquals("User", actual.getObjectType());
//        assertFalse(actual.getDead());
//
//        verify(repository).findAll(pageable);
//        verify(mapper).toDto(entity, "en");
//    }
//
//    @Test
//    void searchAccounts_shouldReturnTrimmedMappedPage() {
//        // Arrange
//        Pageable pageable = PageRequest.of(0, 5);
//
//        FilterRequest request = new FilterRequest();
//        request.setFilters(Collections.emptyList());
//        request.setFields(List.of("id", "nameOrig", "objectType"));   // включённые
//        request.setExcludeFields(List.of("v_displayName"));           // исключаем
//
//        AccountsEntity entity = mock(AccountsEntity.class);
//
//        AccountsDto fullDto = new AccountsDto();
//        fullDto.setId(10L);
//        fullDto.setNameOrig("sys_account");
//        fullDto.setObjectType("Shadow");
//        fullDto.setDead(true);
//        fullDto.setV_displayName("Технический аккаунт");
//
//        when(repository.findAll(any(Specification.class), eq(pageable)))
//                .thenReturn(new PageImpl<>(List.of(entity)));
//        when(mapper.toDto(entity, "ru")).thenReturn(fullDto);
//
//        // Act
//        Page<AccountsDto> result = service.searchAccounts(request, pageable, "ru");
//
//        // Assert
//        assertEquals(1, result.getTotalElements());
//        AccountsDto trimmed = result.getContent().get(0);
//        assertEquals(10L, trimmed.getId());
//        assertEquals("sys_account", trimmed.getNameOrig());
//        assertEquals("Shadow", trimmed.getObjectType());
//        assertNull(trimmed.getV_displayName(), "Поле должно быть исключено");
//
//        verify(repository).findAll(any(Specification.class), eq(pageable));
//        verify(mapper).toDto(entity, "ru");
//    }
//}
