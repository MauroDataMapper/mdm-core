/*
 * Copyright 2020-2024 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping

import grails.plugins.metadata.GrailsPlugin
import io.micronaut.core.order.Ordered
import org.grails.datastore.mapping.config.Property
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.orm.hibernate.cfg.Mapping

/**
 * @since 04/11/2019
 */
abstract class PluginSchemaHibernateMappingContext extends DynamicHibernateMappingContext
    implements Ordered, Comparable<PluginSchemaHibernateMappingContext> {

    abstract String getPluginName()

    abstract String getSchemaName()

    String[] getFlywayLocations() {
        ["classpath:db/migration/${getSchemaName()}"].toArray(String[])
    }

    @Override
    boolean handlesDomainClass(Class domainClass) {
        GrailsPlugin annotation = domainClass.getAnnotation(GrailsPlugin) as GrailsPlugin
        annotation.name() == pluginName
    }

    @Override
    Property updateDomainMapping(PersistentEntity entity) {
        Mapping mapping = entity.mapping.mappedForm as Mapping
        mapping.table([schema: schemaName])
        null
    }

    @Override
    int compareTo(PluginSchemaHibernateMappingContext that) {
        this.order <=> that.order ?: this.pluginName <=> that.pluginName
    }
}
