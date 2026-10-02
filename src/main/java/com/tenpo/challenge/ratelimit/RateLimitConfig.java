package com.tenpo.challenge.ratelimit;

import com.tenpo.challenge.shared.web.ProblemDetailWriter;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig {

    @Bean
    public Bucket rateLimitBucket(RateLimitProperties properties) {
        Bandwidth bandwidth = Bandwidth.builder()
            .capacity(properties.capacity())
            .refillGreedy(properties.refillTokens(), properties.refillPeriod())
            .build();
        return Bucket.builder().addLimit(bandwidth).build();
    }

    @Bean
    public RateLimitFilter rateLimitFilter(Bucket rateLimitBucket, ProblemDetailWriter problemDetailWriter) {
        return new RateLimitFilter(rateLimitBucket, problemDetailWriter);
    }
}
