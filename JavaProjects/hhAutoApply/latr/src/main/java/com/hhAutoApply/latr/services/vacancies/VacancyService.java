package com.hhAutoApply.latr.services.vacancies;

import com.hhAutoApply.latr.clients.vacancies.VacancyClient;
import com.hhAutoApply.latr.models.Vacancy;
import com.hhAutoApply.latr.repos.VacancyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VacancyService {

    private final VacancyClient vacancyClient;
    private final VacancyRepository vacancyRepository;

    public List<Vacancy> getVacancies(String accessToken) {

        /*
        TODO: сделать автоматическую генерацию параметров из файла
         */
        Map<String, String> params = new HashMap<>();
        params.put("text", "Java NOT C++ NOT MOBILE NOT KOTLIN NOT FRONTEND");
        params.put("area", "1");
        params.put("experience", "between3And6");
        params.put("per_page", "100");
        params.put("page", "0");

        return vacancyClient.fetchVacancies(accessToken, params);

    }

    public void applyVacancies(List<Vacancy> vacancies, String accessToken) {
        for(Vacancy vacancy : vacancies) {
            applyVacancy(vacancy, accessToken);
        }
    }

    void applyVacancy(Vacancy vacancy, String accessToken) {

        vacancyClient.applyVacancy(accessToken, vacancy.getId());

    }

}
