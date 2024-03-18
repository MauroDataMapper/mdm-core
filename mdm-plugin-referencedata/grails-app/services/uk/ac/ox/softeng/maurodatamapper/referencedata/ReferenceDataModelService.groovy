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
package uk.ac.ox.softeng.maurodatamapper.referencedata

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.diff.CachedDiffable
import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffCache
import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable
import uk.ac.ox.softeng.maurodatamapper.core.facet.EditTitle
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.provider.dataloader.DataLoaderProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.ModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.CopyInformation
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.MultiFacetItemAware
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadataAware
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadataService
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElement
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElementService
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataValue
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataValueService
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataTypeService
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceEnumerationType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.enumeration.ReferenceEnumerationValue
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.enumeration.ReferenceEnumerationValueService
import uk.ac.ox.softeng.maurodatamapper.referencedata.provider.DefaultReferenceDataTypeProvider
import uk.ac.ox.softeng.maurodatamapper.referencedata.provider.exporter.ReferenceDataJsonExporterService
import uk.ac.ox.softeng.maurodatamapper.referencedata.provider.exporter.ReferenceDataModelExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer.ReferenceDataJsonImporterService
import uk.ac.ox.softeng.maurodatamapper.referencedata.similarity.ReferenceDataElementSimilarityResult
import uk.ac.ox.softeng.maurodatamapper.referencedata.traits.service.ReferenceSummaryMetadataAwareService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils
import uk.ac.ox.softeng.maurodatamapper.util.Utils
import uk.ac.ox.softeng.maurodatamapper.version.Version

import grails.gorm.transactions.Transactional
import grails.util.Environment
import groovy.util.logging.Slf4j
import org.hibernate.engine.spi.SessionFactoryImplementor
import org.hibernate.search.mapper.orm.Search
import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy
import org.hibernate.search.mapper.orm.session.SearchSession
import org.springframework.beans.factory.annotation.Autowired

import java.time.OffsetDateTime

@Slf4j
@Transactional
@SuppressWarnings('unused')
class ReferenceDataModelService extends ModelService<ReferenceDataModel> implements ReferenceSummaryMetadataAwareService {

    ReferenceDataElementService referenceDataElementService
    ReferenceDataTypeService referenceDataTypeService
    ReferenceDataValueService referenceDataValueService
    ReferenceSummaryMetadataService referenceSummaryMetadataService
    ReferenceDataJsonImporterService referenceDataJsonImporterService
    ReferenceDataJsonExporterService referenceDataJsonExporterService
    ReferenceEnumerationValueService referenceEnumerationValueService

    @Autowired
    Set<DefaultReferenceDataTypeProvider> defaultReferenceDataTypeProviders

    @Autowired(required = false)
    Set<ReferenceDataModelExporterProviderService> exporterProviderServices

    @Override
    ReferenceDataModel get(Serializable id) {
        ReferenceDataModel.get(id)
    }

    @Override
    List<ReferenceDataModel> getAll(Collection<UUID> ids) {
        ReferenceDataModel.getAll(ids).findAll().collect {unwrapIfProxy(it)}
    }

    @Override
    List<ReferenceDataModel> list(Map pagination) {
        ReferenceDataModel.list(pagination)
    }

    @Override
    List<ReferenceDataModel> list() {
        ReferenceDataModel.list().collect {unwrapIfProxy(it)}
    }

    Long count() {
        ReferenceDataModel.count()
    }

    int countByAuthorityAndLabel(Authority authority, String label) {
        ReferenceDataModel.countByAuthorityAndLabel(authority, label)
    }

    ReferenceDataModel validate(ReferenceDataModel referenceDataModel) {
        referenceDataModel.validate()
        referenceDataModel
    }

