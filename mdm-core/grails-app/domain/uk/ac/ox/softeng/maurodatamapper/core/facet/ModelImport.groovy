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

import groovy.util.logging.Slf4j

@Slf4j
@Resource(readOnly = false, formats = ['json', 'xml'])
class ModelImport implements CatalogueItemAware, CreatorAware {

    UUID id
    CatalogueItem importedCatalogueItem
    UUID importedCatalogueItemId
    String importedCatalogueItemDomainType

    static constraints = {
        CallableConstraints.call(CreatorAwareConstraints, delegate)
        catalogueItemId nullable: true, validator: {val, obj ->
            if (val) return true
            if (!val && obj.catalogueItem && !obj.catalogueItem.ident()) return true
            ['default.null.message']
        }
        catalogueItemDomainType nullable: false, blank: false

        /**
         * Custom constraint validation on the importedCatalogueItemId to prevent duplicate imports.
         * A duplicate is defined as two records having the same catalogueItemId and importedCatalogueItemId.
         * Using a unique constraint did not work as it fails in cases where the catalogueItem has not yet been created
         * (which occurs when copying a catalogue item and its facets).
         * So only when dealing with an already existing catalogue item the valdiator checks for an already
         * existing ModelImport.
         */
        importedCatalogueItemId nullable: false, validator: {UUID importedCatalogueItemId, ModelImport instance ->
            //If the catalogue item does not yet have an ID then this is a new catalogue item, 
            //so duplication is not possible
            if (!instance.catalogueItem.ident()) return true
            
            //Else this is an existing catalogue item, in which case check that the import does not
            //already exist
            def alreadyImported = ModelImport.withCriteria() {
                if (instance.id) {
                    ne('id', instance.id)
                }
                eq('catalogueItemId', instance.catalogueItemId)
                eq('importedCatalogueItemId', instance.importedCatalogueItemId)
            }

            if (alreadyImported) {
                return ['default.not.unique.message']
            } else {
                return true
            }
        }        
        importedCatalogueItemDomainType nullable: false, blank: false
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

    void setImportedCatalogueItem(CatalogueItem catalogueItem) {
        if (!catalogueItem) {
            return
        }
        
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

    static DetachedCriteria<ModelImport> byCatalogueItemIdAndImportedCatalogueItemId(Serializable catalogueItemId, Serializable importedCatalogueItemId) {
        byCatalogueItemId(catalogueItemId)
        .eq('importedCatalogueItemId', Utils.toUuid(importedCatalogueItemId))
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