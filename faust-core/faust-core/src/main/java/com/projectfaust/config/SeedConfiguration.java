package com.projectfaust.config;

import com.projectfaust.service.LocationSeedService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
@Slf4j
public class SeedConfiguration {

    @Bean
    CommandLineRunner initDatabase(LocationSeedService seedService) {
        return args -> {
            log.info("SYSTEM_INITIALIZATION: Verifying geographic data integrity...");
            seedService.seedLocations();
            log.info("SYSTEM_READY: Geographic backbone is up to date.");
        };
    }
}
