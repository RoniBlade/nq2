//
//package org.example.service.view;
//
//import org.example.dto.view.UserProfileDto;
//import org.example.entity.view.MenuParamEntity;
//import org.example.model.filter.FilterRequest;
//import org.example.repository.hibernate.MenuParamRepository;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.jdbc.core.BeanPropertyRowMapper;
//
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//import java.util.UUID;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.when;
//
//@ExtendWith(MockitoExtension.class)
//class MenuParamServiceTest {
//
//    @Mock
//    private MenuParamRepository menuParamRepository;
//    @Mock
//    private JdbcTemplate jdbcTemplate;
//    @InjectMocks
//    private MenuParamService menuParamService;
//
//    @Test
//    void testMappingToDtoWhenDtoClassResolved() {
//        // Arrange
//        String oid = UUID.randomUUID().toString();
//        MenuParamEntity entity = new MenuParamEntity();
//        entity.setObject("glt_get_user_profile");
//        entity.setCondition("1=1");
//
//        when(menuParamRepository.findByOid(eq(oid))).thenReturn(Optional.of(entity));
//
//        UserProfileDto dto = new UserProfileDto();
//        dto.setId(123);
//        dto.setName("John Doe");
//
//        when(jdbcTemplate.query(
//                anyString(),
//                any(Object[].class),
//                any(BeanPropertyRowMapper.class)
//        )).thenReturn(List.of(dto));
//
//        when(jdbcTemplate.queryForObject(
//                anyString(),
//                any(Object[].class),
//                eq(Long.class)
//        )).thenReturn(1L);
//
//        FilterRequest request = new FilterRequest();
//        Pageable pageable = PageRequest.of(0, 10);
//
//        // Act
//        Map<String, Object> result = menuParamService.searchDataFromMenuByOid(oid, request, pageable);
//
//        // Assert
//        List<?> content = (List<?>) result.get("content");
//        assertEquals(1, content.size());
//        Object first = content.get(0);
//        assertEquals("John Doe", ((UserProfileDto) first).getName());
//    }
//}
