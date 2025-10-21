package com.glt.connector.model;

import com.glt.connector.model.enums.OperatingSystemType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "servers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Server {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String displayname;

    @Column(nullable = false)
    private String host;

    @Enumerated(EnumType.STRING)
    @Column(name = "os_type", nullable = false)
    private OperatingSystemType osType;

    @Column(nullable = false)
    private String login;

    @Column(nullable = false)
    private String password;

    @Column(columnDefinition = "int4 default 22")
    private Integer port = 22;

    @Builder.Default
    private Boolean active = true;

    /** когда успешно завершён последний скан */
    @Column(name = "last_scanned_at")
    private LocalDateTime lastScannedAt;

    /** интервал скана в минутах */
    @Builder.Default
    @Column(name = "scan_interval_minutes", nullable = false, columnDefinition = "int4 not null default 5")
    private Integer scanIntervalMinutes = 5;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
