package org.example.v1.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.v1.dto.DMenuParamDto;
import org.example.dto.view.MenuParamDto;
import org.example.v1.entity.DMenuParamEntity;
import org.example.entity.view.MenuParamEntity;
import org.example.v1.mapper.DMenuParamMapper;
import org.example.mapper.MenuParamMapper;
import org.example.model.filter.FilterRequest;
import org.example.v1.repository.DMenuParamRepository;
import org.example.repository.hibernate.MenuParamRepository;
import org.example.util.field.DtoFieldTrimmer;
import org.example.util.filter.FilterSpecificationBuilder;
import org.example.v1.service.StructureService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class MenuParamService {

    private final DMenuParamMapper dMenuParamMapper;
    private final DMenuParamRepository dMenuParamRepository;
    private final StructureService structureService;

    public Page<DMenuParamDto> getMenu(Pageable pageable) {
        return dMenuParamRepository.findAll(pageable).map(dMenuParamMapper::toDto);
    }

    public Page<DMenuParamDto> searchMenu(FilterRequest request, Pageable pageable) {
        var spec = FilterSpecificationBuilder.build(
                null,
                request.getFilters(),
                DMenuParamEntity.class,
                null
        );

        return dMenuParamRepository.findAll(spec, pageable).map(entity -> {
            var dto = dMenuParamMapper.toDto(entity);
            return DtoFieldTrimmer.trim(dto, request.getFields(), request.getExcludeFields());
        });
    }

    public DMenuParamDto createMenuParam(DMenuParamDto dto) {
        DMenuParamEntity entity = dMenuParamMapper.toEntity(dto);
        DMenuParamEntity saved = dMenuParamRepository.save(entity);
        return dMenuParamMapper.toDto(saved);
    }

    public DMenuParamDto updateMenuParam(UUID oid, DMenuParamDto dto) {
        // Получаем существующую запись по OID
        DMenuParamEntity existing = dMenuParamRepository.findByOid(oid)
                .orElseThrow(() -> new EntityNotFoundException(
                        "DMenuParam с oid=" + oid + " не найден"));

        // Маппируем только измененные поля
        updateDMenuParamEntityFromDto(existing, dto);

        // Сохраняем и возвращаем обновленную сущность
        DMenuParamEntity saved = dMenuParamRepository.save(existing);
        return dMenuParamMapper.toDto(saved);
    }

    private void updateDMenuParamEntityFromDto(DMenuParamEntity existing, DMenuParamDto dto) {
        if (dto.getOid() != null) {
            existing.setOid(dto.getOid());
        }
        if (dto.getParentoid() != null) {
            existing.setParentoid(dto.getParentoid());
        }
        if (dto.getEntrytype() != null) {
            existing.setEntrytype(dto.getEntrytype());
        }
        if (dto.getVisible() != null) {
            existing.setVisible(dto.getVisible());
        }
        if (dto.getMenuitem() != null) {
            existing.setMenuitem(dto.getMenuitem());
        }
        if (dto.getSortorder() != null) {
            existing.setSortorder(dto.getSortorder());
        }
        if (dto.getIcon() != null) {
            existing.setIcon(dto.getIcon());
        }
        if (dto.getDisplaytype() != null) {
            existing.setDisplaytype(dto.getDisplaytype());
        }
        if (dto.getCondition() != null) {
            existing.setCondition(dto.getCondition());
        }
        if (dto.getObjecttype() != null) {
            existing.setObjecttype(dto.getObjecttype());
        }
        if (dto.getArchetype() != null) {
            existing.setArchetype(dto.getArchetype());
        }
        if (dto.getTab() != null) {
            existing.setTab(dto.getTab());
        }
        if (dto.getChannel() != null) {
            existing.setChannel(dto.getChannel());
        }
    }


    @Transactional
    public void deleteMenuParam(UUID oid) {
        if (!dMenuParamRepository.existsByOid(oid)) {
            throw new EntityNotFoundException("DMenuParam с oid=" + oid + " не найден");
        }
        dMenuParamRepository.deleteByOid(oid);
    }

    public Page<Map<String, Object>> searchDataFromMenuByOid(String oid,
                                                             FilterRequest request,
                                                             Pageable pageable) {
        log.info("[MenuParam] Поиск данных по OID: {}", oid);

        DMenuParamEntity menuParamEntity = dMenuParamRepository.findByOid(UUID.fromString(oid))
                .orElseThrow(() -> new IllegalArgumentException("Menu param not found for oid: " + oid));

        String structureKey = menuParamEntity.getObjecttype();
        String archetype = menuParamEntity.getArchetype() == null ? menuParamEntity.getObjecttype() : menuParamEntity.getArchetype();
        String whereClause  = menuParamEntity.getCondition();

        log.info("[MenuParam] StructureKey: {}, where: {}", structureKey, whereClause);

        Page<Map<String, Object>> raw = structureService.getDataStructure(structureKey, archetype, request.getFilters(), whereClause, pageable);

        List<Map<String, Object>> trimmed = raw.getContent().stream()
                .map(row -> DtoFieldTrimmer.trimMap(
                        row,
                        request.getFields(),
                        request.getExcludeFields()
                ))
                .toList();

        return new PageImpl<>(trimmed, pageable, raw.getTotalElements());
    }

}
