package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.repository.jdbc.CertificationAndQueryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CertificationAndQueryService {

    private final CertificationAndQueryRepository repository;

    public Page<Map<String, Object>> getAllByOid(Pageable pageable, UUID oid){
        return repository.findAllByOid(pageable,oid);
    }
}
