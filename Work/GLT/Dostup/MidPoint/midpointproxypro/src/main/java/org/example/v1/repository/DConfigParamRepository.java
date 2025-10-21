package org.example.v1.repository;

import org.example.v1.entity.DConfigParamEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DConfigParamRepository
        extends JpaRepository<DConfigParamEntity, UUID>,
        JpaSpecificationExecutor<DConfigParamEntity> {

    Optional<DConfigParamEntity> findByOid(UUID oid); // (найтиПоOid)
    @Modifying
    void deleteByOid(UUID oid);                     // (удалитьПоOid)
    boolean existsByOid(UUID oid);                    // (существуетПоOid)

    Optional<DConfigParamEntity> findFirstByConfigparamIgnoreCase(String configParam); // (найтиПервыйПоConfigParamБезУчётаРегистра)

    List<DConfigParamEntity> findByParentoid(UUID parentOid); // (найтиПоParentOid)

    Optional<DConfigParamEntity> findFirstByParentoidAndConfigparamIgnoreCase(
            UUID parentOid, String configParam
    ); // (найтиПервыйПоParentOidИConfigParamБезУчётаРегистра)

    List<DConfigParamEntity> findByParentoidIsNull(); // (найтиГдеParentOidNull)

}
