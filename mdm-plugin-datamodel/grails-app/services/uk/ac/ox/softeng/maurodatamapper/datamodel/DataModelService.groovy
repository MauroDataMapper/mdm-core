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
package uk.ac.ox.softeng.maurodatamapper.datamodel

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.authority.AuthorityService
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.ModelImport
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.provider.dataloader.DataLoaderProviderService
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClassService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElementService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataTypeService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ModelDataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration.EnumerationValue
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.DefaultDataTypeProvider
import uk.ac.ox.softeng.maurodatamapper.datamodel.similarity.DataElementSimilarityResult
import uk.ac.ox.softeng.maurodatamapper.security.SecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils
import uk.ac.ox.softeng.maurodatamapper.util.Utils
import uk.ac.ox.softeng.maurodatamapper.util.Version

import grails.gorm.DetachedCriteria
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.grails.datastore.mapping.validation.ValidationErrors
import org.hibernate.engine.spi.SessionFactoryImplementor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.Errors

@Slf4j
@Transactional
@SuppressWarnings('unused')
class DataModelService extends ModelService<DataModel> {

    DataTypeService dataTypeService
    DataClassService dataClassService
    DataElementService dataElementService
    AuthorityService authorityService
    SummaryMetadataService summaryMetadataService

    @Autowired
    Set<DefaultDataTypeProvider> defaultDataTypeProviders

    @Autowired(required = false)
    SecurityPolicyManagerService securityPolicyManagerService

    @Override
    DataModel get(Serializable id) {
        DataModel.get(id)
    }

    @Override
    List<DataModel> getAll(Collection<UUID> ids) {
        DataModel.getAll(ids).findAll()
    }

    @Override
    List<DataModel> list(Map pagination = [:]) {
        DataModel.list(pagination)
    }

    @Override
    boolean handlesPathPrefix(String pathPrefix) {
        pathPrefix == "dm"
    }

    /**
     * DataModel allows the import of DataType and DataClass
     */
    @Override
    List<Class> importsDomains() {
        [DataType, DataClass, PrimitiveType, EnumerationType, ReferenceType]
    }

    /**
     * Does the importedModelItem belong to a DataModel which is finalised, or does it belong to the same
     * collection as the importing DataModel?
     *
     * @param importingDataModel The DataModel which is importing the importedModelItem
     * @param importedModelItem The ModelItem which is being imported into importingDataModel
     *
     * @return boolean Is this import allowed by domain specific rules?
     */
    @Override
    boolean isImportableByCatalogueItem(CatalogueItem importingDataModel, CatalogueItem importedModelItem) {
        DataModel importedFromDataModel = importedModelItem.getModel()

        importedFromDataModel.finalised

        //TODO add OR importedFromModel is in the same collection as importingDataModel
    }    

    Long count() {
        DataModel.count()
    }

    int countByLabel(String label) {
        DataModel.countByLabel(label)
    }

    DataModel validate(DataModel dataModel) {
        log.debug('Validating DataModel')
        long st = System.currentTimeMillis()
        dataModel.validate()
        if (dataModel.hasErrors()) {
            Errors existingErrors = dataModel.errors
            Errors cleanedErrors = new ValidationErrors(dataModel)
            existingErrors.fieldErrors.each { fe ->
                if (!fe.field.contains('dataModel')) {
                    cleanedErrors.rejectValue(fe.field, fe.code, fe.arguments, fe.defaultMessage)
                }
                true
            }
            dataModel.errors = cleanedErrors
        }
        log.debug('Validated DataModel in {}', Utils.timeTaken(st))
        dataModel
    }

    @Override
    void deleteAll(Collection<DataModel> catalogueItems) {
        deleteAll(catalogueItems.id, true)
    }

    void delete(UUID id) {
        delete(get(id))
    }

    @Override
    void delete(DataModel dm) {
        dm?.deleted = true
    }

    @Override
    void delete(DataModel dm, boolean permanent, boolean flush = true) {
        if (!dm) return
        if (permanent) {
            if (securityPolicyManagerService) {
                securityPolicyManagerService.removeSecurityForSecurableResource(dm, null)
            }
            long start = System.currentTimeMillis()
            log.debug('Deleting DataModel')
            deleteModelAndContent(dm)
            log.debug('DataModel deleted. Took {}', Utils.timeTaken(start))
        } else delete(dm)
    }

