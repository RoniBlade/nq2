package org.example.controller.view;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.view.ObjectInfoLiteDto;
import org.example.model.filter.FilterRequest;
import org.example.service.view.ObjectInfoLiteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/object-info")
@OpenAPIDefinition(info = @Info(title = "Object info API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "ObjectInfoLiteController", description = "Взаимодействие с информацией о пользователе, включая стандартные атрибуты")
public class ObjectInfoLiteController {

    private final ObjectInfoLiteService service;

    @GetMapping("/view")
    public Page<ObjectInfoLiteDto> getObjectInfoLite(Pageable pageable,
                                                     Authentication authentication,
                                                     @RequestParam(name = "lang", required = false) String lang) {
        String userOid = authentication.getPrincipal().toString();
        log.info("[CTRL] GET /object-info/view by user {} with lang {}", userOid, lang);
        return service.getObjectInfoLiteViaJdbc(pageable);
    }

    @PostMapping("/filtered-view")
    public Page<ObjectInfoLiteDto> searchObjectInfoLite(@RequestBody FilterRequest request,
                                                          Pageable pageable,
                                                          Authentication authentication,
                                                          @RequestParam(name = "lang", required = false) String lang) {
        String userOid = authentication.getPrincipal().toString();
        log.info("[CTRL] POST /object-info/filtered-view by user {} with lang {}", userOid, lang);
        return service.searchObjectInfoLiteViaJdbc(
                request,
                pageable,
                lang
        );
    }

//    @GetMapping("/query")
//    public Object getData(Authentication authentication){
//        return service.getAll();
//    }
//
//    @PostMapping("/filtered")
//    public Object searchData(Authentication authentication, @RequestBody FilterRequest filterRequest){
//        return service.getAllWithFilters(filterRequest);
//    }
}
