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
package uk.ac.ox.softeng.maurodatamapper.referencedata

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.authority.AuthorityService
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.provider.dataloader.DataLoaderProviderService
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadataService
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElement
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElementService
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataValue
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataValueService
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataTypeService
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceEnumerationType
import uk.ac.ox.softeng.maurodatamapper.referencedata.provider.DefaultReferenceDataTypeProvider
import uk.ac.ox.softeng.maurodatamapper.referencedata.similarity.ReferenceDataElementSimilarityResult
import uk.ac.ox.softeng.maurodatamapper.security.SecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils
import uk.ac.ox.softeng.maurodatamapper.util.Version

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

import java.time.OffsetDateTime

@Slf4j
@Transactional
@SuppressWarnings('unused')
class ReferenceDataModelService extends ModelService<ReferenceDataModel> {

    ReferenceDataElementService referenceDataElementService
    ReferenceDataTypeService referenceDataTypeService
    ReferenceDataValueService referenceDataValueService
    AuthorityService authorityService
    ReferenceSummaryMetadataService referenceSummaryMetadataService

    @Autowired
    Set<DefaultReferenceDataTypeProvider> defaultReferenceDataTypeProviders

    @Autowired(required = false)
    SecurityPolicyManagerService securityPolicyManagerService

    @Override
    ReferenceDataModel get(Serializable id) {
        ReferenceDataModel.get(id)
    }

    @Override
    List<ReferenceDataModel> getAll(Collection<UUID> ids) {
        ReferenceDataModel.getAll(ids).findAll()
    }

    @Override
    List<ReferenceDataModel> list(Map pagination = [:]) {
        ReferenceDataModel.list(pagination)
    }

    Long count() {
        ReferenceDataModel.count()
    }

    int countByLabel(String label) {
        ReferenceDataModel.countByLabel(label)
    }

    ReferenceDataModel validate(ReferenceDataModel referenceDataModel) {
        referenceDataModel.validate()
        referenceDataModel
    }

    @Override
    void deleteAll(Collection<ReferenceDataModel> catalogueItems) {
        deleteAll(catalogueItems.id, true)
    }

    void delete(UUID id) {
        delete(get(id))
    }

    @Override
    void delete(ReferenceDataModel dm) {
        dm?.deleted = true
    }

    @Override
    void delete(ReferenceDataModel rdm, boolean permanent, boolean flush = true) {
        if (!rdm) return
        if (permanent) {
            rdm.folder = null
            if (securityPolicyManagerService) {
                securityPolicyManagerService.removeSecurityForSecurableResource(rdm, null)
            }
            rdm.delete(flush: flush)
        } else delete(rdm)
    }

    List<ReferenceDataModel> deleteAll(List<Serializable> idsToDelete, Boolean permanent) {
        List<ReferenceDataModel> updated = []
        idsToDelete.each {
            ReferenceDataModel dm = get(it)
            delete(dm, permanent, false)
            if (!permanent) updated << dm
        }
        updated
    }

    void removeReferenceSummaryMetadataFromCatalogueItem(UUID catalogueItemId, ReferenceSummaryMetadata summaryMetadata) {
        removeFacetFromDomain(catalogueItemId, summaryMetadata.id, 'referenceSummaryMetadata')
    }

    @Override
    ReferenceDataModel save(ReferenceDataModel referenceDataModel) {
        log.debug('Saving {}({}) without batching', referenceDataModel.label, referenceDataModel.ident())
        save(failOnError: true, validate: false, flush: false, referenceDataModel)
    }

    @Override
    ReferenceDataModel updateFacetsAfterInsertingCatalogueItem(ReferenceDataModel catalogueItem) {
        super.updateFacetsAfterInsertingCatalogueItem(catalogueItem)
        if (catalogueItem.referenceSummaryMetadata) {
            catalogueItem.referenceSummaryMetadata.each {
                if (!it.isDirty('catalogueItemId')) it.trackChanges()
                it.catalogueItemId = catalogueItem.getId()
            }
            ReferenceSummaryMetadata.saveAll(catalogueItem.referenceSummaryMetadata)
        }
        catalogueItem
    }

