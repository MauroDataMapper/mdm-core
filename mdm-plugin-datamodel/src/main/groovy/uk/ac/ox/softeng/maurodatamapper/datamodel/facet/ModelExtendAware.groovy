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
package uk.ac.ox.softeng.maurodatamapper.datamodel.facet

import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.compiler.GrailsCompileStatic
import groovy.transform.SelfType
import org.grails.datastore.gorm.GormEntity

/**
 * @since 23/12/2020
 */
@SelfType(GormEntity)
@GrailsCompileStatic
trait ModelExtendAware {

    abstract Set<ModelExtend> getModelExtends()

    def addToModelExtends(ModelExtend add) {
        add.setModelItem(this as ModelItem)
        addTo('modelExtends', add)
    }

    def addToModelExtends(Map args) {
        addToModelExtends(new ModelExtend(args))
    }

    def removeFromModelExtends(ModelExtend modelExtend) {
        removeFrom('modelExtends', modelExtend)
    }

    def addToModelExtends(String extendedModelItemDomainType, UUID extendedModelItemId, User createdBy) {
        addToModelExtends(extendedModelItemDomainType, extendedModelItemId, createdBy.emailAddress)
    }

    def addToModelExtends(String extendedModelItemDomainType, UUID extendedModelItemId, String createdBy) {
        addToModelExtends(new ModelExtend(extendedModelItemDomainType: extendedModelItemDomainType,
                                          extendedModelItemId: extendedModelItemId,
                                          createdBy: createdBy))
    }
}
