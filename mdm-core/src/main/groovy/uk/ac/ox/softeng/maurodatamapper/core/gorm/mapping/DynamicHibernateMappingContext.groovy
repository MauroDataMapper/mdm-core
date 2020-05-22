/*
 * Copyright 2020 University of Oxford
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


import org.grails.datastore.mapping.config.Property
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.orm.hibernate.cfg.PropertyConfig

/**
 * @since 31/10/2019
 */
abstract class DynamicHibernateMappingContext {

    abstract boolean handlesDomainClass(Class domainClass)

    abstract Property updateDomainMapping(PersistentEntity entity)

    PropertyConfig updateProperty(PersistentEntity entity, String propertyName, Map updatedConfig) {
        PropertyConfig config = getPropertyConfig(entity, propertyName)
        PropertyConfig.configureExisting(config, updatedConfig)
    }

    PropertyConfig getPropertyConfig(PersistentEntity entity, String propertyName) {
        PersistentProperty pathProp = entity.getPropertyByName(propertyName)
        pathProp.mapping.mappedForm as PropertyConfig
    }
}
