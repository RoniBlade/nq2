package org.example.controller.view;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.dto.view.MyRequestDto;
import org.example.model.filter.FilterRequest;
import org.example.service.view.MyRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/my-requests")
@OpenAPIDefinition(info = @Info(title = "My request API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "MyRequestController", description = "Взаимодействие с заявками")
@RequiredArgsConstructor
public class MyRequestController {

    private static final Logger log = LoggerFactory.getLogger(MyRequestController.class);
    private final MyRequestService myRequestService;

    @GetMapping("/view")
    public Page<MyRequestDto> getAllRequests(Pageable pageable,
                                             @RequestParam(name = "lang", required = false) String lang) {
        log.info("[CTRL] GET /my-requests/view with lang {}", lang);
        return myRequestService.getMyRequestViaJdbc(pageable);
    }

    @PostMapping("/filtered-view")
    public Page<MyRequestDto> searchAllRequests(@RequestBody FilterRequest request,
                                                Pageable pageable,
                                                @RequestParam(name = "lang", required = false) String lang) {
        log.info("[CTRL] POST /my-requests/filtered-view with lang {}", lang);
        return myRequestService.searchMyRequestViaJdbc(request, pageable, lang);
    }
}
