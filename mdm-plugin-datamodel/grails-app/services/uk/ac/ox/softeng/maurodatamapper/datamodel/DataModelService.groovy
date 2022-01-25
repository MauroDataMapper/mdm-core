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
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.EditTitle
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.provider.dataloader.DataLoaderProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.ModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.CopyInformation
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.MultiFacetItemAware
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataAware
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
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter.DataModelJsonExporterService
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.DataModelJsonImporterService
import uk.ac.ox.softeng.maurodatamapper.datamodel.similarity.DataElementSimilarityResult
import uk.ac.ox.softeng.maurodatamapper.datamodel.traits.service.SummaryMetadataAwareService
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.path.PathNode
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.traits.domain.CreatorAware
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils
import uk.ac.ox.softeng.maurodatamapper.util.Utils
import uk.ac.ox.softeng.maurodatamapper.version.Version

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
class DataModelService extends ModelService<DataModel> implements SummaryMetadataAwareService {

    DataTypeService dataTypeService
    DataClassService dataClassService
    DataElementService dataElementService
    SummaryMetadataService summaryMetadataService
    DataModelJsonImporterService dataModelJsonImporterService
    DataModelJsonExporterService dataModelJsonExporterService

    @Autowired
    Set<DefaultDataTypeProvider> defaultDataTypeProviders

    @Autowired(required = false)
    Set<ModelService> modelServices

    @Override
    DataModel get(Serializable id) {
        DataModel.get(id)
    }

    @Override
    List<DataModel> getAll(Collection<UUID> ids) {
        DataModel.getAll(ids).findAll().collect {unwrapIfProxy(it)}
    }

    @Override
    List<DataModel> list(Map pagination) {
        DataModel.list(pagination)
    }

    @Override
    List<DataModel> list() {
        DataModel.list().collect { unwrapIfProxy(it) }
    }

    @Override
    String getUrlResourceName() {
        "dataModels"
    }

    /**
     * DataModel allows the import of DataType and DataClass
     *
     @Override
      List<Class>                domainImportableModelItemClasses() {[DataType, DataClass, PrimitiveType, EnumerationType, ReferenceType]}
     */
    Long count() {
        DataModel.count()
    }

    int countByAuthorityAndLabel(Authority authority, String label) {
        DataModel.countByAuthorityAndLabel(authority, label)
    }

    void validateModelItemsForDataModel(Collection<ModelItem> modelItems, DataModel dataModel, String associationName, ModelItemService service) {
        modelItems.eachWithIndex { et, i ->
            service.validate(et)
            if (et.hasErrors()) {
                et.errors.fieldErrors.each { err ->
                    dataModel.errors.rejectValue("${associationName}[${i}].${err.field}", err.code, err.arguments, err.defaultMessage)
                }
            }
        }
        dataModel."${associationName}" = modelItems
    }

    void validateDataClassesForDataModel(Collection<DataClass> allDataClasses, DataModel dataModel) {
        if (!allDataClasses) return

        log.trace('{} dataclasses to validate', allDataClasses.count { !it.parentDataClass }, allDataClasses.size())

        dataModel.fullSortOfChildren(allDataClasses.findAll { !it.parentDataClass })

        allDataClasses.each {
            it.dataModel.skipValidation(true)
            it.skipValidation(true)
        }
        dataModel.dataTypes.each { it.skipValidation(true) }
        allDataClasses.eachWithIndex { dc, i ->
            long st = System.currentTimeMillis()
            dc.skipValidation(false)
            dataClassService.validate(dc)
            dc.skipValidation(true)
            long tt = System.currentTimeMillis() - st
            if (tt >= 1000) log.debug('{} validated in {}', dc.label, Utils.getTimeString(tt))
        }
        dataModel.dataClasses.addAll(allDataClasses)

        // To be able to register the errors in the DM we need to add the DCs back to the DM
        allDataClasses.eachWithIndex { dc, i ->
            if (dc.hasErrors()) {
                dc.errors.fieldErrors.each { err ->
                    dataModel.errors.rejectValue("dataClasses[${i}].${err.field}", err.code, err.arguments, err.defaultMessage)
                }
            }
        }
    }

