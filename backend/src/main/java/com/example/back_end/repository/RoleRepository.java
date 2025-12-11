package com.example.back_end.repository;

import com.example.back_end.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByCode(String code);
    List<Role> findByCodeIn(Collection<String> codes);
}

