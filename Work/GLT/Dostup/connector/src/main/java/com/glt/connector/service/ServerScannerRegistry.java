package com.glt.connector.service;

import com.glt.connector.model.enums.OperatingSystemType;
import com.glt.connector.connector.ServerConnector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ServerScannerRegistry {

    private final Map<String, ServerConnector> scannerMap;

    public ServerConnector getScanner(OperatingSystemType osType) {
        ServerConnector scanner = scannerMap.get(osType.name());
        if (scanner == null) {
            throw new IllegalArgumentException("No connector for OS: " + osType);
        }
        return scanner;
    }
}
