package com.glt.connector.model;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MachineInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String serverIp;
    private String cpu;
    private String ram;
    private String disk;
    private String uptime;


    public MachineInfo(String serverIp, String cpu, String ram, String disk, String uptime) {
        this.serverIp = serverIp;
        this.cpu = cpu;
        this.ram = ram;
        this.disk = disk;
        this.uptime = uptime;
    }
}