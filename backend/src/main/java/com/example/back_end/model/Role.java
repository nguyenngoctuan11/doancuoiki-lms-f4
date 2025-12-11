package com.example.back_end.model;

import jakarta.persistence.*;

@Entity
@Table(name = "roles", schema = "dbo")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "code", nullable = false, unique = true, length = 64)
    private String code; // e.g. student, teacher, manager

    @Column(name = "name", nullable = false, length = 128)
    private String name; // e.g. Học viên, Giảng viên, Quản lí

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

