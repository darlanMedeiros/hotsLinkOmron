package org.ctrl.db.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;
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
import org.ctrl.db.repository.TurnoRepository;
import org.ctrl.db.repository.QualidadeRepository;
import org.ctrl.db.repository.DefeitoRepository;
import org.ctrl.db.service.QualidadeService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
public class DbConfig {

    private static final String DEFAULT_DB_URL = "jdbc:postgresql://localhost:5432/omron?currentSchema=public";
    private static final String DEFAULT_DB_USER = "omron_user";
    private static final String DEFAULT_DB_PASSWORD = "admin";

    @Bean
    public DataSource dataSource() {
        String profile = resolveProfile();
        Properties profileProps = loadProfileProperties(profile);

        String url = resolveValue("DB_URL", "db.url", profileProps, DEFAULT_DB_URL);
        String user = resolveValue("DB_USER", "db.user", profileProps, DEFAULT_DB_USER);
        String password = resolveValue("DB_PASSWORD", "db.password", profileProps, DEFAULT_DB_PASSWORD);

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

    @Bean
    public TurnoRepository turnoRepository(JdbcTemplate jdbcTemplate) {
        return new TurnoRepository(jdbcTemplate);
    }

    @Bean
    public QualidadeRepository qualidadeRepository(JdbcTemplate jdbcTemplate) {
        return new QualidadeRepository(jdbcTemplate);
    }

    @Bean
    public DefeitoRepository defeitoRepository(JdbcTemplate jdbcTemplate) {
        return new DefeitoRepository(jdbcTemplate);
    }

    @Bean
    public QualidadeService qualidadeService(QualidadeRepository repository, TurnoRepository turnoRepository) {
        return new QualidadeService(repository, turnoRepository);
    }

    private String resolveProfile() {
        String profile = firstNonBlank(
                System.getProperty("app.env"),
                System.getenv("APP_ENV"),
                System.getProperty("spring.profiles.active"),
                System.getenv("SPRING_PROFILES_ACTIVE"),
                "dev");

        if (profile.contains(",")) {
            profile = profile.split(",", 2)[0].trim();
        }
        return profile;
    }

    private Properties loadProfileProperties(String profile) {
        Properties props = new Properties();
        String resourceName = "db-" + profile + ".properties";

        try (InputStream in = DbConfig.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (in != null) {
                props.load(in);
                return props;
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Erro ao carregar " + resourceName, ex);
        }

        if (!"dev".equalsIgnoreCase(profile)) {
            try (InputStream in = DbConfig.class.getClassLoader().getResourceAsStream("db-dev.properties")) {
                if (in != null) {
                    props.load(in);
                }
            } catch (IOException ex) {
                throw new IllegalStateException("Erro ao carregar db-dev.properties", ex);
            }
        }

        return props;
    }

    private String resolveValue(String envKey, String propKey, Properties profileProps, String fallback) {
        String value = firstNonBlank(
                System.getenv(envKey),
                System.getProperty(envKey),
                System.getProperty(propKey),
                profileProps.getProperty(propKey),
                fallback);
        return value;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }
}
