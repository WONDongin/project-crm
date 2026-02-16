package com.project.crm.config;


import com.project.crm.domain.user.User;

import com.project.crm.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
@Profile("local")
@RequiredArgsConstructor
public class InitDataConfig implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${init.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) throws Exception {

        if (userRepository.findByEmail("admin@gmail.com").isEmpty()) {

            User admin = new User();
            admin.setEmail("admin@gmail.com");
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setName("관리자");
            admin.setRoles("ADMIN");
            admin.setSpecialty("ALL");
            admin.setStatus("ACTIVE");

            userRepository.save(admin);

            System.out.println("관리자 계정 생성 완료");
        }
    }
}
