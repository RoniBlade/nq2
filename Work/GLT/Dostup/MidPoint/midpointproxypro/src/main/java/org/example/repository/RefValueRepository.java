package org.example.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class RefValueRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public String getDisplayName(String id, String extObject) {

        String result = null;
        try {
            result = (String) entityManager
                    .createNativeQuery("SELECT glt_get_ref_value(:id, :extObject)")
                    .setParameter("id", id)
                    .setParameter("extObject", extObject)
                    .getSingleResult();
        } catch (Exception e) {
            log.warn("⚠️ Ошибка при вызове glt_get_ref_value для '{}': {}", extObject, e.getMessage(), e);
        }
        return result;
    }

}
