package org.ctrl.db;

import java.time.ZoneId;
import java.util.Objects;
import java.util.TimeZone;
import org.ctrl.db.config.DbConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.lang.NonNull;

@SpringBootApplication
@Import(DbConfig.class)
public class ApiApplication {

    public static void main(String[] args) {
        @NonNull TimeZone tz = Objects.requireNonNull(
                TimeZone.getTimeZone(ZoneId.systemDefault()),
                "timeZone");
        TimeZone.setDefault(tz);
        SpringApplication.run(ApiApplication.class, args);
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonTimeZoneCustomizer() {
        @NonNull TimeZone tz = Objects.requireNonNull(
                TimeZone.getTimeZone(ZoneId.systemDefault()),
                "timeZone");
        return builder -> builder.timeZone(tz);
    }
}
