package com.tenpo.challenge.history.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenpo.challenge.history.application.port.in.RecordCallUseCase;
import com.tenpo.challenge.history.application.port.out.CallHistoryReaderPort;
import com.tenpo.challenge.history.application.port.out.CallHistoryWriterPort;
import com.tenpo.challenge.history.application.service.CallHistoryService;
import com.tenpo.challenge.history.infrastructure.adapter.in.web.CallHistoryFilter;
import com.tenpo.challenge.history.infrastructure.adapter.in.web.CallRecordFactory;
import com.tenpo.challenge.history.infrastructure.adapter.in.web.HistoryCaptureProperties;
import com.tenpo.challenge.history.infrastructure.adapter.out.persistence.R2dbcCallHistoryAdapter;
import com.tenpo.challenge.shared.web.ProblemDetailWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(HistoryCaptureProperties.class)
public class HistoryConfig {

    @Bean(destroyMethod = "dispose")
    public Scheduler historyScheduler(
            @Value("${history.writer.thread-cap}") int threadCap,
            @Value("${history.writer.queued-task-cap}") int queuedTaskCap) {
        return Schedulers.newBoundedElastic(threadCap, queuedTaskCap, "history-writer");
    }

    @Bean
    public CallHistoryWriterPort callHistoryWriterPort(DatabaseClient databaseClient) {
        R2dbcCallHistoryAdapter adapter = new R2dbcCallHistoryAdapter(databaseClient);
        return adapter::save;
    }

    @Bean
    public CallHistoryReaderPort callHistoryReaderPort(DatabaseClient databaseClient) {
        R2dbcCallHistoryAdapter adapter = new R2dbcCallHistoryAdapter(databaseClient);
        return adapter::findSlice;
    }

    @Bean
    public CallHistoryService callHistoryService(CallHistoryWriterPort writerPort, CallHistoryReaderPort readerPort) {
        return new CallHistoryService(writerPort, readerPort);
    }

    @Bean
    public CallRecordFactory callRecordFactory(ObjectMapper objectMapper, Clock clock) {
        return new CallRecordFactory(objectMapper, clock);
    }

    @Bean
    public CallHistoryFilter callHistoryFilter(
            RecordCallUseCase recordCallUseCase,
            CallRecordFactory callRecordFactory,
            ProblemDetailWriter problemDetailWriter,
            Scheduler historyScheduler,
            HistoryCaptureProperties captureProperties) {
        return new CallHistoryFilter(
            recordCallUseCase,
            callRecordFactory,
            problemDetailWriter,
            historyScheduler,
            (int) captureProperties.maxRequestBytes().toBytes(),
            (int) captureProperties.maxResponseBytes().toBytes());
    }
}
