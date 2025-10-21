package org.example.controller.view;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.view.AccountsDto;

import org.example.dto.view.MyRequestDto;
import org.example.model.filter.FilterRequest;
import org.example.service.view.AccountsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accounts")
@OpenAPIDefinition(info = @Info(title = "Accounts API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Slf4j
@Tag(name = "AccountsController", description = "Взаимодействие с информацией о shadow objects, связывает их с пользователями"
        + " и предоставляет ключевые свойства аккаунтов")
public class AccountsController {

    private final AccountsService service;

    @GetMapping("/view")
    public Page<Map<String, Object>> getAccounts(
            Authentication authentication,
            Pageable pageable,
            @RequestParam(name = "lang", required = false) String lang){
        return service.getAccounts(pageable);
    }

    @PostMapping("/filtered-view")
    public Page<Map<String, Object>> searchAllRequests(@RequestBody FilterRequest request,
            Pageable pageable,
            @RequestParam(name = "lang", required = false) String lang) {
        log.info("[CTRL] POST /my-requests/filtered-view with lang {}", lang);
        return service.searchAccounts(request, pageable);
    }

//    @PostMapping("/filtered-view")
//    public Page<AccountsDto> searchAccounts(
//            Authentication authentication,
//            Pageable pageable,
//            @RequestParam(name = "lang", required = false) String lang,
//            @RequestBody FilterRequest request){
//        return service.searchAccounts(
//                request,
//                pageable
//        );
//    }

}
