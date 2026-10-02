plugins {
	java
	jacoco
	id("org.springframework.boot") version "3.5.5"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.tenpo"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-database-postgresql")
	implementation("io.github.resilience4j:resilience4j-spring-boot3:2.3.0")
	implementation("io.github.resilience4j:resilience4j-reactor:2.3.0")
	implementation("com.bucket4j:bucket4j_jdk17-core:8.14.0")
	implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.8.6")
	implementation("org.postgresql:r2dbc-postgresql")

	runtimeOnly("org.postgresql:postgresql")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:junit-jupiter")
	testImplementation("org.testcontainers:postgresql")
	testImplementation("org.testcontainers:r2dbc")
	testImplementation("org.awaitility:awaitility")
	testImplementation("io.projectreactor.tools:blockhound:1.0.11.RELEASE")
	testImplementation("io.projectreactor.tools:blockhound-junit-platform:1.0.11.RELEASE")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.jar {
	enabled = false
}

tasks.test {
	useJUnitPlatform()
	jvmArgs("-XX:+AllowRedefinitionToAddDeleteMethods")
	systemProperty("api.version", System.getProperty("api.version", "1.41"))
	environment("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE", "/var/run/docker.sock")
	finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
	dependsOn(tasks.test)
}
