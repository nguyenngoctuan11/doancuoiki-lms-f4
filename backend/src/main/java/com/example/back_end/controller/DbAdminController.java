package com.example.back_end.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/db")
public class DbAdminController {
    @PersistenceContext
    private EntityManager em;

    private static final String SCHEMA = "dbo";

    @GetMapping("/tables")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> listTables() {
        @SuppressWarnings("unchecked")
        List<String> names = em.createNativeQuery(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE='BASE TABLE' AND TABLE_SCHEMA=:s ORDER BY TABLE_NAME")
                .setParameter("s", SCHEMA)
                .getResultList();
        Map<String,Object> res = new HashMap<>();
        res.put("schema", SCHEMA);
        res.put("tables", names);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/table/{name}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> readTable(@PathVariable("name") String name,
                                       @RequestParam(defaultValue = "0") int offset,
                                       @RequestParam(defaultValue = "50") int limit) {
        // Whitelist by checking INFORMATION_SCHEMA.TABLES
        @SuppressWarnings("unchecked")
        List<String> exists = em.createNativeQuery(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA=:s AND TABLE_NAME=:t")
                .setParameter("s", SCHEMA)
                .setParameter("t", name)
                .getResultList();
        if (exists.isEmpty()) {
            return ResponseEntity.badRequest().body("Unknown table: " + name);
        }
        // Columns
        @SuppressWarnings("unchecked")
        List<String> cols = em.createNativeQuery(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=:s AND TABLE_NAME=:t ORDER BY ORDINAL_POSITION")
                .setParameter("s", SCHEMA)
                .setParameter("t", name)
                .getResultList();

        // Data (TOP + OFFSET FETCH for SQL Server 2012+)
        int safeLimit = Math.max(1, Math.min(limit, 200));
        String sql = "SELECT * FROM " + SCHEMA + ".[" + name + "] ORDER BY 1 OFFSET " + offset + " ROWS FETCH NEXT " + safeLimit + " ROWS ONLY";
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql).getResultList();

        List<List<Object>> data = new ArrayList<>();
        for (Object o : rows) {
            if (o instanceof Object[]) data.add(Arrays.asList((Object[]) o));
            else data.add(Collections.singletonList(o));
        }
        Map<String,Object> res = new HashMap<>();
        res.put("table", name);
        res.put("columns", cols);
        res.put("rows", data);
        res.put("offset", offset);
        res.put("limit", safeLimit);
        return ResponseEntity.ok(res);
    }
}

