package com.example.back_end.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class DbInfoController {

    @PersistenceContext
    private EntityManager em;

    @GetMapping("/dbinfo")
    public ResponseEntity<Map<String, Object>> dbinfo() {
        Object[] row = (Object[]) em.createNativeQuery("SELECT @@SERVERNAME as server_name, DB_NAME() as db_name").getSingleResult();
        Map<String, Object> info = new HashMap<>();
        info.put("server_name", row[0]);
        info.put("db_name", row[1]);
        return ResponseEntity.ok(info);
    }
}

