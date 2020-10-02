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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.enumeration

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

class ReferenceEnumerationValueService extends ModelItemService<ReferenceEnumerationValue> {

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
    boolean hasTreeTypeModelItems(ReferenceEnumerationValue catalogueItem) {
        false
    }

    @Override
    List<ModelItem> findAllTreeTypeModelItemsIn(ReferenceEnumerationValue catalogueItem) {
        []
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
    List<ReferenceEnumerationValue> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        ReferenceEnumerationValue.byClassifierId(classifier.id).list().findAll {
            userSecurityPolicyManager.userCanReadSecuredResourceId(ReferenceDataModel, it.model.id)
        }
    }

    @Override
    Class<ReferenceEnumerationValue> getModelItemClass() {
        ReferenceEnumerationValue
    }

    @Override
    ReferenceEnumerationValue updateIndexForModelItemInParent(ReferenceEnumerationValue enumerationValue, CatalogueItem parent, int newIndex) {
        enumerationValue.index = newIndex
        if (parent.instanceOf(EnumerationType)) {
            parent.updateEnumerationValueIndexes(enumerationValue)
        } else throw new ApiInternalException('EVS01', 'Non-EnumerationType passed as parent to enumeration value')
        enumerationValue
    }

    @Override
    Boolean shouldPerformSearchForTreeTypeCatalogueItems(String domainType) {
        domainType == ReferenceEnumerationValue.simpleName
    }

    @Override
    List<ReferenceEnumerationValue> findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                                               String searchTerm, String domainType) {
        List<UUID> readableIds = userSecurityPolicyManager.listReadableSecuredResourceIds(DataModel)
        if (!readableIds) return []


        List<ReferenceEnumerationValue> results = []
        if (shouldPerformSearchForTreeTypeCatalogueItems(domainType)) {
            log.debug('Performing lucene label search')
            long start = System.currentTimeMillis()
            results = ReferenceEnumerationValue.luceneLabelSearch(ReferenceEnumerationValue, searchTerm, readableIds.toList()).results
            log.debug("Search took: ${Utils.getTimeString(System.currentTimeMillis() - start)}. Found ${results.size()}")
        }

        results
    }

    ReferenceEnumerationValue findByIdAndEnumerationType(UUID resourceId, UUID enumerationTypeId) {
        ReferenceEnumerationValue.byIdAndEnumerationType(resourceId, enumerationTypeId).find()
    }

    private List<ReferenceEnumerationValue> findAllByEnumerationType(UUID enumerationTypeId, Map pagination = [:]) {
        ReferenceEnumerationValue.byEnumerationType(enumerationTypeId).list(pagination)
    }
}