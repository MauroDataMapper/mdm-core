/*
 * Copyright 2020-2024 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.CopyInformation
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.datamodel.traits.service.SummaryMetadataAwareService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import groovy.util.logging.Slf4j

@Slf4j
class EnumerationValueService extends ModelItemService<EnumerationValue> implements SummaryMetadataAwareService {

    SummaryMetadataService summaryMetadataService
    DataModelService dataModelService

    @Override
    EnumerationValue get(Serializable id) {
        EnumerationValue.get(id)
    }

    Long count() {
        EnumerationValue.count()
    }

    @Override
    List<EnumerationValue> list(Map args) {
        EnumerationValue.list(args)
    }

    @Override
    List<EnumerationValue> getAll(Collection<UUID> ids) {
        EnumerationValue.getAll(ids).findAll()
    }

    @Override
    void deleteAll(Collection<EnumerationValue> catalogueItems) {
        EnumerationValue.deleteAll(catalogueItems)
    }

    @Override
    void delete(EnumerationValue resource) {
        if (resource) {
            resource.delete(flush: true)
        }
    }

    @Override
    EnumerationValue findByParentIdAndLabel(UUID parentId, String label) {
        EnumerationValue.byEnumerationType(parentId).eq('label', label).get()
    }

    @Override
    void deleteAllByModelIds(Set<UUID> dataModelIds) {
        //Assume DataElements gone by this point
        List<UUID> enumerationValueIds = EnumerationValue.by().where {
            enumerationType {
                inList('dataModel.id', dataModelIds)
            }
        }.id().list() as List<UUID>

        if (enumerationValueIds) {

            log.trace('Removing facets for {} Enumeration Values', enumerationValueIds.size())
            deleteAllFacetsByMultiFacetAwareIds(enumerationValueIds,
                                                'delete from datamodel.join_enumerationvalue_to_facet where enumerationvalue_id in :ids')

            log.trace('Removing {} EnumerationValues', enumerationValueIds.size())

            Utils.executeInBatches(enumerationValueIds, {ids ->
                sessionFactory.currentSession
                        .createSQLQuery('DELETE FROM datamodel.enumeration_value WHERE id IN :ids')
                        .setParameter('ids', ids)
                        .executeUpdate()
            })
            log.trace('EnumerationValues removed')
        }
    }

    @Override
    void deleteAllFacetDataByMultiFacetAwareIds(List<UUID> catalogueItemIds) {
        super.deleteAllFacetDataByMultiFacetAwareIds(catalogueItemIds)
        summaryMetadataService.deleteAllByMultiFacetAwareItemIds(catalogueItemIds)
    }

    @Override
    EnumerationValue findByIdJoinClassifiers(UUID id) {
        EnumerationValue.findById(id, [fetch: [classifiers: 'join']])
    }

    @Override
    void removeAllFromClassifier(Classifier classifier) {
        EnumerationValue.byClassifierId(classifier.id).list().each {
            it.removeFromClassifiers(classifier)
        }
    }

    @Override
    List<EnumerationValue> findAllByClassifier(Classifier classifier) {
        EnumerationValue.byClassifierId(classifier.id).list()
    }

    @Override
    List<EnumerationValue> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        findAllByClassifier(classifier).findAll {
            userSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, it.model.id)
        }
    }

    @Override
    Boolean shouldPerformSearchForTreeTypeCatalogueItems(String domainType) {
        domainType == EnumerationValue.simpleName
    }

    @Override
    List<EnumerationValue> findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                                      String searchTerm, String domainType) {
        List<UUID> readableIds = userSecurityPolicyManager.listReadableSecuredResourceIds(DataModel)
        if (!readableIds) return []

        List<EnumerationValue> results = []
        if (shouldPerformSearchForTreeTypeCatalogueItems(domainType)) {
            log.debug('Performing hs label search')
            long start = System.currentTimeMillis()
            results =
                EnumerationValue
                    .labelHibernateSearch(EnumerationValue, searchTerm, readableIds.toList(), dataModelService.getAllReadablePaths(readableIds))
                    .results
            log.debug("Search took: ${Utils.getTimeString(System.currentTimeMillis() - start)}. Found ${results.size()}")
        }

        results
    }

    EnumerationValue findByIdAndEnumerationType(UUID resourceId, UUID enumerationTypeId) {
        EnumerationValue.byIdAndEnumerationType(resourceId, enumerationTypeId).find()
    }

    List<EnumerationValue> findAllByEnumerationType(UUID enumerationTypeId, Map pagination = [:]) {
        EnumerationValue.byEnumerationType(enumerationTypeId).list(pagination)
    }

    @Override
    List<EnumerationValue> findAllByMetadataNamespaceAndKey(String namespace, String key, Map pagination) {
        EnumerationValue.byMetadataNamespaceAndKey(namespace, key).list(pagination)
    }

    @Override
    List<EnumerationValue> findAllByMetadataNamespace(String namespace, Map pagination) {
        EnumerationValue.byMetadataNamespace(namespace).list(pagination)
    }

    @Override
    EnumerationValue copy(Model copiedDataModel, EnumerationValue original, CatalogueItem enumerationTypeToCopyInto,
                          UserSecurityPolicyManager userSecurityPolicyManager) {
        copyEnumerationValue(copiedDataModel as DataModel, original, enumerationTypeToCopyInto as EnumerationType, userSecurityPolicyManager.user,
                             userSecurityPolicyManager)
    }

    EnumerationValue copyEnumerationValue(DataModel copiedDataModel, EnumerationValue original, EnumerationType enumerationTypeToCopyInto,
                                          User copier, UserSecurityPolicyManager userSecurityPolicyManager, CopyInformation copyInformation = null) {
        EnumerationValue copy = new EnumerationValue(key: original.key, value: original.value, category: original.category)

        copy = copyModelItemInformation(original, copy, copier, userSecurityPolicyManager, copyInformation)
        setCatalogueItemRefinesCatalogueItem(copy, original, copier)

        EnumerationType enumerationType = enumerationTypeToCopyInto ?: copiedDataModel.findEnumerationTypeByLabel(original.enumerationType.label)
        enumerationType.addToEnumerationValues(copy)
        copy
    }

    List<EnumerationValue> findAllByDataModelId(Serializable dataModelId) {
        EnumerationValue.byDataModelId(dataModelId).list()
    }
}