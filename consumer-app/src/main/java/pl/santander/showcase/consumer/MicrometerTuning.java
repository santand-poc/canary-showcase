package pl.santander.showcase.consumer;

import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Na potrzeby prezentacji — szybsze uwzględnianie
 * p95 - latency update defaultowo latency jest liczone w minutach tu skrócone do 10s
 */
@Configuration
public class MicrometerTuning {

    @Bean
    MeterFilter httpServerP95FastWindow() {
        return new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                if ("http.server.requests".equals(id.getName())) {
                    return DistributionStatisticConfig.builder()
                            .percentiles(0.95)
                            .percentilePrecision(1)
                            .expiry(Duration.ofSeconds(10))
                            .bufferLength(2)
                            .build()
                            .merge(config);
                }
                return config;
            }
        };
    }
}
