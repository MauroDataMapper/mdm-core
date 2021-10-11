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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration.EnumerationValue
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration.EnumerationValueService
import uk.ac.ox.softeng.maurodatamapper.datamodel.traits.service.SummaryMetadataAwareService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.grails.datastore.mapping.model.PersistentEntity

@Slf4j
@Transactional
class EnumerationTypeService extends ModelItemService<EnumerationType> implements SummaryMetadataAwareService {

    EnumerationValueService enumerationValueService
    SummaryMetadataService summaryMetadataService
    DataTypeService dataTypeService

    @Override
    boolean handlesPathPrefix(String pathPrefix) {
        // Have to override to ensure we type DataTypeService
        false
    }

    @Override
    EnumerationType get(Serializable id) {
        EnumerationType.get(id)
    }

    Long count() {
        EnumerationType.count()
    }

    @Override
    List<EnumerationType> list(Map args) {
        EnumerationType.list(args)
    }

    @Override
    List<EnumerationType> getAll(Collection<UUID> ids) {
        EnumerationType.getAll(ids).findAll()
    }

    @Override
    void deleteAll(Collection<EnumerationType> catalogueItems) {
        EnumerationType.deleteAll(catalogueItems)
    }

    @Override
    void delete(EnumerationType enumerationType) {
        delete(enumerationType, true)
    }

    void delete(EnumerationType enumerationType, boolean flush) {
        enumerationType.delete(flush: flush)
    }

    @Override
    EnumerationType copy(Model copiedDataModel, EnumerationType original, CatalogueItem nonModelParent,
                         UserSecurityPolicyManager userSecurityPolicyManager) {
        dataTypeService.copy(copiedDataModel, original, nonModelParent, userSecurityPolicyManager) as EnumerationType
    }

    @Override
    boolean hasTreeTypeModelItems(EnumerationType catalogueItem, boolean fullTreeRender, boolean includeImportedItems) {
        fullTreeRender && catalogueItem.enumerationValues
    }

    @Override
    List<ModelItem> findAllTreeTypeModelItemsIn(EnumerationType catalogueItem, boolean fullTreeRender, boolean includeImportedItems) {
        fullTreeRender ? catalogueItem.enumerationValues.toList() : [] as List<ModelItem>
    }

    @Override
    EnumerationType findByIdJoinClassifiers(UUID id) {
        EnumerationType.findById(id, [fetch: [classifiers: 'join']])
    }

    @Override
    void removeAllFromClassifier(Classifier classifier) {
        EnumerationType.byClassifierId(EnumerationType, classifier.id).list().each {
            it.removeFromClassifiers(classifier)
        }
    }

    @Override
    List<EnumerationType> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        EnumerationType.byClassifierId(EnumerationType, classifier.id).list().findAll {
            userSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, it.model.id)
        }
    }

    @Override
    Class<EnumerationType> getModelItemClass() {
        EnumerationType
    }

    @Override
    Boolean shouldPerformSearchForTreeTypeCatalogueItems(String domainType) {
        domainType == EnumerationType.simpleName
    }

    @Override
    List<EnumerationType> findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                                     String searchTerm, String domainType) {
        List<UUID> readableIds = userSecurityPolicyManager.listReadableSecuredResourceIds(DataModel)
        if (!readableIds) return []

        List<EnumerationType> results = []
        if (shouldPerformSearchForTreeTypeCatalogueItems(domainType)) {
            log.debug('Performing lucene label search')
            long start = System.currentTimeMillis()
            results = EnumerationType.luceneLabelSearch(EnumerationType, searchTerm, readableIds.toList()).results
            log.debug("Search took: ${Utils.getTimeString(System.currentTimeMillis() - start)}. Found ${results.size()}")
        }

        results
    }

    @Override
    EnumerationType updateFacetsAfterInsertingCatalogueItem(EnumerationType enumerationType) {
        super.updateFacetsAfterInsertingCatalogueItem(enumerationType) as EnumerationType
        enumerationType.enumerationValues.each {
            enumerationValueService.updateFacetsAfterInsertingCatalogueItem(it)
        }
        enumerationType
    }

    @Override
    EnumerationType checkFacetsAfterImportingCatalogueItem(EnumerationType enumerationType) {
        enumerationType = super.checkFacetsAfterImportingCatalogueItem(enumerationType) as EnumerationType
        enumerationType.enumerationValues.each {
            enumerationValueService.checkFacetsAfterImportingCatalogueItem(it)
        }
        enumerationType
    }

    private EnumerationType addEnumerationValueToEnumerationType(EnumerationType enumerationType, String key, String value, User createdBy) {
        if (key)
            enumerationType.addToEnumerationValues(key: key, value: value ?: key, createdBy: createdBy.emailAddress)
        enumerationType
    }

    private EnumerationType addEnumerationValueToEnumerationType(EnumerationType enumerationType, String key, String value, String category,
                                                                 User createdBy) {
        if (key)
            enumerationType.addToEnumerationValues(key: key, value: value ?: key, category: category, createdBy: createdBy.emailAddress)
        enumerationType
    }

    private EnumerationType createDataType(String label, String description, User createdBy) {
        new EnumerationType(label: label, description: description, createdBy: createdBy.emailAddress)
    }

    EnumerationType findOrCreateDataTypeForDataModel(DataModel dataModel, String label, String description, User createdBy) {
        String cleanLabel = label.trim()
        EnumerationType enumerationType = dataModel.findDataTypeByLabelAndType(cleanLabel, DataType.ENUMERATION_DOMAIN_TYPE) as EnumerationType
        if (!enumerationType) {
            enumerationType = createDataType(cleanLabel, description, createdBy)
            dataModel.addToDataTypes(enumerationType)
        }
        enumerationType
    }

    @Override
    PersistentEntity getPersistentEntity() {
        grailsApplication.mappingContext.getPersistentEntity(DataType.name)
    }

    @Override
    List<EnumerationType> findAllByMetadataNamespaceAndKey(String namespace, String key, Map pagination) {
        EnumerationType.byMetadataNamespaceAndKey(namespace, key).list(pagination)
    }

    @Override
    List<EnumerationType> findAllByMetadataNamespace(String namespace, Map pagination) {
        EnumerationType.byMetadataNamespace(namespace).list(pagination)
    }

    @Override
    void propagateContentsInformation(EnumerationType catalogueItem, EnumerationType previousVersionCatalogueItem) {
        previousVersionCatalogueItem.enumerationValues.each {previousEnumerationValue ->
            EnumerationValue enumerationValue = catalogueItem.enumerationValues.find {it.label == previousEnumerationValue.label}
            if (enumerationValue) {
                enumerationValueService.propagateDataFromPreviousVersion(enumerationValue, previousEnumerationValue)
            }
        }
    }
}
