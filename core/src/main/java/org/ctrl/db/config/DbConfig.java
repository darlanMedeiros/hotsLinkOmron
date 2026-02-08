package org.ctrl.db.config;

import java.util.Objects;
import javax.sql.DataSource;
import org.ctrl.db.controller.DmValueController;
import org.ctrl.db.controller.RrValueController;
import org.ctrl.db.controller.TagController;
import org.ctrl.db.repository.JdbcMemoryValueRepository;
import org.ctrl.db.repository.JdbcTagRepository;
import org.ctrl.db.repository.MemoryValueRepository;
import org.ctrl.db.repository.TagRepository;
import org.ctrl.db.service.DmValueService;
import org.ctrl.db.service.RrValueService;
import org.ctrl.db.service.TagService;
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
        return new JdbcTemplate(Objects.requireNonNull(dataSource, "dataSource"));
    }

    @Bean
    public MemoryValueRepository memoryValueRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcMemoryValueRepository(jdbcTemplate);
    }

    @Bean
    public TagRepository tagRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcTagRepository(jdbcTemplate);
    }

    @Bean
    public TagService tagService(TagRepository repository, JdbcTemplate jdbcTemplate) {
        return new TagService(repository, jdbcTemplate);
    }

    @Bean
    public DmValueService dmValueService(MemoryValueRepository repository) {
        return new DmValueService(repository);
    }

    @Bean
    public DmValueController dmValueController(DmValueService service) {
        return new DmValueController(service);
    }

    @Bean
    public RrValueService rrValueService(MemoryValueRepository repository) {
        return new RrValueService(repository);
    }

    @Bean
    public RrValueController rrValueController(RrValueService service) {
        return new RrValueController(service);
    }

    @Bean
    public TagController tagController(TagService service) {
        return new TagController(service);
    }

    private String getenv(String name, String fallback) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value;
    }
}
