package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.client.MidPointClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MidpointService {

    private final MidPointClient midPointClient;

    public ResponseEntity<Object> getObject(
            String id,
            String authHeaders,
            String type,
            List<String> options,
            List<String> include,
            List<String> exclude,
            List<String> resolveNames
    ) {
        ResponseEntity<Object> result = midPointClient.getObject(type, id, authHeaders, options, include, exclude, resolveNames);
        return result;
    }

    public ResponseEntity<Object> getFilteredObject(
            Object body,
            String authHeaders,
            String type,
            List<String> options,
            List<String> include,
            List<String> exclude,
            List<String> resolveNames
    ) {
        return midPointClient.searchObject(type, authHeaders, body, options, include, exclude, resolveNames);
    }

    public Mono<ResponseEntity<?>> openNextStage(String authHeader, UUID oid){
        return midPointClient.openNextStage(oid, authHeader);
    }

    public Mono<ResponseEntity<?>> closeCurrentStage(String authHeader, UUID oid){
        return midPointClient.closeCurrentStage(oid, authHeader);
    }

    public Mono<ResponseEntity<?>>closeCampaign(String authHeader, UUID oid){
        return midPointClient.closeCampaign(oid, authHeader);
    }

    public Mono<ResponseEntity<?>> reiterateCampaign(String authHeader, UUID oid){
        return midPointClient.reiterateCampaign(oid, authHeader);
    }

    public Mono<ResponseEntity<?>> startRemediation(String authHeader, UUID oid){
        return midPointClient.startRemediationCampaign(oid, authHeader);
    }

    public ResponseEntity<?> deleteDeputy(String authHeader, Object body){
        return midPointClient.deleteDeputy(authHeader, body);
    }
}
