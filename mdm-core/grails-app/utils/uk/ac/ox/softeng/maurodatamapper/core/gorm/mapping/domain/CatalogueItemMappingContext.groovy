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
package uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.domain

import uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.DynamicHibernateMappingContext
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import org.grails.datastore.mapping.config.Property
import org.grails.datastore.mapping.model.PersistentEntity

/**
 * @since 31/10/2019
 */
class CatalogueItemMappingContext extends DynamicHibernateMappingContext {
    @Override
    boolean handlesDomainClass(Class domainClass) {
        Utils.parentClassIsAssignableFromChild(CatalogueItem, domainClass)
    }

    @Override
    Property updateDomainMapping(PersistentEntity entity) {
        updateProperty(entity, 'aliasesString', [type: 'text'])
    }
}
