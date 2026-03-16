package com.example.modbus_api.config;

import com.example.modbus_api.security.model.AppUser;
import com.example.modbus_api.security.model.Role;
import com.example.modbus_api.security.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        AppUser admin = userRepository.findByUsername("admin").orElse(null);
        if (admin == null) {
            admin = AppUser.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("RA285Q7n"))
                    .role(Role.ROLE_ADMIN)
                    .build();
            userRepository.save(admin);
            logger.info("Admin user created: admin / RA285Q7n");
        } else {

            admin.setPassword(passwordEncoder.encode("RA285Q7n"));
            admin.setRole(Role.ROLE_ADMIN);
            userRepository.save(admin);
            logger.info("Admin user password updated: admin / RA285Q7n");
        }


        AppUser user = userRepository.findByUsername("user").orElse(null);
        if (user == null) {
            user = AppUser.builder()
                    .username("user")
                    .password(passwordEncoder.encode("RU804f*"))
                    .role(Role.ROLE_USER)
                    .build();
            userRepository.save(user);
            logger.info("Default user created: user / RU804f*");
        } else {
            user.setPassword(passwordEncoder.encode("RU804f*"));
            user.setRole(Role.ROLE_USER);
            userRepository.save(user);
            logger.info("User password updated: user / RU804f*");
        }


        AppUser readerUser = userRepository.findByUsername("READER").orElse(null);
        if (readerUser == null) {
            AppUser legacyReadOnlyUser = userRepository.findByUsername("readonly").orElse(null);
            if (legacyReadOnlyUser != null) {
                legacyReadOnlyUser.setUsername("READER");
                legacyReadOnlyUser.setPassword(passwordEncoder.encode("Guest6"));
                legacyReadOnlyUser.setRole(Role.ROLE_READONLY);
                userRepository.save(legacyReadOnlyUser);
                logger.info("Read-only user renamed and updated: READER / Guest6");
            } else {
                readerUser = AppUser.builder()
                        .username("READER")
                        .password(passwordEncoder.encode("Guest6"))
                        .role(Role.ROLE_READONLY)
                        .build();
                userRepository.save(readerUser);
                logger.info("Read-only user created: READER / Guest6");
            }
        } else {
            readerUser.setPassword(passwordEncoder.encode("Guest6"));
            readerUser.setRole(Role.ROLE_READONLY);
            userRepository.save(readerUser);
            logger.info("Read-only user password updated: READER / Guest6");
        }

        AppUser mgmUser = userRepository.findByUsername("MGM").orElse(null);
        if (mgmUser == null) {
            mgmUser = AppUser.builder()
                    .username("MGM")
                    .password(passwordEncoder.encode("Mgm246"))
                    .role(Role.ROLE_MGM)
                    .build();
            userRepository.save(mgmUser);
            logger.info("MGM user created: MGM / Mgm246");
        } else {
            mgmUser.setPassword(passwordEncoder.encode("Mgm246"));
            mgmUser.setRole(Role.ROLE_MGM);
            userRepository.save(mgmUser);
            logger.info("MGM user password updated: MGM / Mgm246");
        }


        AppUser dimaUser = userRepository.findByUsername("DIMA").orElse(null);
        if (dimaUser == null) {
            dimaUser = AppUser.builder()
                    .username("DIMA")
                    .password(passwordEncoder.encode("Dima357"))
                    .role(Role.ROLE_USER)
                    .build();
            userRepository.save(dimaUser);
            logger.info("DIMA user created: DIMA / Dima357");
        } else {
            dimaUser.setPassword(passwordEncoder.encode("Dima357"));
            dimaUser.setRole(Role.ROLE_USER);
            userRepository.save(dimaUser);
            logger.info("DIMA user password updated: DIMA / Dima357");
        }

        AppUser sergeyUser = userRepository.findByUsername("SERGEY").orElse(null);
        if (sergeyUser == null) {
            sergeyUser = AppUser.builder()
                    .username("SERGEY")
                    .password(passwordEncoder.encode("Sergey468"))
                    .role(Role.ROLE_USER)
                    .build();
            userRepository.save(sergeyUser);
            logger.info("SERGEY user created: SERGEY / Sergey468");
        } else {
            sergeyUser.setPassword(passwordEncoder.encode("Sergey468"));
            sergeyUser.setRole(Role.ROLE_USER);
            userRepository.save(sergeyUser);
            logger.info("SERGEY user password updated: SERGEY / Sergey468");
        }
    }
}