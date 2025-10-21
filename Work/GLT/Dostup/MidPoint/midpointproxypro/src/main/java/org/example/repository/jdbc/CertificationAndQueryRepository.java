package org.example.repository.jdbc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.util.SqlBuilder;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class CertificationAndQueryRepository {

    private final JdbcTemplate jdbc;

    private static final String query =
                """
                select 'certification' as"variables", count( macwa.targetoid ) as "values"
                        FROM m_access_cert_case maccas,
                                 m_access_cert_wi macw,
                                 m_access_cert_wi_assignee macwa
                               WHERE        \s
                     macw.owneroid = maccas.owneroid
                     AND macwa.owneroid = macw.owneroid
                     AND macw.accesscertcasecid = maccas.cid
                     AND macwa.accesscertworkitemcid = macw.cid
                     AND macw.stagenumber = maccas.stagenumber
                     AND macw.outcome IS NULL
                     and macwa.targetoid = ?
                     union
                     select 'requests' as"variables", count(*) as "values"
                        FROM
                         m_case_wi_assignee mcwa,
                         m_case_wi mcw,
                         m_case mc
                         where
                         mcwa.owneroid = mcw.owneroid
                         AND mcwa.workitemcid = mcw.cid
                         AND mcwa.owneroid = mc.oid
                         AND mc.state <> 'closed'::text
                     and mcwa.targetoid = ?
                """;

    public Page<Map<String, Object>> findAllByOid(Pageable pageable, UUID oid){
        String parametrizeQuery = SqlBuilder.parameterize(query, oid);
        var dataQ = SqlBuilder
                .selectFrom("(" + parametrizeQuery + ") src")
                .paginate(pageable) // добавит ORDER BY / LIMIT / OFFSET
                .build();

        var rows = jdbc.queryForList(dataQ.sql(), dataQ.params().toArray());

        var countQ = SqlBuilder
                .selectFrom("(" + parametrizeQuery + ") src")
                .count();

        Long total = jdbc.queryForObject(countQ.sql(), countQ.params().toArray(), Long.class);
        if (total == null) total = 0L;

        return new PageImpl<>(rows, pageable, total);
    }
}
