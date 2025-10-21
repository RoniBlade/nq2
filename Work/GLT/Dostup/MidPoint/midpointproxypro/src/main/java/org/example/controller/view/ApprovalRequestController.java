package org.example.controller.view;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.view.ApprovalRequestDto;
import org.example.model.filter.FilterRequest;
import org.example.service.view.ApprovalRequestService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/approval-requests")
@OpenAPIDefinition(info = @Info(title = "approval requests API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "ApprovalRequestController", description = "Взаимодействие с текущими активными запросами на утверждение")
@RequiredArgsConstructor
public class ApprovalRequestController {

    private final ApprovalRequestService approvalRequestService;

//    public ApprovalRequestController(ApprovalRequestService approvalRequestService) {
//        this.approvalRequestService = approvalRequestService;
//    }

    @GetMapping("/view")
    public Page<ApprovalRequestDto> getApprovalRequests(Pageable pageable,
                                                        Authentication authentication,
                                                        @RequestParam(name = "lang", required = false) String lang) {
        log.info("[CTRL] GET /approval-requests/view with lang {}", lang);
        return approvalRequestService.getApprovalRequestViaJdbc(pageable);
    }

    @PostMapping("/filtered-view")
    public Page<ApprovalRequestDto> searchApprovalRequests(@RequestBody FilterRequest request,
                                                                Pageable pageable,
                                                                Authentication authentication,
                                                                @RequestParam(name = "lang", required = false) String lang) {
        log.info("[CTRL] POST /approval-requests/filtered-view with lang {}", lang);
        return approvalRequestService.searchApprovalRequestViaJdbc(
                request,
                pageable
        );
    }
}
