package org.ctrl.db;

import java.time.ZoneId;
import java.util.TimeZone;
import org.ctrl.db.config.DbConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(DbConfig.class)
public class ApiApplication {

    private static final ZoneId ZONE_ID = ZoneId.of("America/Sao_Paulo");
    private static final TimeZone TIME_ZONE = TimeZone.getTimeZone(ZONE_ID);

    public static void main(String[] args) {
        TimeZone.setDefault(TIME_ZONE);
        SpringApplication.run(ApiApplication.class, args);
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonTimeZoneCustomizer() {
        return builder -> builder.timeZone(TIME_ZONE);
    }
}