    List<DataModel> deleteAll(List<Serializable> idsToDelete, Boolean permanent) {
        List<DataModel> updated = []
        idsToDelete.each {
            DataModel dm = get(it)
            delete(dm, permanent, false)
            if (!permanent) updated << dm
        }
        updated
    }

    @Override
    DataModel save(DataModel dataModel) {
        log.debug('Saving {}({}) without batching', dataModel.label, dataModel.ident())
        save(failOnError: true, validate: false, flush: false, dataModel)
    }

    @Override
    DataModel updateFacetsAfterInsertingCatalogueItem(DataModel catalogueItem) {
        super.updateFacetsAfterInsertingCatalogueItem(catalogueItem)
        if (catalogueItem.summaryMetadata) {
            catalogueItem.summaryMetadata.each {
                if (!it.isDirty('catalogueItemId')) it.trackChanges()
                it.catalogueItemId = catalogueItem.getId()
            }
            SummaryMetadata.saveAll(catalogueItem.summaryMetadata)
        }
        if (catalogueItem.modelImports) {
            catalogueItem.modelImports.each {
                if (!it.isDirty('catalogueItemId')) it.trackChanges()
                it.catalogueItemId = catalogueItem.getId()
            }
            ModelImport.saveAll(catalogueItem.modelImports)
        }          
        catalogueItem
    }

    @Override
    DataModel saveModelWithContent(DataModel dataModel) {

        if (dataModel.dataTypes.any { it.id } || dataModel.dataClasses.any { it.id }) {
            throw new ApiInternalException('DMSXX', 'Cannot use saveWithBatching method to save DataModel',
                                           new IllegalStateException('DataModel has previously saved content'))
        }

        log.debug('Saving {} complete datamodel', dataModel.label)

        long start = System.currentTimeMillis()
        Collection<EnumerationType> enumerationTypes = []
        Collection<PrimitiveType> primitiveTypes = []
        Collection<ModelDataType> modelDataTypes = []
        Collection<ReferenceType> referenceTypes = []
        Collection<DataClass> dataClasses = []

        if (dataModel.classifiers) {
            log.trace('Saving {} classifiers')
            classifierService.saveAll(dataModel.classifiers)
        }

        if (dataModel.dataTypes) {
            enumerationTypes.addAll dataModel.enumerationTypes
            primitiveTypes.addAll dataModel.primitiveTypes
            modelDataTypes.addAll dataModel.modelDataTypes
            referenceTypes.addAll dataModel.referenceTypes
            dataModel.enumerationTypes.clear()
            dataModel.primitiveTypes.clear()
            dataModel.modelDataTypes.clear()
            dataModel.referenceTypes.clear()
        }

        if (dataModel.dataClasses) {
            dataClasses.addAll dataModel.dataClasses
            dataModel.dataClasses.clear()
        }

        if (dataModel.breadcrumbTree.children) {
            dataModel.breadcrumbTree.children.each { it.skipValidation(true) }
        }

        save(dataModel)

        sessionFactory.currentSession.flush()

        saveContent(dataModel, enumerationTypes, primitiveTypes, referenceTypes, modelDataTypes, dataClasses)
        log.debug('Complete save of DataModel complete in {}', Utils.timeTaken(start))
        // Return the clean stored version of the datamodel, as we've messed with it so much this is much more stable
        get(dataModel.id)
    }

    @Override
    DataModel saveModelNewContentOnly(DataModel dataModel) {

        long start = System.currentTimeMillis()
        Collection<EnumerationType> enumerationTypes = []
        Collection<PrimitiveType> primitiveTypes = []
        Collection<ModelDataType> modelDataTypes = []
        Collection<ReferenceType> referenceTypes = []
        Collection<DataClass> dataClasses = []
        Set<DataElement> dataElements = [] as HashSet

        if (dataModel.classifiers) {
            log.trace('Saving {} classifiers')
            classifierService.saveAll(dataModel.classifiers)
        }

        if (dataModel.dataTypes) {
            enumerationTypes.addAll dataModel.enumerationTypes.findAll { !it.id }
            primitiveTypes.addAll dataModel.primitiveTypes.findAll { !it.id }
            modelDataTypes.addAll dataModel.modelDataTypes.findAll { !it.id }
            referenceTypes.addAll dataModel.referenceTypes.findAll { !it.id }
        }

        if (dataModel.dataClasses) {
            dataClasses.addAll dataModel.dataClasses.findAll { !it.id }
            dataElements.addAll dataModel.dataClasses.collectMany { it.dataElements.findAll { !it.id } }
        }

        saveContent(dataModel, enumerationTypes, primitiveTypes, referenceTypes, modelDataTypes, dataClasses, dataElements)
        log.debug('Complete save of DataModel complete in {}', Utils.timeTaken(start))
        // Return the clean stored version of the datamodel, as we've messed with it so much this is much more stable
        get(dataModel.id)
    }

