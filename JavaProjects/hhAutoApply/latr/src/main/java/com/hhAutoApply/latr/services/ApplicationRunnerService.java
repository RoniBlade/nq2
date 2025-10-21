package com.hhAutoApply.latr.services;

import com.hhAutoApply.latr.models.Vacancy;
import com.hhAutoApply.latr.services.auth.AuthService;
import com.hhAutoApply.latr.services.vacancies.VacancyService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ApplicationRunnerService {

    private final AuthService authService;
    private final VacancyService vacancyService;

    @PostConstruct
    public void run() {

        System.out.println("Запрос токена");
        String accessToken = authService.getAccessCode();
        System.out.println(accessToken);

        System.out.println("Запрос вакансий");
        List<Vacancy> vacancies = vacancyService.getVacancies(accessToken);
        System.out.println(vacancies);

        System.out.println("Отклик на вакансии");
        vacancyService.applyVacancies(vacancies, accessToken);

    /*

    TODO: подумать о логировании в орекстраторе или отдельно в каждом сервисе

    */
    }



}