    @Override
    String getUrlResourceName() {
        'referenceDataModels'
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
            rdm.delete(flush: flush)
            if (securityPolicyManagerService) {
                securityPolicyManagerService.removeSecurityForSecurableResource(rdm, null)
            }
        } else delete(rdm)
    }

    void deleteModelsAndContent(Set<UUID> idsToDelete) {
        ReferenceDataModel.deleteAll(getAll(idsToDelete))
    }

    @Override
    ReferenceDataModel save(ReferenceDataModel referenceDataModel) {
        log.debug('Saving {}({}) without batching', referenceDataModel.label, referenceDataModel.ident())
        save(failOnError: true, validate: false, flush: false, referenceDataModel)
    }

    ReferenceDataModel saveModelWithContent(ReferenceDataModel referenceDataModel) {

        if (referenceDataModel.referenceDataTypes.any {it.id} ||
            referenceDataModel.referenceDataValues.any {it.id}) {
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

        if (referenceDataModel.referenceDataValues) {
            referenceDataValues.addAll referenceDataModel.referenceDataValues
            referenceDataModel.referenceDataValues.clear()
        }

        if (referenceDataModel.breadcrumbTree.children) {
            referenceDataModel.breadcrumbTree.disableValidation()
        }

        // Set this HS session to be async mode, this is faster and as we dont need to read the indexes its perfectly safe
        //SearchSession searchSession = Search.session(sessionFactory.currentSession)
        //searchSession.automaticIndexingSynchronizationStrategy(AutomaticIndexingSynchronizationStrategy.async())

        save(failOnError: true, validate: false, flush: false, ignoreBreadcrumbs: true, referenceDataModel)

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

    void saveContent(Collection<ReferenceDataType> referenceDataTypes, Collection<ReferenceDataElement> referenceDataElements,
                     Collection<ReferenceDataValue> referenceDataValues) {

        sessionFactory.currentSession.clear()

        //Skip validation on all contents
        long start = System.currentTimeMillis()
        log.debug('Disabling validation on contents')
        referenceDataTypes.each {
            it.skipValidation(true)
            it.referenceDataElements?.clear()
        }
        referenceDataElements.each {
            it.skipValidation(true)
            it.referenceDataValues?.clear()
        }
        referenceDataValues.each {
            it.skipValidation(true)
        }

        // During testing its very important that we dont disable constraints otherwise we may miss an invalid model,
        // The disabling is done to provide a speed up during saving which is not necessary during test
        if (Environment.current != Environment.TEST) {
            log.debug('Disabling database constraints')
            GormUtils.disableDatabaseConstraints(sessionFactory as SessionFactoryImplementor)
        }

        long subStart = System.currentTimeMillis()
        referenceDataTypeService.saveAll(referenceDataTypes)
        log.debug('Saved {} referenceDataTypes took {}', referenceDataTypes.size(), Utils.timeTaken(subStart))

        subStart = System.currentTimeMillis()
        referenceDataElementService.saveAll(referenceDataElements)
        log.debug('Saved {} referenceDataElements took {}', referenceDataElements.size(), Utils.timeTaken(subStart))

        subStart = System.currentTimeMillis()
        referenceDataValueService.saveAll(referenceDataValues)
        log.debug('Saved {} referenceDataValues took {}', referenceDataValues.size(), Utils.timeTaken(subStart))

        if (Environment.current != Environment.TEST) {
            log.debug('Enabling database constraints')
            GormUtils.enableDatabaseConstraints(sessionFactory as SessionFactoryImplementor)
        }

        log.trace('Content save of ReferenceDataModel complete in {}', Utils.timeTaken(start))
    }

    @Override
    List<ReferenceDataModel> findAllReadableByEveryone() {
        ReferenceDataModel.findAllByReadableByEveryone(true)
    }

    @Override
    List<ReferenceDataModel> findAllReadableByAuthenticatedUsers() {
        ReferenceDataModel.findAllByReadableByAuthenticatedUsers(true)
    }

    List<ReferenceDataModel> findAllByAuthorityAndLabel(Authority authority, String label) {
        ReferenceDataModel.findAllByAuthorityAndLabel(authority, label)
    }

    @Override
    List<UUID> getAllModelIds() {
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

    @Override
    int countByAuthorityAndLabelAndBranchNameAndNotFinalised(Authority authority, String label, String branchName) {
        ReferenceDataModel.countByAuthorityAndLabelAndBranchNameAndFinalised(authority, label, branchName, false)
    }

    @Override
    int countByAuthorityAndLabelAndVersion(Authority authority, String label, Version modelVersion) {
        ReferenceDataModel.countByAuthorityAndLabelAndModelVersion(authority, label, modelVersion)
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
     * Reference Data Value. When importing the Reference Data Values, we want to set an association for Reference Data Element (and
     * thus also Reference Data Type) to the entity imported at the model level, rather than creating duplicated entities by creating
     * a new Reference Data Element and Reference Data Type for every value. This is done in the call to checkImportedReferenceDataValueAssociations.
     */
    void checkImportedReferenceDataModelAssociations(User importingUser, ReferenceDataModel referenceDataModel, Map bindingMap = [:]) {
        referenceDataModel.createdBy = importingUser.emailAddress
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
        Folder folder = proxyHandler.unwrapIfProxy(original.folder) as Folder
        copyModel(original, folder, copier, copyPermissions, label, Version.from('1'), original.branchName, throwErrors,
                  userSecurityPolicyManager,
                  false)
    }

    ReferenceDataModel copyModel(ReferenceDataModel original, Folder folderToCopyTo, User copier, boolean copyPermissions, String label,
                                 Version copyDocVersion, String branchName, boolean throwErrors,
                                 UserSecurityPolicyManager userSecurityPolicyManager) {
        copyModel(original, folderToCopyTo, copier, copyPermissions, label, copyDocVersion, branchName, throwErrors, userSecurityPolicyManager, true)
    }

    ReferenceDataModel copyModel(ReferenceDataModel original, Folder folderToCopyInto, User copier, boolean copyPermissions, String label,
                                 Version copyDocVersion, String branchName,
                                 boolean throwErrors, UserSecurityPolicyManager userSecurityPolicyManager, boolean copySummaryMetadata) {

        ReferenceDataModel copy = new ReferenceDataModel(author: original.author, organisation: original.organisation, modelType: original.modelType,
                                                         finalised: false,
                                                         deleted: false, documentationVersion: copyDocVersion, folder: folderToCopyInto,
                                                         authority: authorityService.defaultAuthority,
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
            editService.createAndSaveEdit(EditTitle.COPY, copy.id, copy.domainType,
                                          "ReferenceDataModel ${original.modelType}:${original.label} created as a copy of ${original.id}",
                                          copier
            )
        } else throw new ApiInvalidModelException('DMS01', 'Copied ReferenceDataModel is invalid', copy.errors, messageSource)

        copy.trackChanges()

        CopyInformation referenceDataTypeCopyInformation = new CopyInformation(copyIndex: true)
        if (original.referenceDataTypes) {
            // Copy all the referencedatatypes
            original.referenceDataTypes.sort().each {dt ->
                referenceDataTypeService.copyReferenceDataType(copy, dt, copier, userSecurityPolicyManager, copySummaryMetadata, referenceDataTypeCopyInformation)
            }
        }

        CopyInformation referenceDataElementCopyInformation = new CopyInformation(copyIndex: true)
        if (original.referenceDataElements) {
            // Copy all the referencedataelements
            original.referenceDataElements.sort().each {de ->
                log.debug("copy element ${de}")
                referenceDataElementService.copyReferenceDataElement(copy, de, copier, userSecurityPolicyManager, copySummaryMetadata, referenceDataElementCopyInformation)
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
            referenceSummaryMetadataService.findAllByMultiFacetAwareItemId(original.id).each {
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
            log.debug('Performing hs label search')
            long start = System.currentTimeMillis()
            results = ReferenceDataModel.labelHibernateSearch(ReferenceDataModel, searchTerm, readableIds.toList(), []).results
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
    List<ReferenceDataModel> findAllByClassifier(Classifier classifier) {
        ReferenceDataModel.byClassifierId(classifier.id).list() as List<ReferenceDataModel>
    }

    @Override
    List<ReferenceDataModel> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        findAllByClassifier(classifier).findAll {userSecurityPolicyManager.userCanReadSecuredResourceId(ReferenceDataModel, it.id)}
    }

    @Override
    Integer countByContainerId(UUID containerId) {
        ReferenceDataModel.byFolderId(containerId).count()
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

    List<ReferenceDataModel> findAllModelsByIdInList(List<UUID> ids, Map pagination) {
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
        source.addToSemanticLinks(linkType: SemanticLinkType.IS_FROM, createdBy: user.getEmailAddress(), targetMultiFacetAwareItem: target)
    }


    @Override
    ModelImporterProviderService<ReferenceDataModel, ? extends ModelImporterProviderServiceParameters> getJsonModelImporterProviderService() {
        referenceDataJsonImporterService
    }

    @Override
    ReferenceDataJsonExporterService getJsonModelExporterProviderService() {
        referenceDataJsonExporterService
    }

    @Override
    void updateModelItemPathsAfterFinalisationOfModel(ReferenceDataModel model) {
        updateModelItemPathsAfterFinalisationOfModel model, 'referencedata',
                                                     'reference_data_element',
                                                     'reference_data_type',
                                                     'reference_data_value',
                                                     'reference_enumeration_value'
    }

    @Override
    List<ReferenceDataModel> findAllByMetadataNamespaceAndKey(String namespace, String key, Map pagination) {
        ReferenceDataModel.byMetadataNamespaceAndKey(namespace, key).list(pagination)
    }

    @Override
    List<ReferenceDataModel> findAllByMetadataNamespace(String namespace, Map pagination) {
        ReferenceDataModel.byMetadataNamespace(namespace).list(pagination)
    }

    @Override
    CatalogueItem processDeletionPatchOfFacet(MultiFacetItemAware multiFacetItemAware, Model targetModel, Path path) {
        CatalogueItem catalogueItem = super.processDeletionPatchOfFacet(multiFacetItemAware, targetModel, path)

        if (multiFacetItemAware.domainType == ReferenceSummaryMetadata.simpleName) {
            (catalogueItem as ReferenceSummaryMetadataAware).referenceSummaryMetadata.remove(multiFacetItemAware)
        }

        catalogueItem
    }

    @Override
    CachedDiffable<ReferenceDataModel> loadEntireModelIntoDiffCache(UUID modelId) {
        long start = System.currentTimeMillis()
        ReferenceDataModel loadedModel = get(modelId)
        log.debug('Loading entire model [{}] into memory', loadedModel.path)

        // Load direct content

        log.trace('Loading ReferenceDataType')
        List<ReferenceDataType> dataTypes = referenceDataTypeService.findAllByReferenceDataModelId(loadedModel.id)

        log.trace('Loading ReferenceEnumerationValue')
        List<ReferenceEnumerationValue> enumerationValues = referenceEnumerationValueService.findAllByReferenceDataModelId(modelId)
        Map<UUID, List<ReferenceEnumerationValue>> enumerationValuesMap = enumerationValues.groupBy {it.referenceEnumerationType.id}

        log.trace('Loading ReferenceDataElement')
        List<ReferenceDataElement> dataElements = referenceDataElementService.findAllByReferenceDataModelId(loadedModel.id)

        log.trace('Loading ReferenceDataValues')
        Map<UUID, List<ReferenceDataValue>> dataValuesMap = referenceDataValueService.findAllByReferenceDataModelId(loadedModel.id).groupBy {it.referenceDataElement.id}

        log.trace('Loading Facets')
        List<UUID> allIds = Utils.gatherIds(Collections.singleton(modelId),
                                            dataElements.collect {it.id},
                                            dataTypes.collect {it.id},
                                            enumerationValues.collect {it.id})
        Map<String, Map<UUID, List<Diffable>>> facetData = loadAllDiffableFacetsIntoMemoryByIds(allIds)

        DiffCache diffCache = createCatalogueItemDiffCache(null, loadedModel, facetData)
        createReferenceDataTypeDiffCaches(diffCache, dataTypes, enumerationValuesMap, facetData)
        createReferenceDataElementDiffCaches(diffCache, dataElements, dataValuesMap, facetData)

        log.debug('Model loaded into memory, took {}', Utils.timeTaken(start))
        new CachedDiffable(loadedModel, diffCache)
    }

    private void createReferenceDataTypeDiffCaches(DiffCache diffCache, List<ReferenceDataType> dataTypes, Map<UUID, List<ReferenceEnumerationValue>> enumerationValuesMap,
                                                   Map<String, Map<UUID, List<Diffable>>> facetData) {
        diffCache.addField('referenceDataTypes', dataTypes)
        dataTypes.each {dt ->
            DiffCache dtDiffCache = createCatalogueItemDiffCache(diffCache, dt, facetData)
            if (dt.instanceOf(ReferenceEnumerationType)) {
                createCatalogueItemDiffCaches(dtDiffCache, 'referenceEnumerationValues', enumerationValuesMap[dt.id], facetData)
            }
        }
    }


    private void createReferenceDataElementDiffCaches(DiffCache diffCache, List<ReferenceDataElement> dataElements, Map<UUID, List<ReferenceDataValue>> referenceDataValues,
                                                      Map<String, Map<UUID, List<Diffable>>> facetData) {
        diffCache.addField('referenceDataElements', dataElements)
        dataElements.each {de ->
            DiffCache deDiffCache = createCatalogueItemDiffCache(diffCache, de, facetData)
            deDiffCache.addField('referenceDataValues', referenceDataValues[de.id])

        }
    }
}