    void saveContent(DataModel dataModel, Collection<EnumerationType> enumerationTypes,
                     Collection<PrimitiveType> primitiveTypes,
                     Collection<ReferenceType> referenceTypes,
                     Collection<ModelDataType> modelDataTypes,
                     Collection<DataClass> dataClasses,
                     Set<DataElement> dataElements = []) {

        sessionFactory.currentSession.clear()
        long start = System.currentTimeMillis()
        log.trace('Disabling validation on contents')
        enumerationTypes.each { dt ->
            dt.skipValidation(true)
            dt.enumerationValues.each { ev -> ev.skipValidation(true) }
        }
        primitiveTypes.each { it.skipValidation(true) }
        referenceTypes.each { it.skipValidation(true) }
        modelDataTypes.each { it.skipValidation(true) }
        dataClasses.each { dc ->
            dc.skipValidation(true)
            dc.dataElements.each { de -> de.skipValidation(true) }
        }
        referenceTypes.each { it.skipValidation(true) }

        long subStart = System.currentTimeMillis()
        dataTypeService.saveAll(enumerationTypes)
        dataTypeService.saveAll(primitiveTypes)
        dataTypeService.saveAll(modelDataTypes)
        int totalDts = enumerationTypes.size() + primitiveTypes.size() + modelDataTypes.size()
        log.trace('Saved {} dataTypes in {}', totalDts, Utils.timeTaken(subStart))

        subStart = System.currentTimeMillis()
        dataElements.addAll dataClassService.saveAllAndGetDataElements(dataClasses)
        log.trace('Saved {} dataClasses in {}', dataClasses.size(), Utils.timeTaken(subStart))

        subStart = System.currentTimeMillis()
        dataTypeService.saveAll(referenceTypes as Collection<DataType>)
        log.trace('Saved {} reference datatypes in {}', referenceTypes.size(), Utils.timeTaken(subStart))

        subStart = System.currentTimeMillis()
        dataElementService.saveAll(dataElements)
        log.trace('Saved {} dataElements in {}', dataElements.size(), Utils.timeTaken(subStart))

        log.trace('Content save of DataModel complete in {}', Utils.timeTaken(start))
    }

    @Override
    void deleteModelAndContent(DataModel dataModel) {

        GormUtils.disableDatabaseConstraints(sessionFactory as SessionFactoryImplementor)

        log.trace('Removing other ModelItems in DataModel')
        modelItemServices.findAll {
            !(it.modelItemClass in [DataClass, DataElement, DataType, EnumerationType, ModelDataType, PrimitiveType,
                                    ReferenceType, EnumerationValue])
        }.each { modelItemService ->
            try {
                modelItemService.deleteAllByModelId(dataModel.id)
            } catch (ApiNotYetImplementedException ignored) {
            }
        }

        log.trace('Removing DataClasses in DataModel')
        dataClassService.deleteAllByModelId(dataModel.id)

        log.trace('Removing DataTypes in DataModel')
        dataTypeService.deleteAllByModelId(dataModel.id)

        log.trace('Removing facets')
        deleteAllFacetsByCatalogueItemId(dataModel.id, 'delete from datamodel.join_datamodel_to_facet where datamodel_id=:id')

        log.trace('Content removed')
        sessionFactory.currentSession
            .createSQLQuery('delete from datamodel.data_model where id = :id')
            .setParameter('id', dataModel.id)
            .executeUpdate()

        log.trace('DataModel removed')

        sessionFactory.currentSession
            .createSQLQuery('DELETE FROM core.breadcrumb_tree WHERE domain_id = :id')
            .setParameter('id', dataModel.id)
            .executeUpdate()

        log.trace('Breadcrumb tree removed')

        GormUtils.enableDatabaseConstraints(sessionFactory as SessionFactoryImplementor)
    }

    void removeSummaryMetadataFromCatalogueItem(UUID catalogueItemId, SummaryMetadata summaryMetadata) {
        removeFacetFromDomain(catalogueItemId, summaryMetadata.id, 'summaryMetadata')
    }

