package com.glt.connector.repository;

import com.glt.connector.model.Server;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServerRepository extends JpaRepository<Server, Long> {

    // 1) Годные к скану: ИСКЛЮЧАЕМ актуальные резервации
    @Query(value = """
        SELECT *
        FROM servers s
        WHERE s.active = true
          AND (
               s.last_scanned_at IS NULL
            OR s.last_scanned_at <= now() - (COALESCE(s.scan_interval_minutes,0) || ' minutes')::interval
            OR COALESCE(s.scan_interval_minutes, 0) <= 0
          )
          AND (
               s.reserved_at IS NULL
            OR s.reserved_at < now() - (CAST(:reserveTimeoutMinutes AS text) || ' minutes')::interval
          )
        ORDER BY s.last_scanned_at NULLS FIRST, s.id
        """,
            countQuery = """
        SELECT count(*)
        FROM servers s
        WHERE s.active = true
          AND (
               s.last_scanned_at IS NULL
            OR s.last_scanned_at <= now() - (COALESCE(s.scan_interval_minutes,0) || ' minutes')::interval
            OR COALESCE(s.scan_interval_minutes, 0) <= 0
          )
          AND (
               s.reserved_at IS NULL
            OR s.reserved_at < now() - (CAST(:reserveTimeoutMinutes AS text) || ' minutes')::interval
          )
        """,
            nativeQuery = true)
    Page<Server> findEligibleForScan(Pageable pageable,
                                     @Param("reserveTimeoutMinutes") int reserveTimeoutMinutes);

    // 2) Резерв (ВАЖНО: больше НЕ трогаем last_scanned_at тут)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "UPDATE servers SET reserved_at = now(), updated_at = now() WHERE id = ANY(:ids)", nativeQuery = true)
    int reserveForScan(@Param("ids") Long[] ids);

    // 3) Снять резерв (после обработки)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "UPDATE servers SET reserved_at = NULL, updated_at = now() WHERE id = :id", nativeQuery = true)
    void clearReservation(@Param("id") Long id);

    // 4) Отметить факт попытки (в начале обработки воркером)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "UPDATE servers SET last_attempted_at = now(), updated_at = now() WHERE id = :id", nativeQuery = true)
    void markScanAttempted(@Param("id") Long id);

    // 5) Отметить завершение скана (в конце обработки воркером, успех/ошибка — неважно)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "UPDATE servers SET last_scanned_at = now(), updated_at = now() WHERE id = :id", nativeQuery = true)
    void markScanFinished(@Param("id") Long id);

    // 6) Сколько «свежих» резерваций (используем в Creator, чтобы понять — есть хвост из прошлого обхода)
    @Query(value = """
        SELECT count(*) FROM servers s
        WHERE s.active = true
          AND s.reserved_at IS NOT NULL
          AND s.reserved_at > now() - (CAST(:reserveTimeoutMinutes AS text) || ' minutes')::interval
        """, nativeQuery = true)
    long countFreshReservations(@Param("reserveTimeoutMinutes") int reserveTimeoutMinutes);

    Page<Server> findByActiveTrue(PageRequest of);
}