    DataModel validate(DataModel dataModel) {
        log.debug('Validating DataModel {}', dataModel.label)
        long totalStart = System.currentTimeMillis()

        // Extract DCs to validate separately
        Collection<DataClass> dataClasses = []
        if (dataModel.dataClasses) {
            dataClasses.addAll dataModel.dataClasses
            dataModel.dataClasses.clear()
        }

        long st = System.currentTimeMillis()
        dataModel.validate()
        log.debug('DataModel base content validation took {}', Utils.timeTaken(st))

        st = System.currentTimeMillis()
        validateDataClassesForDataModel(dataClasses, dataModel)
        log.debug('DataModel dataClasses validation took {}', Utils.timeTaken(st))

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
        log.debug('Validated DataModel in {}', Utils.timeTaken(totalStart))
        dataModel
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
                if (!it.isDirty('multiFacetAwareItemId')) it.trackChanges()
                it.multiFacetAwareItemId = catalogueItem.getId()
            }
            SummaryMetadata.saveAll(catalogueItem.summaryMetadata)
        }
        catalogueItem
    }

    @Override
    DataModel checkFacetsAfterImportingCatalogueItem(DataModel catalogueItem) {
        super.checkFacetsAfterImportingCatalogueItem(catalogueItem)
        if (catalogueItem.summaryMetadata) {
            catalogueItem.summaryMetadata.each { sm ->
                sm.multiFacetAwareItemId = catalogueItem.id
                sm.createdBy = sm.createdBy ?: catalogueItem.createdBy
                sm.summaryMetadataReports.each { smr ->
                    smr.createdBy = catalogueItem.createdBy
                }
            }
        }
        catalogueItem
    }

    @Override
    DataModel saveModelWithContent(DataModel dataModel) {

        if (dataModel.dataTypes.any { it.id } || dataModel.dataClasses.any { it.id }) {
            throw new ApiInternalException('DMSXX', 'Cannot use saveModelWithContent method to save DataModel',
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

        saveContent(enumerationTypes, primitiveTypes, referenceTypes, modelDataTypes, dataClasses)
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

        saveContent(enumerationTypes, primitiveTypes, referenceTypes, modelDataTypes, dataClasses, dataElements)
        log.debug('Complete save of DataModel complete in {}', Utils.timeTaken(start))
        // Return the clean stored version of the datamodel, as we've messed with it so much this is much more stable
        get(dataModel.id)
    }

    @Override
    void updateCopiedCrossModelLinks(DataModel copiedDataModel, DataModel originalDataModel) {
        super.updateCopiedCrossModelLinks(copiedDataModel, originalDataModel)

        copiedDataModel.modelDataTypes.each {ModelDataType mdt ->

            ModelService modelService = modelServices.find {service ->
                service.handles(mdt.modelResourceDomainType)
            }

            if (!modelService) throw new ApiInternalException('DMSXX', "No domain service to handle repointing of modelResourceDomainType [${mdt.modelResourceDomainType}]")

            // The model pointed to originally
            Model originalModelResource = modelService.get(mdt.modelResourceId) as Model

            Path fullContextOriginalModelResourcePath = getFullPathForModel(originalModelResource)
            Path fullContextOriginalDataModelPath = getFullPathForModel(originalDataModel)

            // Is the original model resource in the same versioned folder as the original data model?
            PathNode originalModelResourceVersionedFolderPathNode = fullContextOriginalModelResourcePath.find { it.prefix == 'vf' }
            PathNode originalDataModelVersionedFolderPathNode = fullContextOriginalDataModelPath.find { it.prefix == 'vf' }


            if (originalModelResourceVersionedFolderPathNode && originalModelResourceVersionedFolderPathNode == originalDataModelVersionedFolderPathNode) {
                log.debug('Original model resource is inside the same context path as data model for modelDataType [{}]', mdt)

                // Construct a path from the prefix and label of the model pointed to originally, but with the branch name now used,
                // to get the copy of the model in the copied folder
                // Note: Using a method in PathService which does not check security on the securable resource owning the model
                PathNode originalModelPathNode = fullContextOriginalModelResourcePath.last()
                CreatorAware replacementModelResource =
                    pathService.findResourceByPath(Path.from(originalModelPathNode.prefix, originalModelPathNode.getFullIdentifier(copiedDataModel.branchName)))

                if (!replacementModelResource) {
                    throw new ApiInternalException('DMSXX', "Could not find branched model resource ${originalModelResource.label} in branch ${copiedDataModel.branchName}")
                }

                // Update the model data type to point to the copy of the model
                mdt.modelResourceId = replacementModelResource.id
            }
        }

        save(copiedDataModel, flush: false, validate: false)
    }

    void saveContent(Collection<EnumerationType> enumerationTypes,
                     Collection<PrimitiveType> primitiveTypes,
                     Collection<ReferenceType> referenceTypes,
                     Collection<ModelDataType> modelDataTypes,
                     Collection<DataClass> dataClasses,
                     Set<DataElement> dataElements = [] as HashSet) {

        sessionFactory.currentSession.clear()
        long start = System.currentTimeMillis()
        log.trace('Disabling validation on contents')
        enumerationTypes.each { dt ->
            dt.skipValidation(true)
            dt.enumerationValues.each { ev -> ev.skipValidation(true) }
            dt.dataElements?.clear()
        }
        primitiveTypes.each {
            it.skipValidation(true)
            it.dataElements?.clear()
        }
        referenceTypes.each {
            it.skipValidation(true)
            it.dataElements?.clear()
        }
        modelDataTypes.each {
            it.skipValidation(true)
            it.dataElements?.clear()
        }
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
    void deleteModelsAndContent(Set<UUID> idsToDelete) {

        GormUtils.disableDatabaseConstraints(sessionFactory as SessionFactoryImplementor)

        log.trace('Removing other ModelItems in {} DataModels', idsToDelete.size())
        modelItemServices.findAll {
            !(it.modelItemClass in [DataClass, DataElement, DataType, EnumerationType, ModelDataType, PrimitiveType,
                                    ReferenceType, EnumerationValue])
        }.each { modelItemService ->
            try {
                modelItemService.deleteAllByModelIds(idsToDelete)
            } catch (ApiNotYetImplementedException ignored) {
            }
        }

        log.trace('Removing DataClasses in {} DataModels', idsToDelete.size())
        dataClassService.deleteAllByModelIds(idsToDelete)

        log.trace('Removing DataTypes in {} DataModels', idsToDelete.size())
        dataTypeService.deleteAllByModelIds(idsToDelete)

        log.trace('Removing facets')
        deleteAllFacetsByMultiFacetAwareIds(idsToDelete.toList(), 'delete from datamodel.join_datamodel_to_facet where datamodel_id in :ids')

        log.trace('Content removed')
        sessionFactory.currentSession
            .createSQLQuery('DELETE FROM datamodel.data_model WHERE id IN :ids')
            .setParameter('ids', idsToDelete)
            .executeUpdate()

        log.trace('DataModels removed')

        sessionFactory.currentSession
            .createSQLQuery('DELETE FROM core.breadcrumb_tree WHERE domain_id IN :ids')
            .setParameter('ids', idsToDelete)
            .executeUpdate()

        log.trace('Breadcrumb trees removed')

        GormUtils.enableDatabaseConstraints(sessionFactory as SessionFactoryImplementor)
    }


    @Override
    List<DataModel> findAllReadableModels(List<UUID> constrainedIds, boolean includeDeleted) {
        DataModel.withReadable(DataModel.by(), constrainedIds, includeDeleted).list()
    }

    @Override
    void deleteAllFacetDataByMultiFacetAwareIds(List<UUID> catalogueItemIds) {
        super.deleteAllFacetDataByMultiFacetAwareIds(catalogueItemIds)
        summaryMetadataService.deleteAllByMultiFacetAwareItemIds(catalogueItemIds)
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
    List<DataModel> findAllByAuthorityAndLabel(Authority authority, String label) {
        DataModel.findAllByAuthorityAndLabel(authority, label)
    }

    @Override
    List<UUID> getAllModelIds() {
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

    @Override
    int countByAuthorityAndLabelAndBranchNameAndNotFinalised(Authority authority, String label, String branchName) {
        DataModel.countByAuthorityAndLabelAndBranchNameAndFinalised(authority, label, branchName, false)
    }

    @Override
    int countByAuthorityAndLabelAndVersion(Authority authority, String label, Version modelVersion) {
        DataModel.countByAuthorityAndLabelAndModelVersion(authority, label, modelVersion)
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
        Folder folder = proxyHandler.unwrapIfProxy(original.folder) as Folder
        copyModel(original, folder, copier, copyPermissions, label, Version.from('1'), original.branchName, throwErrors,
                  userSecurityPolicyManager, false)
    }

    @Override
    DataModel copyModel(DataModel original, Folder folderToCopyInto, User copier, boolean copyPermissions, String label, Version copyDocVersion,
                        String branchName, boolean throwErrors, UserSecurityPolicyManager userSecurityPolicyManager) {
        copyModel(original, folderToCopyInto, copier, copyPermissions, label, copyDocVersion, branchName, throwErrors,
                  userSecurityPolicyManager, true)
    }

    DataModel copyModel(DataModel original, Folder folderToCopyInto, User copier, boolean copyPermissions, String label, Version copyDocVersion,
                        String branchName, boolean throwErrors, UserSecurityPolicyManager userSecurityPolicyManager, boolean copySummaryMetadata) {
        long start = System.currentTimeMillis()
        log.debug('Creating a new copy of {} with branch name {}', original.label, branchName)
        DataModel copy = new DataModel(author: original.author, organisation: original.organisation, modelType: original.modelType, finalised: false,
                                       deleted: false, documentationVersion: copyDocVersion, folder: folderToCopyInto,
                                       authority: authorityService.defaultAuthority,
                                       branchName: branchName
        )

        copy = copyCatalogueItemInformation(original, copy, copier, userSecurityPolicyManager, copySummaryMetadata, null)
        copy.label = label

        if (copyPermissions) {
            if (throwErrors) {
                throw new ApiNotYetImplementedException('MSXX', 'Model permission copying')
            }
            log.warn('Permission copying is not yet implemented')
        }

        setCatalogueItemRefinesCatalogueItem(copy, original, copier)

        if (copy.validate()) {
            save(copy, validate: false)
            editService.createAndSaveEdit(EditTitle.COPY, copy.id, copy.domainType,
                                          "DataModel ${original.modelType}:${original.label} created as a copy of ${original.id}",
                                          copier
            )
        } else throw new ApiInvalidModelException('DMS01', 'Copied DataModel is invalid', copy.errors, messageSource)

        copy.trackChanges()

        List<DataType> dataTypes = DataType.byDataModelId(original.id).join('classifiers').list()
        List<DataClass> rootDataClasses = DataClass.byRootDataClassOfDataModelId(original.id).join('classifiers').list()
        CopyInformation dataClassCache = cacheFacetInformationForCopy(rootDataClasses.collect { it.id })
        CopyInformation dataTypeCache = cacheFacetInformationForCopy(dataTypes.collect { it.id })

        // Copy all the datatypes
        dataTypes.each { dt ->
            dataTypeService.copyDataType(copy, dt, copier, userSecurityPolicyManager, copySummaryMetadata, dataTypeCache)
        }

        // Copy all the dataclasses (this will also match up the reference types)
        rootDataClasses.each { dc ->
            dataClassService.copyDataClass(copy, dc, copier, userSecurityPolicyManager, null, copySummaryMetadata, dataClassCache)
        }
        log.debug('Copy of datamodel took {}', Utils.timeTaken(start))
        copy
    }

    @Override
    DataModel copyCatalogueItemInformation(DataModel original,
                                           DataModel copy,
                                           User copier,
                                           UserSecurityPolicyManager userSecurityPolicyManager,
                                           CopyInformation copyInformation = null) {
        copyCatalogueItemInformation(original, copy, copier, userSecurityPolicyManager, false, copyInformation)
    }

    DataModel copyCatalogueItemInformation(DataModel original,
                                           DataModel copy,
                                           User copier,
                                           UserSecurityPolicyManager userSecurityPolicyManager,
                                           boolean copySummaryMetadata,
                                           CopyInformation copyInformation) {
        copy = super.copyCatalogueItemInformation(original, copy, copier, userSecurityPolicyManager, copyInformation) as DataModel
        if (copySummaryMetadata) {
            copy = copySummaryMetadataFromOriginal(original, copy, copier, copyInformation)
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
    boolean isCatalogueItemImportedIntoCatalogueItem(CatalogueItem catalogueItem, DataModel owningDataModel) {
        if (!(catalogueItem instanceof DataClass)) return false
        owningDataModel.id && catalogueItem.model.id != owningDataModel.id
    }

    @Override
    boolean hasTreeTypeModelItems(DataModel dataModel, boolean fullTreeRender, boolean includeImportedDataClasses) {
        dataModel.dataClasses || (includeImportedDataClasses ? dataModel.importedDataClasses : false) || (dataModel.dataTypes && fullTreeRender)
    }

    @Override
    List<ModelItem> findAllTreeTypeModelItemsIn(DataModel catalogueItem, boolean fullTreeRender, boolean includeImportedDataClasses) {
        ((includeImportedDataClasses ? dataClassService.findAllWhereRootDataClassOfDataModelIdIncludingImported(catalogueItem.id)
                                     : dataClassService.findAllWhereRootDataClassOfDataModelId(catalogueItem.id)) +
         (fullTreeRender ? DataType.byDataModelId(catalogueItem.id).list() : []) as List<ModelItem>)
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
    List<DataModel> findAllByClassifier(Classifier classifier) {
        DataModel.byClassifierId(classifier.id).list() as List<DataModel>
    }

    @Override
    List<DataModel> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        findAllByClassifier(classifier)
            .findAll { userSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, it.id) } as List<DataModel>
    }

    @Override
    Class<DataModel> getModelClass() {
        DataModel
    }

    @Override
    Integer countByContainerId(UUID containerId) {
        DataModel.byFolderId(containerId).count()
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

    List<DataModel> findAllModelsByIdInList(List<UUID> ids, Map pagination) {
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

    @Override
    ModelImporterProviderService<DataModel, ? extends ModelImporterProviderServiceParameters> getJsonModelImporterProviderService() {
        dataModelJsonImporterService
    }

    @Override
    DataModelJsonExporterService getJsonModelExporterProviderService() {
        dataModelJsonExporterService
    }

    @Override
    void propagateContentsInformation(DataModel catalogueItem, DataModel previousVersionCatalogueItem) {

        previousVersionCatalogueItem.dataTypes.each { previousDataType ->
            DataType dataType = catalogueItem.dataTypes.find { it.label == previousDataType.label }
            if (dataType) {
                dataTypeService.propagateDataFromPreviousVersion(dataType, previousDataType)
            }
        }

        previousVersionCatalogueItem.childDataClasses.each { previousDataClass ->
            DataClass dataClass = catalogueItem.childDataClasses.find { it.label == previousDataClass.label }
            if (dataClass) {
                dataClassService.propagateDataFromPreviousVersion(dataClass, previousDataClass)
            }
        }

        previousVersionCatalogueItem.summaryMetadata.each { previousSummaryMetadata ->
            if (catalogueItem.summaryMetadata.any { it.label == previousSummaryMetadata.label }) return
            SummaryMetadata summaryMetadata = new SummaryMetadata(label: previousSummaryMetadata.label,
                                                                  description: previousSummaryMetadata.description,
                                                                  summaryMetadataType: previousSummaryMetadata.summaryMetadataType)

            previousSummaryMetadata.summaryMetadataReports.each { previousSummaryMetadataReport ->
                summaryMetadata.addToSummaryMetadataReports(reportDate: previousSummaryMetadataReport.reportDate,
                                                            reportValue: previousSummaryMetadataReport.reportValue,
                                                            createdBy: previousSummaryMetadataReport.createdBy
                )
            }
            catalogueItem.addToSummaryMetadata(summaryMetadata)
        }
    }

    @Override
    CatalogueItem processDeletionPatchOfFacet(MultiFacetItemAware multiFacetItemAware, Model targetModel, Path path) {
        CatalogueItem catalogueItem = super.processDeletionPatchOfFacet(multiFacetItemAware, targetModel, path)

        if (multiFacetItemAware.domainType == SummaryMetadata.simpleName) {
            (catalogueItem as SummaryMetadataAware).summaryMetadata.remove(multiFacetItemAware)
        }

        catalogueItem
    }

    @Override
    int getSortResultForFieldPatchPath(Path leftPath, Path rightPath) {
        PathNode leftLastNode = leftPath.last()
        PathNode rightLastNode = rightPath.last()
        // Merge datatypes then dataclasses then dataelements this makes sure any dataelements created already have datatypes in place
        if (leftLastNode.prefix == 'dt') {
            if (rightLastNode.prefix == 'dt') return 0
            if (rightLastNode.prefix in ['de', 'dc']) return -1
            return 0
        }
        if (leftLastNode.prefix == 'dc') {
            if (rightLastNode.prefix == 'dr') return 1
            if (rightLastNode.prefix == 'dc') return 0
            if (rightLastNode.prefix == 'de') return -1
            return 0
        }
        if (leftLastNode.prefix == 'de') {
            if (rightLastNode.prefix in ['dt', 'dc']) return 1
            if (rightLastNode.prefix == 'de') return 0
            return 0
        }
        0
    }

    @Override
    CopyInformation cacheFacetInformationForCopy(List<UUID> originalIds, CopyInformation copyInformation = null) {
        CopyInformation cachedInformation = super.cacheFacetInformationForCopy(originalIds, copyInformation)
        cacheSummaryMetadataInformationForCopy(originalIds, cachedInformation)
    }
}
