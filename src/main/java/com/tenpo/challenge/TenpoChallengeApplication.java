package com.tenpo.challenge;

import jakarta.validation.Validation;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TenpoChallengeApplication {

	static {
		warmUpBeanValidationMessageInterpolation();
	}

	public static void main(String[] args) {
		SpringApplication.run(TenpoChallengeApplication.class, args);
	}

	private static void warmUpBeanValidationMessageInterpolation() {
		try (var factory = Validation.buildDefaultValidatorFactory()) {
			factory.getValidator().validate(new WarmUpTarget(null));
		}
	}

	private record WarmUpTarget(@NotNull Object value) {
	}
}
