/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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

import uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.PluginSchemaHibernateMappingContext

import groovy.util.logging.Slf4j
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy

@Slf4j
class MdmFlywayMigationStrategy implements FlywayMigrationStrategy {

    @Autowired
    Collection<PluginSchemaHibernateMappingContext> pluginSchemaHibernateMappingContexts

    @Override
    void migrate(Flyway flyway) {
        log.debug('Migrating Flyway')
        pluginSchemaHibernateMappingContexts.sort().each { schemaContext ->
            log.debug('Migrating flyway for plugin {} using locations {} for schema {}', schemaContext.pluginName,
                      schemaContext.flywayLocations,
                      schemaContext.schemaName)
            new FluentConfiguration()
                .configuration(flyway.getConfiguration())
                .locations(schemaContext.getFlywayLocations())
                .schemas(schemaContext.schemaName)
                .load()
                .migrate()
            log.info('Flyway now controls the following schema: {}', schemaContext.schemaName)
        }
    }
}
