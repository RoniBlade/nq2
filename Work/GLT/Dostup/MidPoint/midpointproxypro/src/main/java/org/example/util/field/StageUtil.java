package org.example.util.field;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.StageDto;

import java.util.Arrays;
import java.util.List;

@Slf4j
public class StageUtil {
    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);


    public static List<StageDto> parseStageList(String jsonStage) {
        try {
            if (jsonStage == null || jsonStage.isBlank()) return List.of();

            JsonNode node = mapper.readTree(jsonStage);

            if (node.isArray()) {
                return Arrays.asList(mapper.readValue(jsonStage, StageDto[].class));
            } else if (node.isObject()) {
                StageDto single = mapper.readValue(jsonStage, StageDto.class);
                return List.of(single);
            } else {
                log.warn("Stage JSON is neither array nor object: {}", jsonStage);
                return List.of();
            }

        } catch (Exception e) {
            log.error("Ошибка при парсинге stage JSON: {}", e.getMessage());
            return List.of();
        }
    }


    public static String writeStage(List<StageDto> stages) {
        try {
            if (stages == null) return null;
            return mapper.writeValueAsString(stages);
        } catch (Exception e) {
            log.error("Ошибка при сериализации списка StageDto: " + e.getMessage());
            return null;
        }
    }


}

