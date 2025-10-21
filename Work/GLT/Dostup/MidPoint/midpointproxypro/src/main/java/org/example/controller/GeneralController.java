package org.example.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.model.filter.FilterRequest;
import org.example.service.GeneralService;
import org.example.util.RepositoryResolver;
import org.example.util.ViewInfoSchema;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Data
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class GeneralController <E, R extends JpaRepository<E, ?>, M> {

    private final GeneralService service;
    private Object viewInfo; // TODO изменить на другой тип
    private final ViewInfoSchema viewInfoSchema;
    private final RepositoryResolver repositoryResolver;

    public Class<?> targetEntity;
    public Class<?> targetDto;
    public Class<?> targetMapper;
    public Class<?> targetRepository;

    @GetMapping("/{path}/view")
    public ResponseEntity<Page<Map<String, Object>>> getQuery(@PathVariable String path, Pageable pageable){
        try {
            Page<Map<String, Object>> result = service.getQuery(path, pageable);
            return ResponseEntity.ok(result);

        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/{path}/filtered-view")
    public ResponseEntity<Page<Map<String, Object>>> getSearch(
            @PathVariable String path,
            Pageable pageable,
            @RequestBody FilterRequest request){
        try {
            Page<Map<String, Object>> result = service.searchQuery(path, pageable, request.getFilters());
            return ResponseEntity.ok(result);

        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

//    @GetMapping("/{path}/test")
//    public Page<?> get( // TODO change response type
//            Authentication authentication,
//            @PathVariable String path,
//            Pageable pageable,
//            @RequestParam(name = "lang", required = false) String lang) {
//        if(viewInfoSchema.nameAndValuesMap.containsKey(path)){
//            viewInfo = viewInfoSchema.nameAndValuesMap.get(path);
//            targetRepository = viewInfoSchema.nameAndValuesMap.get(path).repositoryType; // TODO use getter and setter
//
//        } else {
//            throw new RuntimeException("параметр 'path' не соответствует существующим представлениям");
//        }
//        return service.setDataAndGetView(targetRepository,pageable);
//    }
}

