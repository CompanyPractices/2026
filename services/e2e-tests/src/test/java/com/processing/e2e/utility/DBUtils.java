package com.processing.e2e.utility;

import com.processing.e2e.E2EBaseTest;


import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;


public class DBUtils {


    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(E2EBaseTest.jdbcUrl(), E2EBaseTest.DB_USER, E2EBaseTest.DB_PASSWORD);
    }


    public long queryLong(String sql, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                throw new SQLException("No result for: " + sql);
            }
        }
    }


    public String queryString(String sql, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
                throw new SQLException("No result for: " + sql);
            }
        }
    }

    public Map<String, Object> queryRow(String sql, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException(("No result for: " + sql));
                }
                Map<String, Object> row = new LinkedHashMap<>();
                ResultSetMetaData meta = rs.getMetaData();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    row.put(meta.getColumnName(i), rs.getObject(i));
                }

                return row;
            }
        }
    }

    private static void bindParams(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }
}
