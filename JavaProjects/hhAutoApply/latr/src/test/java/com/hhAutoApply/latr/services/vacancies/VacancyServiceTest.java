package com.hhAutoApply.latr.services.vacancies;


import com.hhAutoApply.latr.clients.vacancies.VacancyClient;
import com.hhAutoApply.latr.exceptions.VacancyException;
import com.hhAutoApply.latr.models.Vacancy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class VacancyServiceTest {

    @Mock
    private VacancyClient vacancyClientMock;

    @Mock
    private String accessToken = "fakeToken";

    @InjectMocks
    private VacancyService vacancyService;

    Map<String, String> params = new HashMap<>();

    @BeforeEach
    void setUp() {



    }


    @Test
    void getVacancies_Success() {

        List<Vacancy> fakeVacancies = List.of(
                new Vacancy(1L, "Java Developer Junior"),
                new Vacancy(2L, "Java Developer Middle"),
                new Vacancy(3L, "Java Developer Senior")
        );

        Map<String, String> params = Map.of("text", "Java", "area", "1");

        when(vacancyClientMock.fetchVacancies("fakeToken", params))
                .thenReturn(fakeVacancies);

        List<Vacancy> result = vacancyService.getVacancies("accessCode");

        assertEquals(fakeVacancies, result);

    }


    @Test
    void getVacancies_whenClientThrowsExceptions_shouldPropagateExceptions() {
        when(vacancyClientMock.fetchVacancies(anyString(), anyMap())).thenThrow(new VacancyException("VacancyServiceError"));

        Assertions.assertThrows(VacancyException.class, () ->
        {
            vacancyService.getVacancies("accessCode");
        });

    }

}
