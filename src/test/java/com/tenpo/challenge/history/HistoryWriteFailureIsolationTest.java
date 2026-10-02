package com.tenpo.challenge.history;

import com.tenpo.challenge.history.application.port.out.CallHistoryWriterPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class HistoryWriteFailureIsolationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CallHistoryWriterPort callHistoryWriterPort;

    @BeforeEach
    void stubWriterFailure() {
        when(callHistoryWriterPort.save(any())).thenReturn(Mono.error(new RuntimeException("db down")));
    }

    @Test
    void calculateStillSucceedsWhenHistoryWriteFails() {
        webTestClient.post().uri("/api/v1/calculate")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"num1\": 5, \"num2\": 5}")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.result").isEqualTo(11);
    }
}
