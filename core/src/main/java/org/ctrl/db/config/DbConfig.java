package org.ctrl.db.config;

import javax.sql.DataSource;
import org.ctrl.db.controller.DmValueController;
import org.ctrl.db.controller.RrValueController;
import org.ctrl.db.repository.DmValueRepository;
import org.ctrl.db.repository.JdbcDmValueRepository;
import org.ctrl.db.repository.JdbcRrValueRepository;
import org.ctrl.db.repository.RrValueRepository;
import org.ctrl.db.service.DmValueService;
import org.ctrl.db.service.RrValueService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
public class DbConfig {

    @Bean
    public DataSource dataSource() {
        String url = getenv("DB_URL", "jdbc:postgresql://localhost:5432/omron?currentSchema=public");
        String user = getenv("DB_USER", "omron_user");
        String password = getenv("DB_PASSWORD", "admin");

        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setUrl(url);
        ds.setUsername(user);
        ds.setPassword(password);
        return ds;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public DmValueRepository dmValueRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcDmValueRepository(jdbcTemplate);
    }

    @Bean
    public DmValueService dmValueService(DmValueRepository repository) {
        return new DmValueService(repository);
    }

    @Bean
    public DmValueController dmValueController(DmValueService service) {
        return new DmValueController(service);
    }

    @Bean
    public RrValueRepository rrValueRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcRrValueRepository(jdbcTemplate);
    }

    @Bean
    public RrValueService rrValueService(RrValueRepository repository) {
        return new RrValueService(repository);
    }

    @Bean
    public RrValueController rrValueController(RrValueService service) {
        return new RrValueController(service);
    }

    private String getenv(String name, String fallback) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value;
    }
}
