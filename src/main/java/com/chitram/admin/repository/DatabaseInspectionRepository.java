package com.chitram.admin.repository;

import com.chitram.admin.dto.AdminTableResponse;
import com.chitram.admin.dto.DatabaseTableInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DatabaseInspectionRepository {
    private final JdbcTemplate jdbcTemplate;

    public DatabaseInspectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long countRows(String tableName) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COALESCE(n_live_tup, 0) FROM pg_stat_user_tables WHERE schemaname = 'public' AND relname = ?",
                Long.class, tableName);
        return count == null ? 0 : count;
    }

    public boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?",
                Integer.class, tableName);
        return count != null && count > 0;
    }

    public List<AdminTableResponse> findExistingTables() {
        return jdbcTemplate.query(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name",
                (rs, row) -> {
                    String name = rs.getString("table_name");
                    return new AdminTableResponse(name, countRowsExact(name), "Healthy");
                });
    }

    public boolean checkHealth() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    public List<DatabaseTableInfo> findTableInfo() {
        return jdbcTemplate.query(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name",
                (rs, row) -> {
                    String name = rs.getString("table_name");
                    return new DatabaseTableInfo(name, countRowsExact(name), "HEALTHY");
                });
    }

    private long countRowsExact(String tableName) {
        if (!tableName.matches("[a-zA-Z0-9_]+"))
            return 0;
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM \"" + tableName + "\"", Long.class);
        return count == null ? 0 : count;
    }
}
