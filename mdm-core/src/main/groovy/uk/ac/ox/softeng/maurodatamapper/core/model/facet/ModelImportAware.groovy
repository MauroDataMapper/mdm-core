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
package uk.ac.ox.softeng.maurodatamapper.core.model.facet

import uk.ac.ox.softeng.maurodatamapper.core.facet.ModelImport
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.compiler.GrailsCompileStatic
import groovy.transform.SelfType
import org.grails.datastore.gorm.GormEntity

/**
 * @since 03/12/2020
 */
@SelfType(GormEntity)
@GrailsCompileStatic
trait ModelImportAware {
    abstract Set<ModelImport> getModelImports()

    def addToModelImports(ModelImport add) {
        add.setCatalogueItem(this as CatalogueItem)
        addTo('modelImports', add)
    }

    def addToModelImports(Map args) {
        addToModelImports(new ModelImport(args))
    }

    def removeFromModelImports(ModelImport modelImports) {
        removeFrom('modelImports', modelImports)
    }

    def addToModelImports(String catalogueItemDomainType, UUID catalogueItemId, String importedCatalogueItemDomainType, UUID importedCatalogueItemId, User createdBy)  {
        addToModelImports(catalogueItemDomainType, catalogueItemId, importedCatalogueItemDomainType, importedCatalogueItemId, createdBy.emailAddress)
    }

    def addToModelImports(String catalogueItemDomainType, UUID catalogueItemId, String importedCatalogueItemDomainType, UUID importedCatalogueItemId, String createdBy)  {
        addToModelImports(new ModelImport(catalogueItemDomainType: catalogueItemDomainType,
                                          catalogueItemId: catalogueItemId,
                                          importedCatalogueItemDomainType: importedCatalogueItemDomainType, 
                                          importedCatalogueItemId: importedCatalogueItemId,
                                          createdBy: createdBy))
    }
}
