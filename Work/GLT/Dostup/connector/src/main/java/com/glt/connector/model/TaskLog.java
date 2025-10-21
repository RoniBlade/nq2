package com.glt.connector.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "server_ip")
    private String serverIp;

    @Column(name = "task_type")
    private String taskType;  // SCAN и т.д.

    private String status;    // SUCCESS, FAILED и т.д.

    private String message;

    private LocalDateTime timestamp = LocalDateTime.now();
}
