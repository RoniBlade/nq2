package org.example.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StageDto {

    @JsonProperty("@id")
    private Long id;

    @JsonProperty("@ns")
    private String namespace;

    private Integer iteration;
    private Integer number;
    private String name;
    private String description;

    private LocalDateTime startTimestamp;
    private LocalDateTime deadline;
    private LocalDateTime endTimestamp;
}
