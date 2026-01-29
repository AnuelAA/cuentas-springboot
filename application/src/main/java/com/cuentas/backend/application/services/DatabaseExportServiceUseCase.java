package com.cuentas.backend.application.services;

import com.cuentas.backend.application.ports.driving.DatabaseExportServicePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class DatabaseExportServiceUseCase implements DatabaseExportServicePort {

    private static final Logger log = LoggerFactory.getLogger(DatabaseExportServiceUseCase.class);
    private final JdbcTemplate jdbcTemplate;

    public DatabaseExportServiceUseCase(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String exportDatabaseSchemaAndData(Long userId) {
        StringBuilder output = new StringBuilder();
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        output.append("-- ===========================================\n");
        output.append("-- Database Export\n");
        output.append("-- User ID: ").append(userId).append("\n");
        output.append("-- Generated: ").append(sdf.format(new Date())).append("\n");
        output.append("-- ===========================================\n\n");

        // Obtener lista de tablas en orden de dependencias
        List<String> tables = getTablesInOrder();
        
        // Generar DDL para cada tabla
        output.append("-- ===========================================\n");
        output.append("-- DDL: CREATE TABLE statements\n");
        output.append("-- ===========================================\n\n");
        
        for (String table : tables) {
            try {
                String ddl = getTableDDL(table);
                output.append(ddl).append("\n\n");
            } catch (Exception e) {
                log.warn("Error obteniendo DDL para tabla {}: {}", table, e.getMessage());
                output.append("-- Error obteniendo DDL para tabla: ").append(table).append("\n\n");
            }
        }

        // Generar INSERTs para cada tabla
        output.append("-- ===========================================\n");
        output.append("-- DATA: INSERT statements\n");
        output.append("-- ===========================================\n\n");
        
        for (String table : tables) {
            try {
                String inserts = getTableInserts(table, userId);
                if (!inserts.isEmpty()) {
                    output.append("-- Table: ").append(table).append("\n");
                    output.append(inserts).append("\n\n");
                }
            } catch (Exception e) {
                log.warn("Error obteniendo datos para tabla {}: {}", table, e.getMessage());
                output.append("-- Error obteniendo datos para tabla: ").append(table).append("\n\n");
            }
        }

        return output.toString();
    }

    private List<String> getTablesInOrder() {
        // Orden de tablas considerando dependencias (FKs)
        // Tablas base primero, luego las que dependen de ellas
        return List.of(
            "users",
            "user_settings",
            "categories",
            "asset_types",
            "liability_types",
            "assets",
            "liabilities",
            "asset_values",
            "liability_values",
            "interests",
            "interest_history",
            "transactions",
            "budgets"
        );
    }

    private String getTableDDL(String tableName) {
        // Consultar pg_get_tabledef o construir DDL desde information_schema
        String sql = "SELECT " +
                "    'CREATE TABLE ' || quote_ident(table_name) || ' (' || " +
                "    string_agg(" +
                "        quote_ident(column_name) || ' ' || " +
                "        CASE " +
                "            WHEN data_type = 'character varying' THEN 'VARCHAR(' || character_maximum_length || ')' " +
                "            WHEN data_type = 'character' THEN 'CHAR(' || character_maximum_length || ')' " +
                "            WHEN data_type = 'numeric' THEN 'DECIMAL(' || numeric_precision || ',' || numeric_scale || ')' " +
                "            WHEN data_type = 'integer' THEN 'INTEGER' " +
                "            WHEN data_type = 'bigint' THEN 'BIGINT' " +
                "            WHEN data_type = 'smallint' THEN 'SMALLINT' " +
                "            WHEN data_type = 'boolean' THEN 'BOOLEAN' " +
                "            WHEN data_type = 'date' THEN 'DATE' " +
                "            WHEN data_type = 'timestamp without time zone' THEN 'TIMESTAMP' " +
                "            WHEN data_type = 'timestamp with time zone' THEN 'TIMESTAMPTZ' " +
                "            WHEN data_type = 'text' THEN 'TEXT' " +
                "            WHEN data_type = 'serial' THEN 'SERIAL' " +
                "            WHEN data_type = 'bigserial' THEN 'BIGSERIAL' " +
                "            ELSE UPPER(data_type) " +
                "        END || " +
                "        CASE WHEN is_nullable = 'NO' THEN ' NOT NULL' ELSE '' END || " +
                "        CASE WHEN column_default IS NOT NULL THEN ' DEFAULT ' || column_default ELSE '' END, " +
                "        ', ' ORDER BY ordinal_position" +
                "    ) || ');' AS ddl " +
                "FROM information_schema.columns " +
                "WHERE table_schema = 'public' AND table_name = ? " +
                "GROUP BY table_name";
        
        try {
            String ddl = jdbcTemplate.queryForObject(sql, String.class, tableName);
            
            // Agregar constraints (PKs, FKs, etc.)
            String constraints = getTableConstraints(tableName);
            
            return ddl + (constraints.isEmpty() ? "" : "\n" + constraints);
        } catch (DataAccessException e) {
            // Fallback: usar pg_dump style
            return getTableDDLSimple(tableName);
        }
    }

    private String getTableDDLSimple(String tableName) {
        // Método simplificado usando pg_get_tabledef si está disponible
        String sql = "SELECT pg_get_tabledef(?)";
        try {
            return jdbcTemplate.queryForObject(sql, String.class, tableName);
        } catch (Exception e) {
            // Si no funciona, construir manualmente
            return "-- CREATE TABLE " + tableName + " (...); -- DDL no disponible";
        }
    }

    private String getTableConstraints(String tableName) {
        StringBuilder constraints = new StringBuilder();
        
        // Primary Keys
        String pkSql = "SELECT " +
                "    'ALTER TABLE ' || quote_ident(table_name) || ' ADD CONSTRAINT ' || " +
                "    quote_ident(constraint_name) || ' PRIMARY KEY (' || " +
                "    string_agg(quote_ident(column_name), ', ' ORDER BY ordinal_position) || ');' " +
                "FROM information_schema.table_constraints tc " +
                "JOIN information_schema.key_column_usage kcu " +
                "    ON tc.constraint_name = kcu.constraint_name " +
                "WHERE tc.table_schema = 'public' " +
                "    AND tc.table_name = ? " +
                "    AND tc.constraint_type = 'PRIMARY KEY' " +
                "GROUP BY table_name, constraint_name";
        
        try {
            List<String> pks = jdbcTemplate.queryForList(pkSql, String.class, tableName);
            for (String pk : pks) {
                constraints.append(pk).append("\n");
            }
        } catch (Exception e) {
            log.debug("No se pudieron obtener PKs para {}", tableName);
        }

        // Foreign Keys
        String fkSql = "SELECT " +
                "    'ALTER TABLE ' || quote_ident(tc.table_name) || ' ADD CONSTRAINT ' || " +
                "    quote_ident(tc.constraint_name) || ' FOREIGN KEY (' || " +
                "    string_agg(quote_ident(kcu.column_name), ', ' ORDER BY kcu.ordinal_position) || ') ' || " +
                "    'REFERENCES ' || quote_ident(ccu.table_schema) || '.' || quote_ident(ccu.table_name) || " +
                "    ' (' || string_agg(quote_ident(ccu.column_name), ', ' ORDER BY kcu.ordinal_position) || ')' || " +
                "    CASE WHEN rc.delete_rule = 'CASCADE' THEN ' ON DELETE CASCADE' " +
                "         WHEN rc.delete_rule = 'SET NULL' THEN ' ON DELETE SET NULL' " +
                "         ELSE '' END || ';' " +
                "FROM information_schema.table_constraints tc " +
                "JOIN information_schema.key_column_usage kcu " +
                "    ON tc.constraint_name = kcu.constraint_name " +
                "JOIN information_schema.constraint_column_usage ccu " +
                "    ON ccu.constraint_name = tc.constraint_name " +
                "JOIN information_schema.referential_constraints rc " +
                "    ON rc.constraint_name = tc.constraint_name " +
                "WHERE tc.table_schema = 'public' " +
                "    AND tc.table_name = ? " +
                "    AND tc.constraint_type = 'FOREIGN KEY' " +
                "GROUP BY tc.table_name, tc.constraint_name, ccu.table_schema, ccu.table_name, rc.delete_rule";
        
        try {
            List<String> fks = jdbcTemplate.queryForList(fkSql, String.class, tableName);
            for (String fk : fks) {
                constraints.append(fk).append("\n");
            }
        } catch (Exception e) {
            log.debug("No se pudieron obtener FKs para {}", tableName);
        }

        return constraints.toString();
    }

    private String getTableInserts(String tableName, Long userId) {
        StringBuilder inserts = new StringBuilder();
        
        // Obtener todas las columnas de la tabla
        String columnsSql = "SELECT column_name, data_type " +
                "FROM information_schema.columns " +
                "WHERE table_schema = 'public' AND table_name = ? " +
                "ORDER BY ordinal_position";
        
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(columnsSql, tableName);
        if (columns.isEmpty()) {
            return "";
        }

        List<String> columnNames = new ArrayList<>();
        for (Map<String, Object> col : columns) {
            columnNames.add((String) col.get("column_name"));
        }

        // Construir query SELECT
        String selectSql = "SELECT * FROM " + tableName;
        
        // Si la tabla tiene user_id, filtrar por userId
        if (columnNames.contains("user_id")) {
            selectSql += " WHERE user_id = ?";
        }

        try {
            List<Map<String, Object>> rows = columnNames.contains("user_id") 
                    ? jdbcTemplate.queryForList(selectSql, userId)
                    : jdbcTemplate.queryForList(selectSql);

            if (rows.isEmpty()) {
                return "-- No hay datos para la tabla " + tableName;
            }

            for (Map<String, Object> row : rows) {
                StringBuilder insert = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
                StringBuilder values = new StringBuilder("VALUES (");

                boolean first = true;
                for (String colName : columnNames) {
                    if (!first) {
                        insert.append(", ");
                        values.append(", ");
                    }
                    insert.append(colName);
                    
                    Object value = row.get(colName);
                    if (value == null) {
                        values.append("NULL");
                    } else {
                        values.append(formatValue(value));
                    }
                    first = false;
                }

                insert.append(") ").append(values).append(");");
                inserts.append(insert.toString()).append("\n");
            }
        } catch (Exception e) {
            log.error("Error generando INSERTs para tabla {}: {}", tableName, e.getMessage());
            return "-- Error generando INSERTs: " + e.getMessage();
        }

        return inserts.toString();
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        
        if (value instanceof String) {
            return "'" + escapeString((String) value) + "'";
        }
        
        if (value instanceof java.sql.Date) {
            return "'" + value.toString() + "'";
        }
        
        if (value instanceof java.sql.Timestamp) {
            return "'" + value.toString() + "'";
        }
        
        if (value instanceof Boolean) {
            return ((Boolean) value) ? "TRUE" : "FALSE";
        }
        
        // Números y otros tipos
        return value.toString();
    }

    private String escapeString(String str) {
        if (str == null) return "";
        return str.replace("'", "''").replace("\\", "\\\\");
    }
}

