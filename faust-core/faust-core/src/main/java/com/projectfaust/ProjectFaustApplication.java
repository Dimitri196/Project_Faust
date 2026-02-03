package com.projectfaust;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com.projectfaust.entity")
@EnableJpaRepositories("com.projectfaust.repository")
public class ProjectFaustApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProjectFaustApplication.class, args);
	}

}