    ReferenceDataModel saveModelWithContent(ReferenceDataModel referenceDataModel) {

        if (referenceDataModel.referenceDataTypes.any { it.id } ||
            referenceDataModel.referenceDataValues.any { it.id }) {
            throw new ApiInternalException('DMSXX', 'Cannot use saveWithBatching method to save ReferenceDataModel',
                                           new IllegalStateException('ReferenceDataModel has previously saved content'))
        }

        log.debug('Saving {} complete referenceDataModel', referenceDataModel.label)
        Collection<ReferenceDataType> referenceDataTypes = []
        Collection<ReferenceDataElement> referenceDataElements = []
        Collection<ReferenceDataValue> referenceDataValues = []

        if (referenceDataModel.classifiers) {
            log.trace('Saving {} classifiers')
            classifierService.saveAll(referenceDataModel.classifiers)
        }

        if (referenceDataModel.referenceDataTypes) {
            referenceDataTypes.addAll referenceDataModel.referenceDataTypes
            referenceDataModel.referenceDataTypes.clear()
        }

        if (referenceDataModel.referenceDataElements) {
            referenceDataElements.addAll referenceDataModel.referenceDataElements
            referenceDataModel.referenceDataElements.clear()
        }

        if (referenceDataModel.breadcrumbTree.children) {
            referenceDataModel.breadcrumbTree.children.each { it.skipValidation(true) }
        }

        if (referenceDataModel.referenceDataValues) {
            referenceDataValues.addAll referenceDataModel.referenceDataValues
            referenceDataModel.referenceDataValues.clear()
        }        

        save(referenceDataModel)
        sessionFactory.currentSession.flush()

        saveContent(referenceDataTypes, referenceDataElements, referenceDataValues)

        get(referenceDataModel.id)
    }

    ReferenceDataModel saveModelNewContentOnly(ReferenceDataModel referenceDataModel) {
        log.debug('Saving {} using batching', referenceDataModel.label)
        long start = System.currentTimeMillis()
        Collection<ReferenceDataType> referenceDataTypes = []
        Collection<ReferenceDataElement> referenceDataElements = []
        Collection<ReferenceDataValue> referenceDataValues = []

        if (referenceDataModel.classifiers) {
            log.trace('Saving {} classifiers')
            classifierService.saveAll(referenceDataModel.classifiers)
        }

        if (referenceDataModel.referenceDataTypes) {
            referenceDataTypes.addAll referenceDataModel.referenceDataTypes.findAll {!it.id}
        }

        if (referenceDataModel.referenceDataElements) {
            referenceDataElements.addAll referenceDataModel.referenceDataElements.findAll {!it.id}
        }

        if (referenceDataModel.referenceDataValues) {
            referenceDataValues.addAll referenceDataModel.referenceDataValues.findAll {!it.id}
        }         

        saveContent(referenceDataTypes, referenceDataElements, referenceDataValues)
        log.debug('saveModelNewContentOnly took {}', Utils.timeTaken(start))

        get(referenceDataModel.id)
    }

