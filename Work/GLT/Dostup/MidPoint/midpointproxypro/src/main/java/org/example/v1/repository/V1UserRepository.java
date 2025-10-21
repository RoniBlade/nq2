package org.example.v1.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class V1UserRepository {

    private final JdbcTemplate jdbc;

    public int updatePhotoFromBase64(String base64, String oid) { // updatePhotoFromBase64 (Обновить фото из base64)
        return jdbc.update(
                "UPDATE public.m_user " +
                        "SET photo = decode(?, 'base64') " +
                        "WHERE oid = CAST(? AS uuid)",
                ps -> {
                    ps.setString(1, base64); // строка base64
                    ps.setString(2, oid);    // строка UUID
                }
        );
    }

    public byte[] findPhoto(java.util.UUID oid) { // findPhoto (Найти фото)
        return jdbc.query(
                "SELECT photo FROM public.m_user WHERE oid = CAST(? AS uuid)",
                ps -> ps.setString(1, oid.toString()),
                rs -> rs.next() ? rs.getBytes(1) : null
        );
    }

    public int deletePhoto(java.util.UUID oid) { // deletePhoto (Удалить фото)
        return jdbc.update(
                "UPDATE public.m_user SET photo = NULL WHERE oid = CAST(? AS uuid)",
                ps -> ps.setString(1, oid.toString())
        );
    }
}
