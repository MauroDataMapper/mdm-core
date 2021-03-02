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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype


import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.grails.datastore.mapping.model.PersistentEntity

@Slf4j
@Transactional
class ReferencePrimitiveTypeService extends ModelItemService<ReferencePrimitiveType> {

    public static final String DEFAULT_TEXT_TYPE_LABEL = 'Text'
    public static final String DEFAULT_TEXT_TYPE_DESCRIPTION = 'Text Data Type'

    @Override
    ReferencePrimitiveType get(Serializable id) {
        ReferencePrimitiveType.get(id)
    }

    Long count() {
        ReferencePrimitiveType.count()
    }

    @Override
    List<ReferencePrimitiveType> list(Map args) {
        ReferencePrimitiveType.list(args)
    }

    @Override
    List<ReferencePrimitiveType> getAll(Collection<UUID> ids) {
        ReferencePrimitiveType.getAll(ids).findAll()
    }

    @Override
    void deleteAll(Collection<ReferencePrimitiveType> catalogueItems) {
        ReferencePrimitiveType.deleteAll(catalogueItems)
    }

    @Override
    void delete(ReferencePrimitiveType primitiveType) {
        delete(primitiveType, true)
    }

    void delete(ReferencePrimitiveType primitiveType, boolean flush) {
        primitiveType.delete(flush: flush)
    }

    void removeReferenceSummaryMetadataFromCatalogueItem(UUID catalogueItemId, ReferenceSummaryMetadata summaryMetadata) {
        removeFacetFromDomain(catalogueItemId, summaryMetadata.id, 'referenceSummaryMetadata')
    }

    @Override
    boolean hasTreeTypeModelItems(ReferencePrimitiveType catalogueItem, boolean forDiff, boolean includeImported = false) {
        false
    }

    @Override
    List<ModelItem> findAllTreeTypeModelItemsIn(ReferencePrimitiveType catalogueItem, boolean forDiff, boolean includeImported = false) {
        []
    }

    @Override
    ReferencePrimitiveType findByIdJoinClassifiers(UUID id) {
        ReferencePrimitiveType.findById(id, [fetch: [classifiers: 'join']])
    }

    @Override
    void removeAllFromClassifier(Classifier classifier) {
        ReferencePrimitiveType.byClassifierId(ReferencePrimitiveType, classifier.id).list().each {
            it.removeFromClassifiers(classifier)
        }
    }

    @Override
    List<ReferencePrimitiveType> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        ReferencePrimitiveType.byClassifierId(ReferencePrimitiveType, classifier.id).list().findAll {
            userSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, it.model.id)
        }
    }

    @Override
    Class<ReferencePrimitiveType> getModelItemClass() {
        ReferencePrimitiveType
    }

    @Override
    Boolean shouldPerformSearchForTreeTypeCatalogueItems(String domainType) {
        domainType == ReferencePrimitiveType.simpleName
    }

    @Override
    List<ReferencePrimitiveType> findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                                            String searchTerm, String domainType) {
        List<UUID> readableIds = userSecurityPolicyManager.listReadableSecuredResourceIds(DataModel)
        if (!readableIds) return []

        log.debug('Performing lucene label search')
        long start = System.currentTimeMillis()
        List<ReferencePrimitiveType> results = []
        if (shouldPerformSearchForTreeTypeCatalogueItems(domainType)) {
            results = ReferencePrimitiveType.luceneLabelSearch(ReferencePrimitiveType, searchTerm, readableIds.toList()).results
        }
        log.debug("Search took: ${Utils.getTimeString(System.currentTimeMillis() - start)}. Found ${results.size()}")
        results
    }

    @Override
    PersistentEntity getPersistentEntity() {
        grailsApplication.mappingContext.getPersistentEntity(ReferenceDataType.name)
    }

    ReferencePrimitiveType createDataType(String label, String description, User createdBy, String units = null) {
        createDataType(label, description, createdBy.emailAddress, units)
    }

    ReferencePrimitiveType createDataType(String label, String description, String createdByEmailAddress, String units = null) {
        new ReferencePrimitiveType(label: label, description: description, createdBy: createdByEmailAddress, units: units)
    }

    ReferencePrimitiveType findOrCreateDataTypeForDataModel(ReferenceDataModel referenceDataModel, String label, String description, User createdBy,
                                                            String units = null) {
        findOrCreateDataTypeForDataModel(referenceDataModel, label, description, createdBy.emailAddress, units)
    }

    ReferencePrimitiveType findOrCreateDataTypeForDataModel(ReferenceDataModel referenceDataModel, String label, String description, String createdByEmailAddress,
                                                            String units = null) {
        String cleanLabel = label.trim()
        ReferencePrimitiveType primitiveType = dataModel.findDataTypeByLabelAndType(cleanLabel, ReferenceDataType.PRIMITIVE_DOMAIN_TYPE) as ReferencePrimitiveType
        if (!primitiveType) {
            primitiveType = createDataType(cleanLabel, description, createdByEmailAddress, units)
            referenceDataModel.addToReferenceDataTypes(primitiveType)
        }
        primitiveType
    }

    @Override
    List<ReferencePrimitiveType> findAllByMetadataNamespaceAndKey(String namespace, String key, Map pagination) {
        ReferencePrimitiveType.byMetadataNamespaceAndKey(namespace, key).list(pagination)
    }

    @Override
    List<ReferencePrimitiveType> findAllByMetadataNamespace(String namespace, Map pagination) {
        ReferencePrimitiveType.byMetadataNamespace(namespace).list(pagination)
    }
}