    void saveContent(Collection<ReferenceDataType> referenceDataTypes, Collection<ReferenceDataElement> referenceDataElements, Collection<ReferenceDataValue> referenceDataValues) {
        sessionFactory.currentSession.clear()

        //Skip validation on all contents
        long start = System.currentTimeMillis()
        log.debug('Skip validation')
        referenceDataTypes.each { it.skipValidation(true) }
        referenceDataElements.each { it.skipValidation(true) }
        referenceDataValues.each { it.skipValidation(true) }
        log.debug('skip validation took {}', Utils.timeTaken(start))

        log.trace('Saving {} referenceDataTypes', referenceDataTypes.size())
        start = System.currentTimeMillis()
        referenceDataTypeService.saveAll(referenceDataTypes)
        log.debug('referenceDataTypeService.saveAll took {}', Utils.timeTaken(start))

        log.trace('Saving {} referenceDataElements ', referenceDataElements.size())
        start = System.currentTimeMillis()
        referenceDataElementService.saveAll(referenceDataElements)
        log.debug('referenceDataElementService.saveAll took {}', Utils.timeTaken(start))

        start = System.currentTimeMillis()
        log.trace('Saving {} referenceDataValues ', referenceDataValues.size())
        referenceDataValueService.saveAll(referenceDataValues)
        log.debug('referenceDataValueService.saveAll took {}', Utils.timeTaken(start))
    }

    @Override
    List<ReferenceDataModel> findAllReadableModels(List<UUID> constrainedIds, boolean includeDeleted) {
        ReferenceDataModel.withReadable(ReferenceDataModel.by(), constrainedIds, includeDeleted).list()
    }

    @Override
    List<ReferenceDataModel> findAllReadableByEveryone() {
        ReferenceDataModel.findAllByReadableByEveryone(true)
    }

    @Override
    List<ReferenceDataModel> findAllReadableByAuthenticatedUsers() {
        ReferenceDataModel.findAllByReadableByAuthenticatedUsers(true)
    }

    List<ReferenceDataModel> findAllByLabel(String label) {
        ReferenceDataModel.findAllByLabel(label)
    }

    @Override
    List<UUID> findAllModelIds() {
        ReferenceDataModel.by().id().list() as List<UUID>
    }

    List<ReferenceDataModel> findAllByFolderId(UUID folderId) {
        ReferenceDataModel.byFolderId(folderId).list()
    }

    List<UUID> findAllIdsByFolderId(UUID folderId) {
        ReferenceDataModel.byFolderId(folderId).id().list() as List<UUID>
    }

    List<ReferenceDataModel> findAllDeleted(Map pagination = [:]) {
        ReferenceDataModel.byDeleted().list(pagination)
    }

    int countAllByLabelAndBranchNameAndNotFinalised(String label, String branchName) {
        ReferenceDataModel.countByLabelAndBranchNameAndFinalised(label, branchName, false)
    }

    ReferenceDataModel findLatestByDataLoaderPlugin(DataLoaderProviderService dataLoaderProviderService) {
        // If we get all the models with the DL metadata then sort by documentation version we should end up with the latest doc version.
        // We should do this rather than using the value of the metadata as its possible someone creates a new documentation version of the model
        // and we should use that one. All DL loaded models have the doc version set to the DL version.
        ReferenceDataModel latest = ReferenceDataModel
            .byMetadataNamespaceAndKey(dataLoaderProviderService.namespace, dataLoaderProviderService.name)
            .get([order: 'desc', sort: 'documentationVersion'])
        log.debug('Found ReferenceDataModel {}({}) which matches DataLoaderPlugin {}({})', latest.label, latest.documentationVersion,
                  dataLoaderProviderService.name, dataLoaderProviderService.version)
        latest
    }

    ReferenceDataModel checkForAndAddDefaultReferenceDataTypes(ReferenceDataModel resource, String defaultReferenceDataTypeProvider) {
        if (defaultReferenceDataTypeProvider) {
            DefaultReferenceDataTypeProvider provider = defaultReferenceDataTypeProviders.find {
                it.displayName == defaultReferenceDataTypeProvider
            }
            if (provider) {
                log.debug("Adding ${provider.displayName} default DataTypes")
                return referenceDataTypeService.addDefaultListOfReferenceDataTypesToReferenceDataModel(resource,
                                                                                                       provider.defaultListOfReferenceDataTypes)
            }
        }
        resource
    }

