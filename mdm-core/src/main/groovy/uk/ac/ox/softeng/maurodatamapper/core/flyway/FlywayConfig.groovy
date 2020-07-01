package uk.ac.ox.softeng.maurodatamapper.core.flyway

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import javax.sql.DataSource

/**
 * This is required to "hack" the {@link org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration}
 * which requires the DataSource bean to exist at auto configuration time, which it wont with Grails.
 *
 * This configuration makes an alias DataSource bean which will allow Flyway bean to be configured but will then delgate
 * through to the gorm one.
 *
 * https://stackoverflow.com/questions/43211960/how-do-i-configure-flyway-in-grails3-postgres/43214863#43214863
 *
 * All Application classes will need to also have {@link org.springframework.context.annotation.ComponentScan} annotation added to scan this
 * configuration in.
 */
@Configuration
class FlywayConfig {

    @Autowired
    DataSource dataSource

    @Bean
    @FlywayDataSource
    DataSource flywayDataSource() {
        return dataSource
    }
}
