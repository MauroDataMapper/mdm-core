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
package uk.ac.ox.softeng.maurodatamapper.core.facet


import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.CatalogueItemAware
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CreatorAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.traits.domain.CreatorAware
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.databinding.BindUsing
import grails.gorm.DetachedCriteria
import grails.rest.Resource

@Resource(readOnly = false, formats = ['json', 'xml'])
class ModelImport implements CatalogueItemAware, CreatorAware {

    UUID id

    CatalogueItem importedCatalogueItem
    UUID importedCatalogueItemId
    String importedCatalogueItemDomainType

    static constraints = {
        CallableConstraints.call(CreatorAwareConstraints, delegate)

        //TODO don't allow same import twice on one catalogue item
        catalogueItemId nullable: true, validator: {val, obj ->
            if (!val && !obj.catalogueItem) return ['default.null.message']
            if (val == obj.importedCatalogueItemId && obj.catalogueItemDomainType == obj.importedCatalogueItemDomainType) {
                return ['invalid.same.property.message', 'importedCatalogueItem']
            }
        }
    }

    static mapping = {
        batchSize 20
        catalogueItemId index: 'model_import_catalogue_item_idx'
        importedCatalogueItemId index: 'model_import_imported_catalogue_item_idx'
    }

    // Required to prevent bidirectional mapping to catalogue items
    static mappedBy = [
        'catalogueItem'      : 'none',
        'importedCatalogueItem': 'none'
    ]

    static transients = ['importedCatalogueItem', 'catalogueItem']

    ModelImport() {
    }

    @Override
    String getDomainType() {
        ModelImport.simpleName
    }


    @Override
    String getEditLabel() {
        "ModelImport:${importedCatalogueItemDomainType}:${importedCatalogueItemId}"
    }

    void setTargetCatalogueItem(CatalogueItem catalogueItem) {
        importedCatalogueItem = catalogueItem
        importedCatalogueItemDomainType = catalogueItem.domainType
        importedCatalogueItemId = catalogueItem.id
    }

    static DetachedCriteria<ModelImport> by() {
        new DetachedCriteria<ModelImport>(ModelImport)
    }

    static DetachedCriteria<ModelImport> byCatalogueItemId(Serializable catalogueItemId) {
        by().eq('catalogueItemId', Utils.toUuid(catalogueItemId))
    }

    static DetachedCriteria<ModelImport> byCatalogueItemIdAndId(Serializable catalogueItemId, Serializable resourceId) {
        byCatalogueItemId(catalogueItemId).idEq(Utils.toUuid(resourceId))
    }

    static DetachedCriteria<ModelImport> byTargetCatalogueItemId(Serializable catalogueItemId) {
        by().eq('importedCatalogueItemId', Utils.toUuid(catalogueItemId))
    }

    static DetachedCriteria<ModelImport> byAnyCatalogueItemId(Serializable catalogueItemId) {
        by().or {
            eq 'catalogueItemId', Utils.toUuid(catalogueItemId)
            eq 'importedCatalogueItemId', Utils.toUuid(catalogueItemId)
        }
    }

    static DetachedCriteria<ModelImport> withFilter(DetachedCriteria<ModelImport> criteria, Map filters) {
        criteria
    }

    /**
     * Select the distinct IDs of catalogue items which are imported into the catalogue item with the specified ID.
     * Used by other domains to select which of themselves have been imported into a particular model.
     * Will do a query like 'DISTINCT(imported_catalogue_item_id) FROM core.model_import WHERE catalogue_item_id = catalogueItemId'
     *
     * @param catalogueItemId The ID of the catalogue item which imported another catalogue item
     * @return DetachedCriteria<ModelImport> Projection for distinct imported_catalogue_item_id
     */
    static DetachedCriteria<ModelImport> importedByCatalogueItemId(Serializable catalogueItemId) {
        byCatalogueItemId(catalogueItemId)
        .projections {
            distinct('importedCatalogueItemId')
        }
    }    
}