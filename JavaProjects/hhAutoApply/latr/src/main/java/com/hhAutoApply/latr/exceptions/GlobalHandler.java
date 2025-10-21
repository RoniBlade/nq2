package com.hhAutoApply.latr.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalHandler {

    @ExceptionHandler(AuthException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public String handleAuthException(AuthException e) {
        log.error("Ошибка авторизации: {}", e.getMessage(), e);
        return "Ошибка авторизации: " + e.getMessage();
    }

    @ExceptionHandler(VacancyException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public String handleVacancyServiceException(AuthException e) {
        log.error("Ошибка при получении вакансий: {}", e.getMessage(), e);
        return "шибка при получении вакансий: " + e.getMessage();
    }

}