    void deleteAllUnusedDataTypes(ReferenceDataModel referenceDataModel) {
        log.debug('Cleaning ReferenceDataModel {} of DataTypes', referenceDataModel.label)
        referenceDataModel.referenceDataTypes.findAll {!it.referenceDataElements}.each {
            referenceDataTypeService.delete(it)
        }
    }


    /**
     * Set the relationships between Reference Data Type, Reference Data Element and Reference Data Value when importing.
     *
     * A Reference Data Value maps to one Reference Data Element, which maps to one Reference Data Type. This means an export looks
     * something like this:
     * model
     *  - types
     *    - type 1
     *    - type 2
     *  - elements
     *    - element 1
     *      - type 2
     *    - element 2
     *      - type 1
     *  - values
     *    - value 1
     *      - element 1
     *         - type 2
     *      etc....
     *
     * In other words, Reference Data Element and Reference Data Type are stated at the model level, and then restated for every
     * Reference Data Value. When importing the Reference Data Values, we want to set an assocation for Reference Data Element (and 
     * thus also Reference Data Type) to the entity imported at the model level, rather than creating duplicated entities by creating
     * a new Reference Data Element and Reference Data Type for every value. This is done in the call to checkImportedReferenceDataValueAssociations.
     */
    void checkImportedReferenceDataModelAssociations(User importingUser, ReferenceDataModel referenceDataModel, Map bindingMap = [:]) {
        referenceDataModel.createdBy = importingUser.emailAddress
        referenceDataModel.authority = authorityService.getDefaultAuthority()
        checkFacetsAfterImportingCatalogueItem(referenceDataModel)

        if (referenceDataModel.referenceDataTypes) {
            referenceDataModel.referenceDataTypes.each {rdt ->
                referenceDataTypeService.checkImportedReferenceDataTypeAssociations(importingUser, referenceDataModel, rdt)
            }
        }

        if (referenceDataModel.referenceDataElements) {
            referenceDataModel.referenceDataElements.each {rde ->
                referenceDataElementService.checkImportedReferenceDataElementAssociations(importingUser, referenceDataModel, rde)
            }
        }

        if (referenceDataModel.referenceDataValues) {
            referenceDataModel.referenceDataValues.each {rdv ->
                referenceDataValueService.checkImportedReferenceDataValueAssociations(importingUser, referenceDataModel, rdv)
            }
        }
        log.debug('ReferenceDataModel associations checked')
    }

    ReferenceDataModel ensureAllEnumerationTypesHaveValues(ReferenceDataModel referenceDataModel) {
        referenceDataModel.referenceDataTypes.
            findAll {it.instanceOf(ReferenceEnumerationType) && !(it as ReferenceEnumerationType).getReferenceEnumerationValues()}.
            each {ReferenceEnumerationType et ->
                et.addToReferenceEnumerationValues(key: '-', value: '-')
            }
        referenceDataModel
    }

    List<ReferenceDataElement> getAllDataElementsOfDataModel(ReferenceDataModel referenceDataModel) {
        referenceDataModel.referenceDataElements
    }

    List<ReferenceDataElement> findAllDataElementsWithNames(ReferenceDataModel dataModel, Set<String> dataElementNames, boolean caseInsensitive) {
        if (!dataElementNames) return []
        getAllDataElementsOfDataModel(dataModel).findAll {
            caseInsensitive ?
            it.label.toLowerCase() in dataElementNames.collect {it.toLowerCase()} :
            it.label in dataElementNames
        }
    }

    Set<ReferenceEnumerationType> findAllEnumerationTypeByNames(ReferenceDataModel referenceDataModel, Set<String> enumerationTypeNames,
                                                                boolean caseInsensitive) {
        if (!enumerationTypeNames) return []
        referenceDataModel.referenceDataTypes.findAll {it.instanceOf(ReferenceEnumerationType)}.findAll {
            caseInsensitive ?
            it.label.toLowerCase() in enumerationTypeNames.collect {it.toLowerCase()} :
            it.label in enumerationTypeNames
        } as Set<ReferenceEnumerationType>
    }