    @Override
    List<DataModel> findAllReadableModels(List<UUID> constrainedIds, boolean includeDeleted) {
        DataModel.withReadable(DataModel.by(), constrainedIds, includeDeleted).list()
    }

    @Override
    void deleteAllFacetDataByCatalogueItemIds(List<UUID> catalogueItemIds) {
        super.deleteAllFacetDataByCatalogueItemIds(catalogueItemIds)
        summaryMetadataService.deleteAllByCatalogueItemIds(catalogueItemIds)
    }

    @Override
    List<DataModel> findAllReadableByEveryone() {
        DataModel.findAllByReadableByEveryone(true)
    }

    @Override
    List<DataModel> findAllReadableByAuthenticatedUsers() {
        DataModel.findAllByReadableByAuthenticatedUsers(true)
    }

    List<DataModel> findAllDataAssets() {
        findAllByModelType(DataModelType.DATA_ASSET)
    }

    List<DataModel> findAllDataStandards() {
        findAllByModelType(DataModelType.DATA_STANDARD)
    }

    List<DataModel> findAllByModelType(DataModelType type) {
        DataModel.findAllByModelType(type.label)
    }

    List<DataModel> findAllByModelType(String type) {
        DataModel.findAllByModelType(type)
    }

    @Override
    List<DataModel> findAllByLabel(String label) {
        DataModel.findAllByLabel(label)
    }

    @Override
    List<UUID> findAllModelIds() {
        DataModel.by().id().list() as List<UUID>
    }

    @Override
    List<DataModel> findAllByMetadataNamespaceAndKey(String namespace, String key, Map pagination = [:]) {
        DataModel.byMetadataNamespaceAndKey(namespace, key).list(pagination)
    }

    @Override
    List<DataModel> findAllByMetadataNamespace(String namespace, Map pagination = [:]) {
        DataModel.byMetadataNamespace(namespace).list(pagination)
    }

    @Override
    List<DataModel> findAllByFolderId(UUID folderId) {
        DataModel.byFolderId(folderId).list() as List<DataModel>
    }

    List<UUID> findAllIdsByFolderId(UUID folderId) {
        DataModel.byFolderId(folderId).id().list() as List<UUID>
    }

    List<DataModel> findAllDeleted(Map pagination = [:]) {
        DataModel.byDeleted().list(pagination) as List<DataModel>
    }

    int countAllByLabelAndBranchNameAndNotFinalised(String label, String branchName) {
        DataModel.countByLabelAndBranchNameAndFinalised(label, branchName, false)
    }

    DataModel findLatestByDataLoaderPlugin(DataLoaderProviderService dataLoaderProviderService) {
        // If we get all the models with the DL metadata then sort by documentation version we should end up with the latest doc version.
        // We should do this rather than using the value of the metadata as its possible someone creates a new documentation version of the model
        // and we should use that one. All DL loaded models have the doc version set to the DL version.
        DataModel latest = DataModel
            .byMetadataNamespaceAndKey(dataLoaderProviderService.namespace, dataLoaderProviderService.name)
            .get([order: 'desc', sort: 'documentationVersion'])
        log.debug('Found DataModel {}({}) which matches DataLoaderPlugin {}({})', latest.label, latest.documentationVersion,
                  dataLoaderProviderService.name, dataLoaderProviderService.version)
        latest
    }

    DataModel checkForAndAddDefaultDataTypes(DataModel resource, String defaultDataTypeProvider) {
        if (defaultDataTypeProvider) {
            DefaultDataTypeProvider provider = defaultDataTypeProviders.find { it.name == defaultDataTypeProvider }
            if (provider) {
                log.debug("Adding ${provider.displayName} default DataTypes")
                return dataTypeService.addDefaultListOfDataTypesToDataModel(resource, provider.defaultListOfDataTypes)
            }
        }
        resource
    }

    void deleteAllUnusedDataTypes(DataModel dataModel) {
        log.debug('Cleaning DataModel {} of DataTypes', dataModel.label)
        dataModel.dataTypes.findAll { !it.dataElements }.each {
            dataTypeService.delete(it)
        }
    }

    void deleteAllUnusedDataClasses(DataModel dataModel) {
        log.debug('Cleaning DataModel {} of DataClasses', dataModel.label)
        dataModel.dataClasses.findAll { dataClassService.isUnusedDataClass(it) }.each {
            dataClassService.delete(it)
        }
    }

