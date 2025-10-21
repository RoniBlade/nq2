package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.client.MidPointClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
public class TestX5Service {

    private final MidPointClient midPointClient;

    public ResponseEntity<String> proxyGetRequest(String param, HttpHeaders headers, String authorizationHeader) {
        return midPointClient.proxyGetRequest(param, headers, authorizationHeader);
    }

    public ResponseEntity<String> proxyPostRequest(String param, String body, HttpHeaders incomingHeaders, String authorizationHeader) {
        return midPointClient.proxyPostRequest(param, body, incomingHeaders, authorizationHeader);
    }
}