    ReferenceDataModel copyModelAsNewForkModel(ReferenceDataModel original, User copier, boolean copyPermissions, String label, boolean throwErrors,
                                               UserSecurityPolicyManager userSecurityPolicyManager) {
        copyModel(original, copier, copyPermissions, label, Version.from('1'), original.branchName, throwErrors, userSecurityPolicyManager,
                  false)
    }

    ReferenceDataModel copyModel(ReferenceDataModel original, User copier, boolean copyPermissions, String label, Version copyDocVersion,
                                 String branchName,
                                 boolean throwErrors, UserSecurityPolicyManager userSecurityPolicyManager) {
        copyModel(original, copier, copyPermissions, label, copyDocVersion, branchName, throwErrors, userSecurityPolicyManager, true)
    }

    ReferenceDataModel copyModel(ReferenceDataModel original, User copier, boolean copyPermissions, String label, Version copyDocVersion,
                                 String branchName,
                                 boolean throwErrors, UserSecurityPolicyManager userSecurityPolicyManager, boolean copySummaryMetadata) {

        Folder folder = proxyHandler.unwrapIfProxy(original.folder) as Folder
        Authority authority = proxyHandler.unwrapIfProxy(original.authority) as Authority
        ReferenceDataModel copy = new ReferenceDataModel(author: original.author, organisation: original.organisation, modelType: original.modelType,
                                                         finalised: false,
                                                         deleted: false, documentationVersion: copyDocVersion, folder: folder,
                                                         authority: authority,
                                                         branchName: branchName
        )

        copy = copyCatalogueItemInformation(original, copy, copier, userSecurityPolicyManager, copySummaryMetadata)
        copy.label = label

        if (copyPermissions) {
            if (throwErrors) {
                throw new ApiNotYetImplementedException('DMSXX', 'ReferenceDataModel permission copying')
            }
            log.warn('Permission copying is not yet implemented')

        }

        setCatalogueItemRefinesCatalogueItem(copy, original, copier)

        if (copy.validate()) {
            save(copy, validate: false)
            editService.createAndSaveEdit(copy.id, copy.domainType,
                                          "ReferenceDataModel ${original.modelType}:${original.label} created as a copy of ${original.id}",
                                          copier
            )
        } else throw new ApiInvalidModelException('DMS01', 'Copied ReferenceDataModel is invalid', copy.errors, messageSource)

        copy.trackChanges()

        if (original.referenceDataTypes) {
            // Copy all the referencedatatypes
            original.referenceDataTypes.each {dt ->
                referenceDataTypeService.copyReferenceDataType(copy, dt, copier, userSecurityPolicyManager)
            }
        }

        if (original.referenceDataElements) {
            // Copy all the referencedataelements
            original.referenceDataElements.each {de ->
                log.debug("copy element ${de}")
                referenceDataElementService.copyReferenceDataElement(copy, de, copier, userSecurityPolicyManager)
            }
        }

        copy
    }

    @Override
    ReferenceDataModel copyCatalogueItemInformation(ReferenceDataModel original,
                                                    ReferenceDataModel copy,
                                                    User copier,
                                                    UserSecurityPolicyManager userSecurityPolicyManager) {
        copyCatalogueItemInformation(original, copy, copier, userSecurityPolicyManager, false)
    }

    ReferenceDataModel copyCatalogueItemInformation(ReferenceDataModel original,
                                                    ReferenceDataModel copy,
                                                    User copier,
                                                    UserSecurityPolicyManager userSecurityPolicyManager,
                                                    boolean copySummaryMetadata) {
        copy = super.copyCatalogueItemInformation(original, copy, copier, userSecurityPolicyManager) as ReferenceDataModel
        if (copySummaryMetadata) {
            referenceSummaryMetadataService.findAllByCatalogueItemId(original.id).each {
                copy.addToReferenceSummaryMetadata(label: it.label, summaryMetadataType: it.summaryMetadataType, createdBy: copier.emailAddress)
            }
        }
        copy
    }