    void checkImportedDataModelAssociations(User importingUser, DataModel dataModel, Map bindingMap = [:]) {
        dataModel.createdBy = importingUser.emailAddress
        dataModel.authority = authorityService.getDefaultAuthority()
        checkFacetsAfterImportingCatalogueItem(dataModel)

        if (dataModel.dataClasses) {
            dataModel.fullSortOfChildren(dataModel.childDataClasses)
            Collection<DataClass> dataClasses = dataModel.childDataClasses
            dataClasses.each { dc ->
                dataClassService.checkImportedDataClassAssociations(importingUser, dataModel, dc, !bindingMap.isEmpty())
            }
        }

        if (bindingMap && dataModel.dataTypes) {
            Set<ReferenceType> referenceTypes = dataModel.dataTypes.findAll { it.instanceOf(ReferenceType) } as Set<ReferenceType>
            if (referenceTypes) {
                log.debug('Matching {} ReferenceType referenceClasses', referenceTypes.size())
                dataTypeService.matchReferenceClasses(dataModel, referenceTypes,
                                                      bindingMap.dataTypes.findAll { it.domainType == DataType.REFERENCE_DOMAIN_TYPE })
            }
        }

        // Make sure we have all the DTs inside the DM first as some will have been imported from the DEs
        if (dataModel.dataTypes) {
            dataModel.fullSortOfChildren(dataModel.dataTypes)
            dataModel.dataTypes.each { dt ->
                dataTypeService.checkImportedDataTypeAssociations(importingUser, dataModel, dt)
            }
        }

        log.debug('DataModel associations checked')
    }

    DataModel ensureAllEnumerationTypesHaveValues(DataModel dataModel) {
        dataModel.dataTypes.findAll { it.instanceOf(EnumerationType) && !(it as EnumerationType).getEnumerationValues() }.each { EnumerationType et ->
            et.addToEnumerationValues(key: '-', value: '-')
        }
        dataModel
    }

    List<DataElement> getAllDataElementsOfDataModel(DataModel dataModel) {
        List<DataElement> allElements = []
        dataModel.dataClasses.each { allElements += it.dataElements ?: [] }
        allElements
    }

    List<DataElement> findAllDataElementsWithNames(DataModel dataModel, Set<String> dataElementNames, boolean caseInsensitive) {
        if (!dataElementNames) return []
        getAllDataElementsOfDataModel(dataModel).findAll {
            caseInsensitive ?
            it.label.toLowerCase() in dataElementNames.collect { it.toLowerCase() } :
            it.label in dataElementNames
        }
    }

    Set<EnumerationType> findAllEnumerationTypeByNames(DataModel dataModel, Set<String> enumerationTypeNames, boolean caseInsensitive) {
        if (!enumerationTypeNames) return []
        dataModel.dataTypes.findAll { it.instanceOf(EnumerationType) }.findAll {
            caseInsensitive ?
            it.label.toLowerCase() in enumerationTypeNames.collect { it.toLowerCase() } :
            it.label in enumerationTypeNames
        } as Set<EnumerationType>
    }

    @Override
    DataModel copyModelAsNewForkModel(DataModel original, User copier, boolean copyPermissions, String label, boolean throwErrors,
                                      UserSecurityPolicyManager userSecurityPolicyManager) {
        copyModel(original, copier, copyPermissions, label, Version.from('1'), original.branchName, throwErrors, userSecurityPolicyManager,
                  false)
    }

    @Override
    DataModel copyModel(DataModel original, User copier, boolean copyPermissions, String label, Version copyDocVersion, String branchName,
                        boolean throwErrors, UserSecurityPolicyManager userSecurityPolicyManager) {
        copyModel(original, copier, copyPermissions, label, copyDocVersion, branchName, throwErrors, userSecurityPolicyManager, true)
    }

