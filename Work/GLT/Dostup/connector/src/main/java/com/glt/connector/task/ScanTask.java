package com.glt.connector.task;


import com.glt.connector.model.enums.OperatingSystemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ScanTask {
    private String taskId;
    private String serverIp;
    private Long serverId;
    private int port = 22;
    private String login;
    private String encryptedPassword;
    private OperatingSystemType osType;
    private LocalDateTime lastScannedAt;      // когда сервер в последний раз был успешно просканен
    private Integer scanIntervalMinutes;      // интервал скана в минутах

    public ScanTask(String taskId, String serverIp, Long serverId, int port, String login, String encryptedPassword, OperatingSystemType osType) {
        this.taskId = taskId;
        this.serverIp = serverIp;
        this.serverId = serverId;
        this.port = port;
        this.login = login;
        this.encryptedPassword = encryptedPassword;
        this.osType = osType;
    }


}