    List<ReferenceDataElementSimilarityResult> suggestLinksBetweenModels(ReferenceDataModel referenceDataModel,
                                                                         ReferenceDataModel otherReferenceDataModel,
                                                                         int maxResults) {
        referenceDataModel.referenceDataElements.collect {de ->
            referenceDataElementService.findAllSimilarReferenceDataElementsInReferenceDataModel(otherReferenceDataModel, de, maxResults)
        }
    }


    /*    Map<UUID, Long> obtainChildKnowledge(List<ReferenceDataModel> parents) {
            if (!parents) return [:]
            DetachedCriteria<ReferenceDataModel> criteria = new DetachedCriteria<DataClass>(DataClass)
                .isNull('parentDataClass')
                .inList('dataModel', parents)
                .projections {
                    groupProperty('dataModel.id')
                    count()
                }.order('dataModel')
            criteria.list().collectEntries { [it[0], it[1]] }
        }
    */

    @Override
    boolean hasTreeTypeModelItems(ReferenceDataModel dataModel, boolean forDiff, boolean includeImported = false) {
        false
    }

    @Override
    List<ModelItem> findAllTreeTypeModelItemsIn(ReferenceDataModel catalogueItem, boolean forDiff, boolean includeImported = false) {
        []
    }

    @Override
    Boolean shouldPerformSearchForTreeTypeCatalogueItems(String domainType) {
        !domainType || domainType == ReferenceDataModel.simpleName
    }

