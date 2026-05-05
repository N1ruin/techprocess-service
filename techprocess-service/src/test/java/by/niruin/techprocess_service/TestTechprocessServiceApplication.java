package by.niruin.techprocess_service;

import org.springframework.boot.SpringApplication;

public class TestTechprocessServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(TechprocessServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