    DataModel copyModel(DataModel original, User copier, boolean copyPermissions, String label, Version copyDocVersion, String branchName,
                        boolean throwErrors, UserSecurityPolicyManager userSecurityPolicyManager, boolean copySummaryMetadata) {

        Folder folder = proxyHandler.unwrapIfProxy(original.folder) as Folder
        DataModel copy = new DataModel(author: original.author, organisation: original.organisation, modelType: original.modelType, finalised: false,
                                       deleted: false, documentationVersion: copyDocVersion, folder: folder, authority: original.authority,
                                       branchName: branchName
        )

        copy = copyCatalogueItemInformation(original, copy, copier, userSecurityPolicyManager, copySummaryMetadata)
        copy.label = label

        if (copyPermissions) {
            if (throwErrors) {
                throw new ApiNotYetImplementedException('DMSXX', 'DataModel permission copying')
            }
            log.warn('Permission copying is not yet implemented')

        }

        setCatalogueItemRefinesCatalogueItem(copy, original, copier)

        if (copy.validate()) {
            save(copy, validate: false)
            editService.createAndSaveEdit(copy.id, copy.domainType,
                                          "DataModel ${original.modelType}:${original.label} created as a copy of ${original.id}",
                                          copier
            )
        } else throw new ApiInvalidModelException('DMS01', 'Copied DataModel is invalid', copy.errors, messageSource)

        copy.trackChanges()

        if (original.dataTypes) {
            // Copy all the datatypes
            original.dataTypes.each { dt ->
                dataTypeService.copyDataType(copy, dt, copier, userSecurityPolicyManager)
            }
        }

        if (original.childDataClasses) {
            // Copy all the dataclasses (this will also match up the reference types)
            original.childDataClasses.each {dc ->
                dataClassService.copyDataClass(copy, dc, copier, userSecurityPolicyManager)
            }
        }

        copy
    }

    @Override
    DataModel copyCatalogueItemInformation(DataModel original,
                                           DataModel copy,
                                           User copier,
                                           UserSecurityPolicyManager userSecurityPolicyManager) {
        copyCatalogueItemInformation(original, copy, copier, userSecurityPolicyManager, false)
    }

    DataModel copyCatalogueItemInformation(DataModel original,
                                           DataModel copy,
                                           User copier,
                                           UserSecurityPolicyManager userSecurityPolicyManager,
                                           boolean copySummaryMetadata) {
        copy = super.copyCatalogueItemInformation(original, copy, copier, userSecurityPolicyManager) as DataModel
        if (copySummaryMetadata) {
            summaryMetadataService.findAllByCatalogueItemId(original.id).each {
                copy.addToSummaryMetadata(label: it.label, summaryMetadataType: it.summaryMetadataType, createdBy: copier.emailAddress)
            }
        }

        modelImportService.findAllByCatalogueItemId(original.id).each { 
            copy.addToModelImports(it.importedCatalogueItemDomainType,
                                   it.importedCatalogueItemId,
                                   copier) 
        }        
        copy
    }

    List<DataElementSimilarityResult> suggestLinksBetweenModels(DataModel dataModel, DataModel otherDataModel, int maxResults) {
        dataModel.getAllDataElements().collect { de ->
            dataElementService.findAllSimilarDataElementsInDataModel(otherDataModel, de, maxResults)
        }
    }


    Map<UUID, Long> obtainChildKnowledge(List<DataModel> parents) {
        if (!parents) return [:]
        DetachedCriteria<DataClass> criteria = new DetachedCriteria<DataClass>(DataClass)
            .isNull('parentDataClass')
            .inList('dataModel', parents)
            .projections {
                groupProperty('dataModel.id')
                count()
            }.order('dataModel')
        criteria.list().collectEntries { [it[0], it[1]] }
    }

    @Override
    boolean hasTreeTypeModelItems(DataModel dataModel, boolean forDiff, boolean includeImported = false) {
        dataClassService.countByDataModelId(dataModel.id) || (dataModel.dataTypes && forDiff) || (dataModel.modelImports && includeImported)
    }

    @Override
    List<ModelItem> findAllTreeTypeModelItemsIn(DataModel catalogueItem, boolean forDiff = false, boolean includeImported = false) {
        (dataClassService.findAllWhereRootDataClassOfDataModelId(catalogueItem.id, [:], includeImported) +
         (forDiff ? DataType.byDataModelId(catalogueItem.id).list() : []) as List<ModelItem>)
    }

    @Override
    Boolean shouldPerformSearchForTreeTypeCatalogueItems(String domainType) {
        !domainType || domainType == DataModel.simpleName
    }

