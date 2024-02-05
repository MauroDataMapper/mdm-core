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

import org.grails.datastore.mapping.config.Property
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.types.Association
import org.grails.orm.hibernate.cfg.PropertyConfig
import org.hibernate.cfg.ImprovedNamingStrategy

/**
 * @since 09/03/2020
 */
abstract class JoinTableDynamicHibernateMappingContext extends DynamicHibernateMappingContext {

    public final static ImprovedNamingStrategy NAMING_STRATEGY = ImprovedNamingStrategy.INSTANCE as ImprovedNamingStrategy

    abstract String getPropertyName()

    String getInverseSidePropertyName(PersistentEntity entity) {
        entity.decapitalizedName
    }

    String getCascade() {
        'all-delete-orphan'
    }

    String getJoinTableName(PersistentEntity entity) {
        "join_${getInverseSidePropertyName(entity).toLowerCase()}_to_${propertyName}"
    }

    String getJoinTableKey(PersistentEntity entity) {
        "${entity.decapitalizedName.toLowerCase()}_${entity.identity.name}"
    }

    String getJoinTableColumn(PersistentEntity entity) {
        PersistentEntity associatedEntity = (entity.getPropertyByName(propertyName) as Association).associatedEntity
        "${NAMING_STRATEGY.propertyToColumnName(associatedEntity.decapitalizedName)}_${associatedEntity.identity.name}"
    }

    Map getJoinTableMap(PersistentEntity entity) {
        [name  : getJoinTableName(entity),
         key   : getJoinTableKey(entity),
         column: getJoinTableColumn(entity)]
    }

    @Override
    Property updateDomainMapping(PersistentEntity entity) {
        PropertyConfig property = updateProperty(entity, propertyName, [cascade: cascade])
        property.joinTable(getJoinTableMap(entity))
        property

    }
}
