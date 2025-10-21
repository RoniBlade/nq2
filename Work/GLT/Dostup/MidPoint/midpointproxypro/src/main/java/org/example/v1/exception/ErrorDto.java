package org.example.v1.exception;

import lombok.*;
import org.springframework.cglib.core.Local;
import org.springframework.http.HttpStatus;

import java.sql.Date;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorDto {
    private LocalDateTime time;
    private HttpStatus status;
    private String error;
    private String message;
    private String path;
    private String httpMethod;
    private int errorCode;

}
