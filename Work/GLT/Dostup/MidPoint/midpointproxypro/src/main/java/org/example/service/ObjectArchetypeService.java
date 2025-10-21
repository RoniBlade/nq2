package org.example.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.ObjectArchetypeFieldDto;
import org.example.entity.ObjectArchetypeFieldEntity;
import org.example.mapper.ObjectArchetypeFieldMapper;
import org.example.repository.hibernate.ObjectArchetypeFieldRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ObjectArchetypeService {

    private final ObjectArchetypeFieldRepository repository;
    private final ObjectArchetypeFieldMapper mapper;

    public ObjectArchetypeFieldDto create(ObjectArchetypeFieldDto dto) {
        log.info("[Service] Creating ObjectArchetypeField: {}", dto);
        ObjectArchetypeFieldEntity entity = mapper.toEntity(dto);
        entity.setId(null); // при создании
        return mapper.toDto(repository.save(entity));
    }

    public ObjectArchetypeFieldDto update(Long id, ObjectArchetypeFieldDto dto) {
        ObjectArchetypeFieldEntity existing = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ObjectArchetypeField с id=" + id + " не найден"));

        ObjectArchetypeFieldEntity updated = mapper.toEntity(dto);
        updated.setId(id);

        return mapper.toDto(repository.save(updated));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("ObjectArchetypeField с id=" + id + " не найден");
        }
        repository.deleteById(id);
        log.info("[Service] Deleted ObjectArchetypeField with id={}", id);
    }
}
