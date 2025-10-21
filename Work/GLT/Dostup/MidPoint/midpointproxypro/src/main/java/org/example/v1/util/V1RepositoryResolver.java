package org.example.v1.util;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.v1.entity.DConfigParamEntity;
import org.example.v1.repository.DConfigParamRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class V1RepositoryResolver {

    private final DConfigParamRepository repo;

    /** Корень дерева, где лежат источники */
    @Value("${app.data-sources-root:Data Source}")
    private String rootKey;

    /** Имя ветки под корнем (дочка). Пусто — возьмём первую и предупредим в логах. */
    @Value("${app.data-sources-branch:}")
    private String branchKey;

    /** Иммутабельная мапа "структура -> функция БД" */
    private volatile Map<String, String> mapping = Map.of();

    @PostConstruct
    void init() {
        reload();
    }

    @Transactional(readOnly = true)
    public void reload() {
        // 1) пробуем как есть
        Optional<DConfigParamEntity> rootOpt = repo.findFirstByConfigparamIgnoreCase(rootKey);

        // 2) fallback: если в конце "Source" → пробуем "Sources"
        if (rootOpt.isEmpty() && rootKey != null && rootKey.trim().equalsIgnoreCase("Data Source")) {
            rootOpt = repo.findFirstByConfigparamIgnoreCase("Data Sources");
        }

        // 3) если всё ещё пусто — посмотрим, какие корни вообще существуют
        DConfigParamEntity root = rootOpt.orElseGet(() -> {
            // CHANGED: parentOidIsNull
            List<DConfigParamEntity> roots = repo.findByParentoidIsNull(); // CHANGED
            String available = roots.stream()
                    .map(DConfigParamEntity::getConfigparam)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.joining(", "));
            throw new IllegalStateException(
                    "Не найден корень d_config_param по configparam='" + rootKey +
                            "'. Доступные корни: [" + available + "]"
            );
        });

        // дочки первого уровня
        // CHANGED: findByParentOid
        List<DConfigParamEntity> level1 = repo.findByParentoid(root.getOid()); // CHANGED
        if (level1.isEmpty()) {
            throw new IllegalStateException("У корня '" + safeTrim(root.getConfigparam()) + "' нет дочерних записей");
        }

        DConfigParamEntity branch;
        if (branchKey != null && !branchKey.isBlank()) {
            // CHANGED: findFirstByParentOidAndConfigParamIgnoreCase
            branch = repo.findFirstByParentoidAndConfigparamIgnoreCase(root.getOid(), branchKey) // CHANGED
                    .orElseThrow(() -> new IllegalStateException(
                            "Под корнем '" + root.getConfigparam() + "' не найдена ветка '" + branchKey + "'"));
        } else {
            branch = level1.get(0);
            if (level1.size() > 1) {
                log.warn("Найдено несколько веток под '{}', не задан app.data-sources-branch — беру первую: {}",
                        root.getConfigparam(), branch.getConfigparam());
            }
        }

        // листья
        // CHANGED: findByParentOid
        List<DConfigParamEntity> leaves = repo.findByParentoid(branch.getOid()); // CHANGED
        if (leaves.isEmpty()) {
            log.warn("У ветки '{}' нет потомков — мапа будет пустой", branch.getConfigparam());
        }

        Map<String, String> tmp = new LinkedHashMap<>();
        for (DConfigParamEntity e : leaves) {
            String key = safeTrim(e.getConfigparam());
            String val = safeTrim(e.getValue());
            if (key != null && val != null && !key.isEmpty() && !val.isEmpty()) {
                tmp.put(key, val);
            }
        }

        this.mapping = Collections.unmodifiableMap(tmp);
        log.info("Loaded {} structure mappings from d_config_param (root='{}', branch='{}')",
                mapping.size(), root.getConfigparam(),
                (branchKey == null || branchKey.isBlank()) ? branch.getConfigparam() : branchKey);
    }

    /** Вернёт имя функции по ключу структуры или кинет IllegalArgumentException. */
    public String getFunctionName(String structureName) {
        String fn = mapping.get(structureName);
        if (fn == null || fn.isBlank()) {
            throw new IllegalArgumentException("Для структуры '" + structureName + "' нет функции (d_config_param)");
        }
        return fn;
    }

    /** Без исключения: вернёт defaultFn, если ключа нет. */
    public String getFunctionNameOrDefault(String structureName, String defaultFn) {
        String fn = mapping.get(structureName);
        return (fn == null || fn.isBlank()) ? defaultFn : fn;
    }

    public Map<String, String> getAllMappings() {
        return mapping;
    }

    private static String safeTrim(String s) {
        return (s == null) ? null : s.trim();
    }
}
