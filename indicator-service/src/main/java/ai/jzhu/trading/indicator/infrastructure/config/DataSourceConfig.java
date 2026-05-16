package ai.jzhu.trading.indicator.infrastructure.config;

import org.springframework.context.annotation.Configuration;

/**
 * DataSource is auto-configured from application.yml via spring-boot-starter-data-jdbc.
 * JdbcTemplate bean is provided automatically and injected into JdbcIndicatorRepository.
 */
@Configuration
public class DataSourceConfig {
}
