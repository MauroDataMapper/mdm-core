/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.enumeration

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.CopyInformation
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModelService
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadataService
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceEnumerationType
import uk.ac.ox.softeng.maurodatamapper.referencedata.traits.service.ReferenceSummaryMetadataAwareService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

class ReferenceEnumerationValueService extends ModelItemService<ReferenceEnumerationValue> implements ReferenceSummaryMetadataAwareService {

    ReferenceSummaryMetadataService referenceSummaryMetadataService
    ReferenceDataModelService referenceDataModelService

    @Override
    ReferenceEnumerationValue get(Serializable id) {
        ReferenceEnumerationValue.get(id)
    }

    Long count() {
        ReferenceEnumerationValue.count()
    }

    @Override
    List<ReferenceEnumerationValue> list(Map args) {
        ReferenceEnumerationValue.list(args)
    }

    @Override
    List<ReferenceEnumerationValue> getAll(Collection<UUID> ids) {
        ReferenceEnumerationValue.getAll(ids).findAll()
    }

    @Override
    void deleteAll(Collection<ReferenceEnumerationValue> catalogueItems) {
        ReferenceEnumerationValue.deleteAll(catalogueItems)
    }

    @Override
    void delete(ReferenceEnumerationValue resource) {
        if (resource) {
            resource.delete(flush: true)
        }
    }

    @Override
    ReferenceEnumerationValue findByIdJoinClassifiers(UUID id) {
        ReferenceEnumerationValue.findById(id, [fetch: [classifiers: 'join']])
    }

    @Override
    void removeAllFromClassifier(Classifier classifier) {
        ReferenceEnumerationValue.byClassifierId(classifier.id).list().each {
            it.removeFromClassifiers(classifier)
        }
    }

    @Override
    List<ReferenceEnumerationValue> findAllByClassifier(Classifier classifier) {
        ReferenceEnumerationValue.byClassifierId(classifier.id).list()
    }

    @Override
    List<ReferenceEnumerationValue> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        findAllByClassifier(classifier).findAll {
            userSecurityPolicyManager.userCanReadSecuredResourceId(ReferenceDataModel, it.model.id)
        }
    }

    @Override
    Boolean shouldPerformSearchForTreeTypeCatalogueItems(String domainType) {
        domainType == ReferenceEnumerationValue.simpleName
    }

    @Override
    List<ReferenceEnumerationValue> findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                                               String searchTerm, String domainType) {
        List<UUID> readableIds = userSecurityPolicyManager.listReadableSecuredResourceIds(ReferenceDataModel)
        if (!readableIds) return []


        List<ReferenceEnumerationValue> results = []
        if (shouldPerformSearchForTreeTypeCatalogueItems(domainType)) {
            log.debug('Performing hs label search')
            long start = System.currentTimeMillis()
            results =
                ReferenceEnumerationValue
                    .labelHibernateSearch(ReferenceEnumerationValue, searchTerm, readableIds.toList(), referenceDataModelService.getAllReadablePaths(readableIds)).results
            log.debug("Search took: ${Utils.getTimeString(System.currentTimeMillis() - start)}. Found ${results.size()}")
        }

        results
    }

    ReferenceEnumerationValue findByIdAndReferenceEnumerationType(UUID resourceId, UUID referenceEnumerationTypeId) {
        ReferenceEnumerationValue.byIdAndReferenceEnumerationType(resourceId, referenceEnumerationTypeId).find()
    }

    List<ReferenceEnumerationValue> findAllByReferenceEnumerationType(UUID referenceEnumerationTypeId, Map pagination = [:]) {
        ReferenceEnumerationValue.byReferenceEnumerationType(referenceEnumerationTypeId).list(pagination)
    }

    @Override
    List<ReferenceEnumerationValue> findAllByMetadataNamespaceAndKey(String namespace, String key, Map pagination) {
        ReferenceEnumerationValue.byMetadataNamespaceAndKey(namespace, key).list(pagination)
    }

    @Override
    List<ReferenceEnumerationValue> findAllByMetadataNamespace(String namespace, Map pagination) {
        ReferenceEnumerationValue.byMetadataNamespace(namespace).list(pagination)
    }

    List<ReferenceEnumerationValue> findAllByReferenceDataModelId(Serializable dataModelId) {
        ReferenceEnumerationValue.byReferenceDataModelId(dataModelId).list()
    }

    @Override
    ReferenceEnumerationValue copy(Model copiedModel, ReferenceEnumerationValue original, CatalogueItem referenceEnumerationTypeToCopyInto,
                                   UserSecurityPolicyManager userSecurityPolicyManager) {
        copyReferenceEnumerationValue(copiedModel as ReferenceDataModel, original, referenceEnumerationTypeToCopyInto as ReferenceEnumerationType,
                                      userSecurityPolicyManager.user,
                                      userSecurityPolicyManager)
    }

    ReferenceEnumerationValue copyReferenceEnumerationValue(ReferenceDataModel copiedReferenceDataModel, ReferenceEnumerationValue original,
                                                            ReferenceEnumerationType referenceEnumerationTypeToCopyInto,
                                                            User copier, UserSecurityPolicyManager userSecurityPolicyManager, CopyInformation copyInformation = null) {
        ReferenceEnumerationValue copy = new ReferenceEnumerationValue(key: original.key, value: original.value, category: original.category)

        copy = copyModelItemInformation(original, copy, copier, userSecurityPolicyManager, copyInformation)
        setCatalogueItemRefinesCatalogueItem(copy, original, copier)

        ReferenceEnumerationType referenceEnumerationType = referenceEnumerationTypeToCopyInto ?:
                                                            copiedReferenceDataModel.findReferenceDataTypeByLabelAndType(original.referenceEnumerationType.label,
                                                                                                                         ReferenceEnumerationType.simpleName)
        referenceEnumerationType.addToReferenceEnumerationValues(copy)
        copy
    }
}