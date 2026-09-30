package com.example.modbus_api.config;

import com.example.modbus_api.security.model.AppUser;
import com.example.modbus_api.security.model.Role;
import com.example.modbus_api.security.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.example.modbus_api.service.DefaultCredentialsRotation credentials;

    public DatabaseInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder, com.example.modbus_api.service.DefaultCredentialsRotation credentials) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.credentials = credentials;
    }

    @Override
    public void run(String... args) throws Exception {
        credentials.applyOnce();

        AppUser readerUser = userRepository.findByUsername("READER").orElse(null);
        if (readerUser == null) {
            AppUser legacyReadOnlyUser = userRepository.findByUsername("readonly").orElse(null);
            if (legacyReadOnlyUser != null) {
                legacyReadOnlyUser.setUsername("READER");
                legacyReadOnlyUser.setPassword(passwordEncoder.encode("Guest6"));
                legacyReadOnlyUser.setRole(Role.ROLE_READONLY);
                userRepository.save(legacyReadOnlyUser);
                logger.info("Read-only user renamed and credentials updated");
            } else {
                readerUser = AppUser.builder()
                        .username("READER")
                        .password(passwordEncoder.encode("Guest6"))
                        .role(Role.ROLE_READONLY)
                        .build();
                userRepository.save(readerUser);
                logger.info("Read-only user created");
            }
        } else {
            readerUser.setPassword(passwordEncoder.encode("Guest6"));
            readerUser.setRole(Role.ROLE_READONLY);
            userRepository.save(readerUser);
            logger.info("Read-only user credentials updated");
        }

        AppUser mgmUser = userRepository.findByUsername("MGM").orElse(null);
        if (mgmUser == null) {
            mgmUser = AppUser.builder()
                    .username("MGM")
                    .password(passwordEncoder.encode("Mgm246"))
                    .role(Role.ROLE_MGM)
                    .build();
            userRepository.save(mgmUser);
            logger.info("MGM user created");
        } else {
            mgmUser.setPassword(passwordEncoder.encode("Mgm246"));
            mgmUser.setRole(Role.ROLE_MGM);
            userRepository.save(mgmUser);
            logger.info("MGM user credentials updated");
        }


        AppUser dimaUser = userRepository.findByUsername("DIMA").orElse(null);
        if (dimaUser == null) {
            dimaUser = AppUser.builder()
                    .username("DIMA")
                    .password(passwordEncoder.encode("Dima357"))
                    .role(Role.ROLE_USER)
                    .build();
            userRepository.save(dimaUser);
            logger.info("DIMA user created");
        } else {
            dimaUser.setPassword(passwordEncoder.encode("Dima357"));
            dimaUser.setRole(Role.ROLE_USER);
            userRepository.save(dimaUser);
            logger.info("DIMA user credentials updated");
        }

        AppUser sergeyUser = userRepository.findByUsername("SERGEY").orElse(null);
        if (sergeyUser == null) {
            sergeyUser = AppUser.builder()
                    .username("SERGEY")
                    .password(passwordEncoder.encode("Sergey468"))
                    .role(Role.ROLE_USER)
                    .build();
            userRepository.save(sergeyUser);
            logger.info("SERGEY user created");
        } else {
            sergeyUser.setPassword(passwordEncoder.encode("Sergey468"));
            sergeyUser.setRole(Role.ROLE_USER);
            userRepository.save(sergeyUser);
            logger.info("SERGEY user credentials updated");
        }
    }
}
