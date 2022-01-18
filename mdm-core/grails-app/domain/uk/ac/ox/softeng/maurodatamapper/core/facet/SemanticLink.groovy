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

import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.MultiFacetItemAware
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CreatorAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.databinding.BindUsing
import grails.gorm.DetachedCriteria
import grails.rest.Resource

@Resource(readOnly = false, formats = ['json', 'xml'])
class SemanticLink implements MultiFacetItemAware {

    UUID id

    @BindUsing({ obj, source -> SemanticLinkType.findFromMap(source) })
    SemanticLinkType linkType
    MultiFacetAware targetMultiFacetAwareItem
    UUID targetMultiFacetAwareItemId
    String targetMultiFacetAwareItemDomainType
    Boolean unconfirmed

    static constraints = {
        CallableConstraints.call(CreatorAwareConstraints, delegate)
        multiFacetAwareItemId nullable: true, validator: {val, obj ->
            if (!val && !obj.multiFacetAwareItem) return ['default.null.message']
            if (val == obj.targetMultiFacetAwareItemId && obj.multiFacetAwareItemDomainType == obj.targetMultiFacetAwareItemDomainType) {
                return ['invalid.same.property.message', 'targetMultiFacetAwareItem']
            }
        }
    }

    static mapping = {
        batchSize 20
        multiFacetAwareItemId index: 'semantic_link_catalogue_item_idx'
        targetMultiFacetAwareItemId index: 'semantic_link_target_catalogue_item_idx'
    }

    // Required to prevent bidirectional mapping to catalogue items
    static mappedBy = [
        'multiFacetAwareItem'      : 'none',
        'targetMultiFacetAwareItem': 'none'
    ]

    static transients = ['targetMultiFacetAwareItem', 'multiFacetAwareItem']

    SemanticLink() {
        unconfirmed = false
    }

    @Override
    String getDomainType() {
        SemanticLink.simpleName
    }

    @Override
    String getPathPrefix() {
        'sl'
    }

    @Override
    String getPathIdentifier() {
        "${linkType}.${targetMultiFacetAwareItemId}"
    }

    @Override
    String getEditLabel() {
        "SemanticLink:${linkType}:${targetMultiFacetAwareItemId}"
    }

    void setTargetMultiFacetAwareItem(MultiFacetAware multiFacetAware) {
        targetMultiFacetAwareItem = multiFacetAware
        targetMultiFacetAwareItemDomainType = multiFacetAware.domainType
        targetMultiFacetAwareItemId = multiFacetAware.id
    }

    static DetachedCriteria<SemanticLink> by() {
        new DetachedCriteria<SemanticLink>(SemanticLink)
    }

    static DetachedCriteria<SemanticLink> byMultiFacetAwareItemId(Serializable multiFacetAwareItemId) {
        by().eq('multiFacetAwareItemId', Utils.toUuid(multiFacetAwareItemId))
    }

    static DetachedCriteria<SemanticLink> byMultiFacetAwareItemIdAndId(Serializable multiFacetAwareItemId, Serializable resourceId) {
        byMultiFacetAwareItemId(multiFacetAwareItemId).idEq(Utils.toUuid(resourceId))
    }

    static DetachedCriteria<SemanticLink> byTargetMultiFacetAwareItemId(Serializable multiFacetAwareItemId) {
        by().eq('targetMultiFacetAwareItemId', Utils.toUuid(multiFacetAwareItemId))
    }

    static DetachedCriteria<SemanticLink> byMultiFacetAwareItemIdInList(List<UUID> multiFacetAwareItemIds) {
        by().inList('multiFacetAwareItemId', multiFacetAwareItemIds)
    }

    static DetachedCriteria<SemanticLink> byAnyMultiFacetAwareItemId(Serializable multiFacetAwareItemId) {
        by().or {
            eq 'multiFacetAwareItemId', Utils.toUuid(multiFacetAwareItemId)
            eq 'targetMultiFacetAwareItemId', Utils.toUuid(multiFacetAwareItemId)
        }
    }

    static DetachedCriteria<SemanticLink> byAnyMultiFacetAwareItemIdInList(List<UUID> multiFacetAwareItemIds) {
        by().or {
            inList 'multiFacetAwareItemId', multiFacetAwareItemIds
            inList 'targetMultiFacetAwareItemId', multiFacetAwareItemIds
        }
    }

    static DetachedCriteria<SemanticLink> bySourceMultiFacetAwareItemAndTargetMultiFacetAwareItemAndLinkType(MultiFacetAware source,
                                                                                                             MultiFacetAware target,
                                                                                                             SemanticLinkType linkType) {
        by().eq('multiFacetAwareItemId', Utils.toUuid(source.id))
            .eq('multiFacetAwareItemDomainType', source.domainType)
            .eq('targetMultiFacetAwareItemId', Utils.toUuid(target.id))
            .eq('targetMultiFacetAwareItemDomainType', target.domainType)
            .eq('linkType', linkType)
    }

    static DetachedCriteria<SemanticLink> bySourceMultiFacetAwareItemIdInListAndTargetMultiFacetAwareItemIdInListAndLinkType(
        List<UUID> sourceMultiFacetAwareItemIds,
        List<UUID> targetMultiFacetAwareItemIds,
        SemanticLinkType linkType) {
        by().inList('multiFacetAwareItemId', sourceMultiFacetAwareItemIds)
            .inList('targetMultiFacetAwareItemId', targetMultiFacetAwareItemIds)
            .eq('linkType', linkType)
    }

    static DetachedCriteria<SemanticLink> withFilter(DetachedCriteria<SemanticLink> criteria, Map filters) {
        if (filters.linkType) criteria = criteria.eq('linkType', SemanticLinkType.findForLabel(filters.linkType))
        criteria
    }
}