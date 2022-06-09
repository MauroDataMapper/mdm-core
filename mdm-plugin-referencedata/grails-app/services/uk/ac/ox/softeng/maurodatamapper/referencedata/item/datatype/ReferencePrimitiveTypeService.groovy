/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModelService
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadataService
import uk.ac.ox.softeng.maurodatamapper.referencedata.traits.service.ReferenceSummaryMetadataAwareService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.grails.datastore.mapping.model.PersistentEntity

@Slf4j
@Transactional
class ReferencePrimitiveTypeService extends ModelItemService<ReferencePrimitiveType> implements ReferenceSummaryMetadataAwareService {

    public static final String DEFAULT_TEXT_TYPE_LABEL = 'Text'
    public static final String DEFAULT_TEXT_TYPE_DESCRIPTION = 'Text Data Type'

    ReferenceDataTypeService referenceDataTypeService
    ReferenceSummaryMetadataService referenceSummaryMetadataService
    ReferenceDataModelService referenceDataModelService

    @Override
    ReferencePrimitiveType get(Serializable id) {
        ReferencePrimitiveType.get(id)
    }

    @Override
    boolean handlesPathPrefix(String pathPrefix) {
        // Have to override to ensure we type DataTypeService
        false
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
    List<ReferencePrimitiveType> findAllByClassifier(Classifier classifier) {
        ReferencePrimitiveType.byClassifierId(ReferencePrimitiveType, classifier.id).list()
    }

    @Override
    List<ReferencePrimitiveType> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        findAllByClassifier(classifier).findAll {
            userSecurityPolicyManager.userCanReadSecuredResourceId(ReferenceDataModel, it.model.id)
        }
    }

    @Override
    Boolean shouldPerformSearchForTreeTypeCatalogueItems(String domainType) {
        domainType == ReferencePrimitiveType.simpleName
    }

    @Override
    List<ReferencePrimitiveType> findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                                            String searchTerm, String domainType) {
        List<UUID> readableIds = userSecurityPolicyManager.listReadableSecuredResourceIds(ReferenceDataModel)
        if (!readableIds) return []

        log.debug('Performing hs label search')
        long start = System.currentTimeMillis()
        List<ReferencePrimitiveType> results = []
        if (shouldPerformSearchForTreeTypeCatalogueItems(domainType)) {
            results =
                ReferencePrimitiveType
                    .labelHibernateSearch(ReferencePrimitiveType, searchTerm, readableIds.toList(), referenceDataModelService.getAllReadablePaths(readableIds)).results
        }
        log.debug("Search took: ${Utils.getTimeString(System.currentTimeMillis() - start)}. Found ${results.size()}")
        results
    }

    @Override
    ReferencePrimitiveType copy(Model copiedReferenceDataModel, ReferencePrimitiveType original, CatalogueItem nonModelParent, UserSecurityPolicyManager userSecurityPolicyManager) {
        ReferencePrimitiveType existing = copiedReferenceDataModel.findReferenceDataTypeByLabel(original.label)

        // If it doesn't already exist then copy the ReferencePrimitiveType
        existing ?: referenceDataTypeService.copy(copiedReferenceDataModel, original, nonModelParent, userSecurityPolicyManager)
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
        ReferencePrimitiveType primitiveType = referenceDataModel.findReferenceDataTypeByLabelAndType(cleanLabel, ReferenceDataType
            .PRIMITIVE_DOMAIN_TYPE) as ReferencePrimitiveType
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