    @Override
    List<DataModel> findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                               String searchTerm, String domainType) {
        List<UUID> readableIds = userSecurityPolicyManager.listReadableSecuredResourceIds(DataModel)
        if (!readableIds) return []

        List<DataModel> results = []
        if (shouldPerformSearchForTreeTypeCatalogueItems(domainType)) {
            log.debug('Performing lucene label search')
            long start = System.currentTimeMillis()
            results = DataModel.luceneLabelSearch(DataModel, searchTerm, readableIds.toList()).results
            log.debug("Search took: ${Utils.getTimeString(System.currentTimeMillis() - start)}. Found ${results.size()}")
        }

        results
    }

    @Override
    DataModel findByIdJoinClassifiers(UUID id) {
        DataModel.findById(id, [fetch: [classifiers: 'join']])
    }

    @Override
    void removeAllFromClassifier(Classifier classifier) {
        removeAllFromContainer(classifier)
    }

    @Override
    List<DataModel> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        DataModel.byClassifierId(classifier.id)
            .list()
            .findAll {userSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, it.id)} as List<DataModel>
    }

    @Override
    Class<DataModel> getModelClass() {
        DataModel
    }

    @Override
    List<DataModel> findAllByContainerId(UUID containerId) {
        // We do not concern ourselves any other types of containers for DataModels at this time
        findAllByFolderId(containerId)
    }

    @Override
    void deleteAllInContainer(Container container) {
        if (container.instanceOf(Folder)) {
            deleteAll(DataModel.byFolderId(container.id).id().list() as List<UUID>, true)
        }
        if (container.instanceOf(Classifier)) {
            deleteAll(DataModel.byClassifierId(container.id).id().list() as List<UUID>, true)
        }
    }

    @Override
    void removeAllFromContainer(Container container) {
        if (container.instanceOf(Folder)) {
            DataModel.byFolderId(container.id).list().each {
                it.folder = null
            }
        }
        if (container.instanceOf(Classifier)) {
            DataModel.byClassifierId(container.id).list().each {
                it.removeFromClassifiers(container as Classifier)
            }
        }
    }

    @Override
    List<UUID> findAllModelIdsWithTreeChildren(List<DataModel> models) {
        models.collect { it.id }.findAll { dataClassService.countByDataModelId(it) }
    }

    @Override
    List<DataModel> findAllDeletedModels(Map pagination) {
        DataModel.byDeleted().list(pagination) as List<DataModel>
    }

    List<DataModel> findAllSupersededModels(List<UUID> ids, Map pagination) {
        if (!ids) return []
        DataModel.byIdInList(ids).list(pagination) as List<DataModel>
    }

    List<DataModel> findAllDocumentSuperseded(UserSecurityPolicyManager userSecurityPolicyManager, Map pagination = [:]) {
        DataModel.byIdInList(findAllDocumentSupersededIds(userSecurityPolicyManager.listReadableSecuredResourceIds(DataModel))).list(pagination) as
            List<DataModel>
    }

    List<DataModel> findAllModelSuperseded(UserSecurityPolicyManager userSecurityPolicyManager, Map pagination = [:]) {
        DataModel.byIdInList(findAllModelSupersededIds(userSecurityPolicyManager.listReadableSecuredResourceIds(DataModel))).list(pagination) as
            List<DataModel>
    }

    /**
     * Find all resources by the defined user security policy manager. If none provided then assume no security policy in place in which case
     * everything is public.
     * @param userSecurityPolicyManager
     * @param pagination
     * @return
     */
    @Override
    List<DataModel> findAllByUser(UserSecurityPolicyManager userSecurityPolicyManager, Map pagination = [:]) {
        List<UUID> ids = userSecurityPolicyManager.listReadableSecuredResourceIds(DataModel)
        ids ? DataModel.byIds(ids).list(pagination) : []
    }

    DataModel createAndSaveDataModel(User createdBy, Folder folder, DataModelType type, String label, String description,
                                     String author, String organisation, Authority authority = authorityService.getDefaultAuthority(),
                                     boolean saveDataModel = true) {
        DataModel dataModel = new DataModel(createdBy: createdBy.emailAddress, label: label, description: description, author: author,
                                            organisation: organisation, type: type, folder: folder, authority: authority)
        if (saveDataModel) {
            if (validate(dataModel)) {
                save(dataModel, flush: true) // Have to save before adding an edit
                dataModel.addCreatedEdit(createdBy)
            } else throw new ApiInvalidModelException('DMSXX', 'Could not create new DataModel', dataModel.errors)
        }
        dataModel
    }

    /**
     * Find a DataModel by label.
     * @param label
     * @return The found DataModel
     */
    DataModel findByLabel(String label) {
        DataModel.findByLabel(label)
    }
}
