package com.example.back_end.config;

import com.example.back_end.model.Role;
import com.example.back_end.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StartupData {
    @Bean
    CommandLineRunner seedRoles(RoleRepository roleRepository) {
        return args -> {
            createIfMissing(roleRepository, "student", "Học viên");
            createIfMissing(roleRepository, "teacher", "Giảng viên");
            createIfMissing(roleRepository, "manager", "Quản lí");
        };
    }

    private void createIfMissing(RoleRepository repo, String code, String name) {
        repo.findByCode(code).orElseGet(() -> {
            Role r = new Role();
            r.setCode(code);
            r.setName(name);
            return repo.save(r);
        });
    }
}

