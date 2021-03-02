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
class ModelExtend implements CatalogueItemAware, CreatorAware {

    UUID id
    CatalogueItem extendedCatalogueItem
    UUID extendedCatalogueItemId
    String extendedCatalogueItemDomainType

    static constraints = {
        CallableConstraints.call(CreatorAwareConstraints, delegate)
        catalogueItemId nullable: true, validator: {val, obj ->
            if (val) return true
            if (!val && obj.catalogueItem && !obj.catalogueItem.ident()) return true
            ['default.null.message']
        }
        catalogueItemDomainType nullable: false, blank: false

        /**
         * Custom constraint validation on the extendedCatalogueItemId to prevent duplicate extends.
         * A duplicate is defined as two records having the same catalogueItemId and extendedCatalogueItemId.
         * Using a unique constraint did not work as it fails in cases where the catalogueItem has not yet been created
         * (which occurs when copying a catalogue item and its facets).
         * So only when dealing with an already existing catalogue item the valdiator checks for an already
         * existing ModelExtend.
         */
        extendedCatalogueItemId nullable: false, validator: {UUID extendedCatalogueItemId, ModelExtend instance ->
            //If the catalogue item does not yet have an ID then this is a new catalogue item, 
            //so duplication is not possible
            if (!instance.catalogueItem.ident()) return true
            
            //Else this is an existing catalogue item, in which case check that the extend does not
            //already exist
            def alreadyExtended = ModelExtend.withCriteria() {
                if (instance.id) {
                    ne('id', instance.id)
                }
                eq('catalogueItemId', instance.catalogueItemId)
                eq('extendedCatalogueItemId', instance.extendedCatalogueItemId)
            }

            if (alreadyExtended) {
                return ['default.not.unique.message']
            } else {
                return true
            }
        }        
        extendedCatalogueItemDomainType nullable: false, blank: false
    }

    static mapping = {
        batchSize 20
        catalogueItemId index: 'model_extend_catalogue_item_idx'
        extendedCatalogueItemId index: 'model_extend_extended_catalogue_item_idx'
    }

    // Required to prevent bidirectional mapping to catalogue items
    static mappedBy = [
        'catalogueItem'      : 'none',
        'extendedCatalogueItem': 'none'
    ]

    static transients = ['extendedCatalogueItem', 'catalogueItem']

    ModelExtend() {
    }

    @Override
    String getDomainType() {
        ModelExtend.simpleName
    }


    @Override
    String getEditLabel() {
        "ModelExtend:${extendedCatalogueItemDomainType}:${extendedCatalogueItemId}"
    }

    void setExtendedCatalogueItem(CatalogueItem catalogueItem) {
        if (!catalogueItem) {
            return
        }
        
        extendedCatalogueItem = catalogueItem
        extendedCatalogueItemDomainType = catalogueItem.domainType
        extendedCatalogueItemId = catalogueItem.id
    }

    static DetachedCriteria<ModelExtend> by() {
        new DetachedCriteria<ModelExtend>(ModelExtend)
    }

    static DetachedCriteria<ModelExtend> byCatalogueItemId(Serializable catalogueItemId) {
        by().eq('catalogueItemId', Utils.toUuid(catalogueItemId))
    }

    static DetachedCriteria<ModelExtend> byCatalogueItemIdAndId(Serializable catalogueItemId, Serializable resourceId) {
        byCatalogueItemId(catalogueItemId).idEq(Utils.toUuid(resourceId))
    }

    static DetachedCriteria<ModelExtend> byCatalogueItemIdAndExtendedCatalogueItemId(Serializable catalogueItemId, Serializable extendedCatalogueItemId) {
        byCatalogueItemId(catalogueItemId)
        .eq('extendedCatalogueItemId', Utils.toUuid(extendedCatalogueItemId))
    }

    static DetachedCriteria<ModelExtend> byAnyCatalogueItemId(Serializable catalogueItemId) {
        by().or {
            eq 'catalogueItemId', Utils.toUuid(catalogueItemId)
            eq 'extendedCatalogueItemId', Utils.toUuid(catalogueItemId)
        }
    }

    static DetachedCriteria<ModelExtend> withFilter(DetachedCriteria<ModelExtend> criteria, Map filters) {
        criteria
    }

    /**
     * Select the distinct IDs of catalogue items which are extended by the catalogue item with the specified ID.
     * Used by other domains to select which of themselves have been extended into a particular model.
     * Will do a query like 'DISTINCT(extended_catalogue_item_id) FROM core.model_extend WHERE catalogue_item_id = catalogueItemId'
     *
     * @param catalogueItemId The ID of the catalogue item which extended another catalogue item
     * @return DetachedCriteria<ModelExtend> Projection for distinct extended_catalogue_item_id
     */
    static DetachedCriteria<ModelExtend> extendedByCatalogueItemId(Serializable catalogueItemId) {
        byCatalogueItemId(catalogueItemId)
        .projections {
            distinct('extendedCatalogueItemId')
        }
    }    
}