package pl.santander.featureflag.spring;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import pl.santander.featureflag.engine.*;

import java.util.Set;

@AutoConfiguration
@EnableConfigurationProperties(CanaryProperties.class)
public class CanaryAutoConfiguration {

    @Bean
    public SpelExpressionParser spelExpressionParser() {
        return new SpelExpressionParser(new SpelParserConfiguration(false,false));
    }

    @Bean
    public NormalizeUtil normalizeUtil(){ return new NormalizeUtil(); }

    @Bean
    public MetricsProvider metricsProvider(MeterRegistry reg){ return new MicrometerMetricsProvider(reg); }

    @Bean
    public GuardFunctions guardFunctions(MetricsProvider metrics){ return new GuardFunctions(metrics); }

    @Bean
    public WhitelistMethodResolver whitelistMethodResolver() {
        return new WhitelistMethodResolver(Set.of("error_rate", "p95", "diff_rate"));
    }

    @Bean
    public GuardEngine guardEngine(SpelExpressionParser parser, NormalizeUtil norm,
                                   GuardFunctions fun, WhitelistMethodResolver white) {
        return new GuardEngine(parser, norm, fun, white);
    }
}
