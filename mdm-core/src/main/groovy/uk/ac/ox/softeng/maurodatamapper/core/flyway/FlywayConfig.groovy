/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
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
