package org.example.v1.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.filter.FilterRequest;
import org.example.util.field.DtoFieldTrimmer;
import org.example.util.filter.FilterSpecificationBuilder;
import org.example.v1.dto.ObjectTypeFieldDto;
import org.example.v1.entity.ObjectTypeFieldEntity;
import org.example.v1.mapper.ObjectTypeFieldMapper;
import org.example.v1.repository.ObjectTypeFieldRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ObjectFieldDefinitionsService {


    private final ObjectTypeFieldRepository repository;
    private final ObjectTypeFieldMapper mapper;

    public ObjectTypeFieldDto create(ObjectTypeFieldDto dto) {
        ObjectTypeFieldEntity entity = mapper.toEntity(dto);
        return mapper.toDto(repository.save(entity));
    }

    public ObjectTypeFieldDto update(UUID oid, ObjectTypeFieldDto dto) {
        ObjectTypeFieldEntity existing = repository.findById(oid)
                .orElseThrow(() -> new EntityNotFoundException("Поле с id=" + oid + " не найдено"));

        updateObjectTypeFieldEntityFromDto(existing, dto);

        ObjectTypeFieldEntity saved = repository.save(existing);
        return mapper.toDto(saved);
    }

    private void updateObjectTypeFieldEntityFromDto(ObjectTypeFieldEntity existing, ObjectTypeFieldDto dto) {
        if (dto.getFieldname() != null) {
            existing.setFieldname(dto.getFieldname());
        }
        if (dto.getFieldtype() != null) {
            existing.setFieldtype(dto.getFieldtype());
        }
        if (dto.getObjecttype() != null) {
            existing.setObjecttype(dto.getObjecttype());
        }
        if (dto.getArchetype() != null) {
            existing.setArchetype(dto.getArchetype());
        }
        if (dto.getTablefield() != null) {
            existing.setTablefield(dto.getTablefield());
        }
        if (dto.getSend() != null) {
            existing.setSend(dto.getSend());
        }
        if (dto.getVisible() != null) {
            existing.setVisible(dto.getVisible());
        }
        if (dto.getRead() != null) {
            existing.setRead(dto.getRead());
        }
        if (dto.getTabname() != null) {
            existing.setTabname(dto.getTabname());
        }
        if (dto.getExtorder() != null) {
            existing.setExtorder(dto.getExtorder());
        }
        if (dto.getExttype() != null) {
            existing.setExttype(dto.getExttype());
        }
        if (dto.getExtobject() != null) {
            existing.setExtobject(dto.getExtobject());
        }
        if (dto.getExtwhereclause() != null) {
            existing.setExtwhereclause(dto.getExtwhereclause());
        }
        if (dto.getExtnotes() != null) {
            existing.setExtnotes(dto.getExtnotes());
        }
    }

    @Transactional
    public void delete(UUID oid) {
        if (!repository.existsByOid(oid)) {
            throw new EntityNotFoundException("Поле с oid=" + oid + " не найдено");
        }
        repository.deleteByOid(oid);
    }

    public Page<ObjectTypeFieldDto> getConfigParams(Pageable pageable) {
        return repository.findAll(pageable)
                .map(mapper::toDto);
    }

    public Page<ObjectTypeFieldDto> searchConfigParams(FilterRequest request, Pageable pageable) {
        Specification<ObjectTypeFieldEntity> spec = FilterSpecificationBuilder
                .build(null, request.getFilters(), ObjectTypeFieldEntity.class, null);

        return repository.findAll(spec, pageable)
                .map(entity -> {
                    ObjectTypeFieldDto dto = mapper.toDto(entity);
                    return DtoFieldTrimmer.trim(dto, request.getFields(), request.getExcludeFields());
                });
    }

    public List<String> getAllObjectTypes() {
        return repository.findDistinctObjectTypes();
    }

    public List<String> getArchetypesByObjectType(String objectType) {
        return repository.findDistinctArchetypesByObjectType(objectType);
    }



}