    @Override
    List<ReferenceDataModel> findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                                        String searchTerm, String domainType) {
        List<UUID> readableIds = userSecurityPolicyManager.listReadableSecuredResourceIds(ReferenceDataModel)
        if (!readableIds) return []

        List<ReferenceDataModel> results = []
        if (shouldPerformSearchForTreeTypeCatalogueItems(domainType)) {
            log.debug('Performing lucene label search')
            long start = System.currentTimeMillis()
            results = ReferenceDataModel.luceneLabelSearch(ReferenceDataModel, searchTerm, readableIds.toList()).results
            log.debug("Search took: ${Utils.getTimeString(System.currentTimeMillis() - start)}. Found ${results.size()}")
        }

        results
    }

    @Override
    ReferenceDataModel findByIdJoinClassifiers(UUID id) {
        ReferenceDataModel.findById(id, [fetch: [classifiers: 'join']])
    }

    @Override
    void removeAllFromClassifier(Classifier classifier) {
        removeAllFromContainer(classifier)
    }

    @Override
    List<ReferenceDataModel> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        ReferenceDataModel.byClassifierId(classifier.id).list().
            findAll {userSecurityPolicyManager.userCanReadSecuredResourceId(ReferenceDataModel, it.id)}
    }

    @Override
    Class<ReferenceDataModel> getModelClass() {
        ReferenceDataModel
    }

    @Override
    List<ReferenceDataModel> findAllByContainerId(UUID containerId) {
        // We do not concern ourselves any other types of containers for DataModels at this time
        findAllByFolderId(containerId)
    }

    @Override
    void deleteAllInContainer(Container container) {
        if (container.instanceOf(Folder)) {
            deleteAll(ReferenceDataModel.byFolderId(container.id).id().list() as List<UUID>, true)
        }
        if (container.instanceOf(Classifier)) {
            deleteAll(ReferenceDataModel.byClassifierId(container.id).id().list() as List<UUID>, true)
        }
    }

    @Override
    void removeAllFromContainer(Container container) {
        if (container.instanceOf(Folder)) {
            ReferenceDataModel.byFolderId(container.id).list().each {
                it.folder = null
            }
        }
        if (container.instanceOf(Classifier)) {
            ReferenceDataModel.byClassifierId(container.id).list().each {
                it.removeFromClassifiers(container as Classifier)
            }
        }
    }

    @Override
    List<UUID> findAllModelIdsWithTreeChildren(List<ReferenceDataModel> models) {
        []
    }

    @Override
    List<ReferenceDataModel> findAllDeletedModels(Map pagination) {
        ReferenceDataModel.byDeleted().list(pagination)
    }

    List<ReferenceDataModel> findAllSupersededModels(List<UUID> ids, Map pagination) {
        if (!ids) return []
        ReferenceDataModel.byIdInList(ids).list(pagination)
    }

    List<ReferenceDataModel> findAllDocumentSuperseded(UserSecurityPolicyManager userSecurityPolicyManager, Map pagination = [:]) {
        ReferenceDataModel.byIdInList(findAllDocumentSupersededIds(userSecurityPolicyManager.listReadableSecuredResourceIds(ReferenceDataModel))).
            list(pagination)
    }

    List<ReferenceDataModel> findAllModelSuperseded(UserSecurityPolicyManager userSecurityPolicyManager, Map pagination = [:]) {
        ReferenceDataModel.byIdInList(findAllModelSupersededIds(userSecurityPolicyManager.listReadableSecuredResourceIds(ReferenceDataModel))).
            list(pagination)
    }

    /**
     * Find all resources by the defined user security policy manager. If none provided then assume no security policy in place in which case
     * everything is public.
     * @param userSecurityPolicyManager
     * @param pagination
     * @return
     */
    List<ReferenceDataModel> findAllByUser(UserSecurityPolicyManager userSecurityPolicyManager, Map pagination = [:]) {
        List<UUID> ids = userSecurityPolicyManager.listReadableSecuredResourceIds(ReferenceDataModel)
        ids ? ReferenceDataModel.findAllByIdInList(ids, pagination) : []
    }

    void checkfinaliseModel(ReferenceDataModel dataModel, boolean finalised) {
        if (finalised && !dataModel.finalised) {
            dataModel.finalised = finalised
            dataModel.dateFinalised = dataModel.finalised ? OffsetDateTime.now() : null
        }
        if (dataModel.finalised && !dataModel.modelVersion) {
            dataModel.modelVersion = Version.from('1.0.0')
        }
    }

    ReferenceDataModel createAndSaveDataModel(User createdBy, Folder folder, String label, String description,
                                              String author, String organisation, Authority authority = authorityService.getDefaultAuthority()) {
        ReferenceDataModel referenceDataModel = new ReferenceDataModel(createdBy: createdBy.emailAddress, label: label, description: description,
                                                                       author: author,
                                                                       organisation: organisation, folder: folder, authority: authority)

        // Have to save before adding an edit
        if (referenceDataModel.validate()) {
            referenceDataModel.save(flush: true)
            referenceDataModel.addCreatedEdit(createdBy)
        } else {
            throw new ApiInvalidModelException('DMSXX', 'Could not create new ReferenceDataModel', referenceDataModel.errors)
        }

        referenceDataModel
    }

    void setReferenceDataModelIsFromReferenceDataModel(ReferenceDataModel source, ReferenceDataModel target, User user) {
        source.addToSemanticLinks(linkType: SemanticLinkType.IS_FROM, createdBy: user.getEmailAddress(), targetCatalogueItem: target)
    }

    @Override
    List<ReferenceDataModel> findAllByMetadataNamespaceAndKey(String namespace, String key, Map pagination) {
        ReferenceDataModel.byMetadataNamespaceAndKey(namespace, key).list(pagination)
    }

    @Override
    List<ReferenceDataModel> findAllByMetadataNamespace(String namespace, Map pagination) {
        ReferenceDataModel.byMetadataNamespace(namespace).list(pagination)
    }

}