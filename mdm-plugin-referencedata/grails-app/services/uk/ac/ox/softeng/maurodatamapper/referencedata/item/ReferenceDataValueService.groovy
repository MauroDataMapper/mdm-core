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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item


import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadataService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

@Slf4j
@Transactional
class ReferenceDataValueService extends ModelItemService<ReferenceDataValue> {

    ReferenceSummaryMetadataService referenceSummaryMetadataService

    @Override
    ReferenceDataValue get(Serializable id) {
        ReferenceDataValue.get(id)
    }

    Long count() {
        ReferenceDataValue.count()
    }

    @Override
    List<ReferenceDataValue> list(Map args) {
        ReferenceDataValue.list(args)
    }

    @Override
    List<ReferenceDataValue> getAll(Collection<UUID> ids) {
        ReferenceDataValue.getAll(ids).findAll()
    }

    @Override
    void deleteAll(Collection<ReferenceDataValue> referenceDataValues) {
        referenceDataValues.each { delete(it) }
    }

    void delete(UUID id) {
        delete(get(id), true)
    }

    void delete(ReferenceDataValue referenceDataValue, boolean flush = false) {
        if (!referenceDataValue) return
        referenceDataValue.breadcrumbTree.removeFromParent()
        referenceDataValue.referenceDataElement = null
        referenceDataValuet.referenceDataModel?.removeFromReferenceDataValues(referenceDataValue)
        referenceDataValue.delete(flush: flush)
    }

    @Override
    boolean hasTreeTypeModelItems(ReferenceDataValue catalogueItem, boolean forDiff) {
        false
    }


    @Override
    List<ModelItem> findAllTreeTypeModelItemsIn(ReferenceDataValue catalogueItem, boolean forDiff) {
        []
    }

    @Override
    ReferenceDataValue findByIdJoinClassifiers(UUID id) {
        ReferenceDataValue.findById(id, [fetch: [classifiers: 'join']])
    }

    @Override
    void removeAllFromClassifier(Classifier classifier) {
        ReferenceDataValue.byClassifierId(classifier.id).list().each {
            it.removeFromClassifiers(classifier)
        }
    }

    @Override
    List<ReferenceDataValue> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        ReferenceDataValue.byClassifierId(classifier.id).list().findAll { userSecurityPolicyManager.userCanReadSecuredResourceId(ReferenceDataModel, it.model.id) }
    }

    @Override
    Class<ReferenceDataValue> getModelItemClass() {
        ReferenceDataValue
    }

    @Override
    Boolean shouldPerformSearchForTreeTypeCatalogueItems(String domainType) {
        domainType == ReferenceDataValue.simpleName
    }


    @Override
    List<ReferenceDataValue> findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                                          String searchTerm, String domainType) {
        /*List<UUID> readableIds = userSecurityPolicyManager.listReadableSecuredResourceIds(ReferenceDataModel)
        if (!readableIds) return []

        List<ReferenceDataElement> results = []
        if (shouldPerformSearchForTreeTypeCatalogueItems(domainType)) {
            log.debug('Performing lucene label search')
            long start = System.currentTimeMillis()
            results = ReferenceDataElement.luceneLabelSearch(ReferenceDataElement, searchTerm, readableIds.toList()).results
            log.debug("Search took: ${Utils.getTimeString(System.currentTimeMillis() - start)}. Found ${results.size()}")
        }*/

results = [] //TODO
        results
    }

    List<ReferenceDataValue> findAllByReferenceDataModelId(Serializable referenceDataModelId, Map pagination = [:]) {
        findAllByReferenceDataModelId(referenceDataModelId, pagination, pagination)
    }

    List<ReferenceDataValue> findAllByReferenceDataModelId(Serializable referenceDataModelId, Map filter, Map pagination) {
        ReferenceDataValue.withFilter(ReferenceDataValue.byReferenceDataModelId(referenceDataModelId), filter).list(pagination)
    }

    List<ReferenceDataValue> findAllByReferenceDataModelIdAndRowNumber(Serializable referenceDataModelId, Integer fromRowNumber, Integer toRowNumber, Map params) {
        ReferenceDataValue.withFilter(ReferenceDataValue.byReferenceDataModelIdAndRowNumber(referenceDataModelId, fromRowNumber, toRowNumber), params).list()
    }

    List<ReferenceDataValue> findAllByReferenceDataModelIdAndRowNumberIn(Serializable referenceDataModelId, List rowNumbers, Map params = [:]) {
        ReferenceDataValue.withFilter(ReferenceDataValue.byReferenceDataModelIdAndRowNumberIn(referenceDataModelId, rowNumbers), params).list(params)
    }    

    Integer countRowsByReferenceDataModelId(Serializable referenceDataModelId) {
        ReferenceDataValue.countByReferenceDataModelId(referenceDataModelId).list()[0]
    }

    List<Integer> findDistinctRowNumbersByReferenceDataModelIdAndValueIlike(Serializable referenceDataModelId, String valueSearch) {
        ReferenceDataValue.distinctRowNumbersByReferenceDataModelIdAndValueIlike(referenceDataModelId, valueSearch).list()
    }

    List<ReferenceDataValue> findAllByReferenceDataModelIdAndValueIlike(Serializable referenceDataModelId, String valueSearch, Map pagination = [:]) {
        ReferenceDataValue.byReferenceDataModelIdAndValueIlike(referenceDataModelId, valueSearch).list(pagination)
    }

    void checkImportedReferenceDataValueAssociations(User importingUser, ReferenceDataModel referenceDataModel, ReferenceDataValue referenceDataValue) {
        referenceDataModel.addToReferenceDataValues(referenceDataValue)
        referenceDataValue.createdBy = importingUser.emailAddress

        //Get the reference data element for this value by getting the matching reference data element for the model
        referenceDataValue.referenceDataElement = referenceDataModel.referenceDataElements.find {it.label == referenceDataValue.referenceDataElement.label}

        checkFacetsAfterImportingCatalogueItem(referenceDataValue)
    }    
}