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
class SemanticLink implements CatalogueItemAware, CreatorAware {

    UUID id

    @BindUsing({obj, source -> SemanticLinkType.findFromMap(source)})
    SemanticLinkType linkType
    CatalogueItem targetCatalogueItem
    UUID targetCatalogueItemId
    String targetCatalogueItemDomainType

    static constraints = {
        CallableConstraints.call(CreatorAwareConstraints, delegate)
        catalogueItemId nullable: true, validator: {val, obj ->
            if (!val && !obj.catalogueItem) return ['default.null.message']
            if (val == obj.targetCatalogueItemId && obj.catalogueItemDomainType == obj.targetCatalogueItemDomainType) {
                return ['invalid.same.property.message', 'targetCatalogueItem']
            }
        }
    }

    static mapping = {
        batchSize 20
        createdBy cascade: 'none', index: 'semantic_link_created_by_idx'
        catalogueItemId index: 'semantic_link_catalogue_item_idx'
        targetCatalogueItemId index: 'semantic_link_target_catalogue_item_idx'
    }

    // Required to prevent bidirectional mapping to catalogue items
    static mappedBy = [
        'catalogueItem'      : 'none',
        'targetCatalogueItem': 'none'
    ]

    static transients = ['targetCatalogueItem', 'catalogueItem']

    SemanticLink() {
    }

    @Override
    String getDomainType() {
        SemanticLink.simpleName
    }


    @Override
    String getEditLabel() {
        "SemanticLink:${linkType}:${targetCatalogueItemId}"
    }

    void setTargetCatalogueItem(CatalogueItem catalogueItem) {
        targetCatalogueItem = catalogueItem
        targetCatalogueItemDomainType = catalogueItem.domainType
        targetCatalogueItemId = catalogueItem.id
    }

    static DetachedCriteria<SemanticLink> by() {
        new DetachedCriteria<SemanticLink>(SemanticLink)
    }

    static DetachedCriteria<SemanticLink> byCatalogueItemId(Serializable catalogueItemId) {
        by().eq('catalogueItemId', Utils.toUuid(catalogueItemId))
    }

    static DetachedCriteria<SemanticLink> byCatalogueItemIdAndId(Serializable catalogueItemId, Serializable resourceId) {
        byCatalogueItemId(catalogueItemId).idEq(Utils.toUuid(resourceId))
    }

    static DetachedCriteria<SemanticLink> byTargetCatalogueItemId(Serializable catalogueItemId) {
        by().eq('targetCatalogueItemId', Utils.toUuid(catalogueItemId))
    }

    static DetachedCriteria<SemanticLink> byAnyCatalogueItemId(Serializable catalogueItemId) {
        by().or {
            eq 'catalogueItemId', Utils.toUuid(catalogueItemId)
            eq 'targetCatalogueItemId', Utils.toUuid(catalogueItemId)
        }
    }

    static DetachedCriteria<SemanticLink> bySourceCatalogueItemAndTargetCatalogueItemAndLinkType(CatalogueItem source, CatalogueItem target,
                                                                                                 SemanticLinkType linkType) {
        by().eq('catalogueItemId', Utils.toUuid(source.id))
            .eq('catalogueItemDomainType', source.domainType)
            .eq('targetCatalogueItemId', Utils.toUuid(target.id))
            .eq('targetCatalogueItemDomainType', target.domainType)
            .eq('linkType', linkType)
    }

    static DetachedCriteria<SemanticLink> bySourceCatalogueItemIdInListAndTargetCatalogueItemIdInListAndLinkType(List<UUID> sourceCatalogueItemIds,
        List<UUID> targetCatalogueItemIds,
        SemanticLinkType linkType) {
        by().inList('catalogueItemId', sourceCatalogueItemIds)
            .inList('targetCatalogueItemId',targetCatalogueItemIds)
            .eq('linkType', linkType)
    }

    static DetachedCriteria<SemanticLink> withFilter(DetachedCriteria<SemanticLink> criteria, Map filters) {
        if (filters.linkType) criteria = criteria.eq('linkType', SemanticLinkType.findForLabel(filters.linkType))
        criteria
    }
}