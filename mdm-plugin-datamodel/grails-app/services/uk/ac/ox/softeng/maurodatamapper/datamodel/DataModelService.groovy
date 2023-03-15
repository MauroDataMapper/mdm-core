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
package uk.ac.ox.softeng.maurodatamapper.datamodel

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
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.provider.dataloader.DataLoaderProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.ModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.merge.FieldPatchData
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
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration.EnumerationValueService
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.DefaultDataTypeProvider
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter.DataModelExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter.DataModelJsonExporterService
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.DataModelJsonImporterService
import uk.ac.ox.softeng.maurodatamapper.datamodel.rest.transport.Intersects
import uk.ac.ox.softeng.maurodatamapper.datamodel.rest.transport.SourceTargetIntersects
import uk.ac.ox.softeng.maurodatamapper.datamodel.rest.transport.Subset
import uk.ac.ox.softeng.maurodatamapper.datamodel.similarity.DataElementSimilarityResult
import uk.ac.ox.softeng.maurodatamapper.datamodel.traits.service.SummaryMetadataAwareService
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.path.PathNode
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils
import uk.ac.ox.softeng.maurodatamapper.util.Utils
import uk.ac.ox.softeng.maurodatamapper.version.Version

import grails.gorm.DetachedCriteria
import grails.gorm.transactions.Transactional
import grails.util.Environment
import groovy.util.logging.Slf4j
import org.grails.datastore.mapping.validation.ValidationErrors
import org.hibernate.engine.spi.SessionFactoryImplementor
import org.hibernate.search.mapper.orm.Search
import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy
import org.hibernate.search.mapper.orm.session.SearchSession
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.Errors

import static uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType.ENUMERATION_DOMAIN_TYPE
import static uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType.MODEL_DATA_DOMAIN_TYPE
import static uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType.PRIMITIVE_DOMAIN_TYPE
import static uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType.REFERENCE_DOMAIN_TYPE
import static uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType.count
import static uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType.get

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
    EnumerationValueService enumerationValueService

    @Autowired
    Set<DefaultDataTypeProvider> defaultDataTypeProviders

    @Autowired(required = false)
    Set<ModelService> modelServices

    @Autowired(required = false)
    Set<DataModelExporterProviderService> exporterProviderServices

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
        DataModel.list().collect {unwrapIfProxy(it)}
    }

    @Override
    String getUrlResourceName() {
        'dataModels'
    }

    Long count() {
        DataModel.count()
    }

    int countByAuthorityAndLabel(Authority authority, String label) {
        DataModel.countByAuthorityAndLabel(authority, label)
    }

    DataModel validate(DataModel dataModel) {
        log.debug('Validating DataModel {}', dataModel.label)
        long totalStart = System.currentTimeMillis()

        if (dataModel.id) {
            validateFullExistingModel(dataModel)
        } else {
            dataModel.validate()
        }

        if (dataModel.hasErrors()) {
            Errors existingErrors = dataModel.errors
            Errors cleanedErrors = new ValidationErrors(dataModel)
            existingErrors.fieldErrors.each {fe ->
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

    /**
     *  If DM has an id and we want to validate the whole DM then there are massive speedups available due to checks which are in place for validating
     *  components when they're added to a DM. Therefore use this optimised faster validation for a DM which has an id
     * @param dataModel
     * @return
     */
    DataModel validateFullExistingModel(DataModel dataModel) {
        // Extract DCs to validate separately
        Collection<DataClass> dataClasses = []
        if (dataModel.dataClasses) {
            dataClasses.addAll dataModel.dataClasses
            dataModel.dataClasses.clear()
        }

        long st = System.currentTimeMillis()

        dataModel.fullSortOfChildren(dataModel.getDataTypes())

        dataModel.validate()
        log.debug('DataModel base content validation took {}', Utils.timeTaken(st))

        st = System.currentTimeMillis()
        validateDataClassesForDataModel(dataClasses, dataModel)
        log.debug('DataModel dataClasses validation took {}', Utils.timeTaken(st))

        dataModel
    }

    void validateDataClassesForDataModel(Collection<DataClass> allDataClasses, DataModel dataModel) {
        if (!allDataClasses) return

        log.trace('{} dataclasses to validate', allDataClasses.count {!it.parentDataClass}, allDataClasses.size())

        dataModel.fullSortOfChildren(allDataClasses.findAll {!it.parentDataClass})

        allDataClasses.each {
            it.dataModel.skipValidation(true)
            it.skipValidation(true)
        }
        dataModel.dataTypes.each {it.skipValidation(true)}
        allDataClasses.eachWithIndex {dc, i ->
            long st = System.currentTimeMillis()
            dc.skipValidation(false)
            dataClassService.validate(dc)
            dc.skipValidation(true)
            long tt = System.currentTimeMillis() - st
            if (tt >= 1000) log.debug('{} validated in {}', dc.label, Utils.getTimeString(tt))
        }
        dataModel.dataClasses.addAll(allDataClasses)

        // To be able to register the errors in the DM we need to add the DCs back to the DM
        allDataClasses.eachWithIndex {dc, i ->
            if (dc.hasErrors()) {
                dc.errors.fieldErrors.each {err ->
                    dataModel.errors.rejectValue("dataClasses[${i}].${err.field}", err.code, err.arguments, err.defaultMessage)
                }
            }
        }
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
            long start = System.currentTimeMillis()
            log.debug('Deleting DataModel')
            deleteModelAndContent(dm)
            log.debug('DataModel deleted. Took {}', Utils.timeTaken(start))
            if (securityPolicyManagerService) {
                securityPolicyManagerService.removeSecurityForSecurableResource(dm, null)
            }
        } else delete(dm)
    }

    @Override
    DataModel save(DataModel dataModel) {
        log.debug('Saving {}({}) without batching', dataModel.label, dataModel.ident())
        save(failOnError: true, validate: false, flush: false, dataModel)
    }

    @Override
    DataModel checkFacetsAfterImportingCatalogueItem(DataModel catalogueItem) {
        super.checkFacetsAfterImportingCatalogueItem(catalogueItem)
        if (catalogueItem.summaryMetadata) {
            catalogueItem.summaryMetadata.each {sm ->
                sm.multiFacetAwareItemId = catalogueItem.id
                sm.createdBy = sm.createdBy ?: catalogueItem.createdBy
                sm.summaryMetadataReports.each {smr ->
                    smr.createdBy = catalogueItem.createdBy
                }
            }
        }
        catalogueItem
    }

    @Override
    DataModel saveModelWithContent(DataModel dataModel) {
        saveModelWithContent(dataModel, 1000)
    }

    DataModel saveModelWithContent(DataModel dataModel, Integer modelItemBatchSize) {

        if (dataModel.dataTypes.any {it.id} || dataModel.dataClasses.any {it.id}) {
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
            // The flush below will try to save the importingDC side and as the objects havent been saved yet the flush will fail
            dataClasses.each {dc ->
                if (!dc.id && dc.importedDataClasses) {
                    dc.importedDataClasses.each {idc ->
                        idc.removeFromImportingDataClasses(dc)
                    }
                }
                if (!dc.id && dc.importedDataElements) {
                    dc.importedDataElements.each {ide ->
                        ide.removeFromImportingDataClasses(dc)
                    }
                }
            }
            dataModel.dataClasses.clear()
        }

        if (dataModel.breadcrumbTree.children) {
            dataModel.breadcrumbTree.disableValidation()
        }

        // Set this HS session to be async mode, this is faster and as we dont need to read the indexes its perfectly safe
        SearchSession searchSession = Search.session(sessionFactory.currentSession)
        searchSession.automaticIndexingSynchronizationStrategy(AutomaticIndexingSynchronizationStrategy.async())

        save(failOnError: true, validate: false, flush: false, ignoreBreadcrumbs: true, dataModel)

        sessionFactory.currentSession.flush()

        saveContent(modelItemBatchSize, enumerationTypes, primitiveTypes, referenceTypes, modelDataTypes, dataClasses)

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
            enumerationTypes.addAll dataModel.enumerationTypes.findAll {!it.id}
            primitiveTypes.addAll dataModel.primitiveTypes.findAll {!it.id}
            modelDataTypes.addAll dataModel.modelDataTypes.findAll {!it.id}
            referenceTypes.addAll dataModel.referenceTypes.findAll {!it.id}
        }

        if (dataModel.dataClasses) {
            dataClasses.addAll dataModel.dataClasses.findAll {!it.id}
            dataElements.addAll dataModel.dataClasses.collectMany {it.dataElements.findAll {!it.id}}
        }

        dataModel.breadcrumbTree.save(failOnError: true, validate: false, flush: false)

        saveContent(ModelItemService.BATCH_SIZE, enumerationTypes, primitiveTypes, referenceTypes, modelDataTypes, dataClasses, dataElements)
        log.debug('Complete save of DataModel complete in {}', Utils.timeTaken(start))
        // Return the clean stored version of the datamodel, as we've messed with it so much this is much more stable
        get(dataModel.id)
    }

    void saveContent(Integer modelItemBatchSize,
                     Collection<EnumerationType> enumerationTypes,
                     Collection<PrimitiveType> primitiveTypes,
                     Collection<ReferenceType> referenceTypes,
                     Collection<ModelDataType> modelDataTypes,
                     Collection<DataClass> dataClasses,
                     Set<DataElement> dataElements = [] as HashSet) {

        sessionFactory.currentSession.clear()
        long start = System.currentTimeMillis()
        log.trace('Disabling validation on contents')
        enumerationTypes.each {dt ->
            dt.skipValidation(true)
            dt.enumerationValues.each {ev -> ev.skipValidation(true)}
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
        dataClasses.each {dc ->
            dc.skipValidation(true)
            dc.dataElements.each {de -> de.skipValidation(true)}
        }
        referenceTypes.each {it.skipValidation(true)}

        // During testing its very important that we dont disable constraints otherwise we may miss an invalid model,
        // The disabling is done to provide a speed up during saving which is not necessary during test
        if (Environment.current != Environment.TEST) {
            log.debug('Disabling database constraints')
            GormUtils.disableDatabaseConstraints(sessionFactory as SessionFactoryImplementor)
        }

        long subStart = System.currentTimeMillis()
        dataTypeService.saveAll(enumerationTypes, modelItemBatchSize)
        dataTypeService.saveAll(primitiveTypes, modelItemBatchSize)
        dataTypeService.saveAll(modelDataTypes, modelItemBatchSize)
        int totalDts = enumerationTypes.size() + primitiveTypes.size() + modelDataTypes.size()
        if (totalDts) log.debug('Saved {} dataTypes in {}', totalDts, Utils.timeTaken(subStart))

        subStart = System.currentTimeMillis()
        dataElements.addAll(dataClassService.saveAll(dataClasses) as Collection<DataElement>)
        if (dataClasses) log.debug('Saved {} dataClasses in {}', dataClasses.size(), Utils.timeTaken(subStart))

        subStart = System.currentTimeMillis()
        dataTypeService.saveAll(referenceTypes as Collection<DataType>, modelItemBatchSize)
        if (referenceTypes) log.debug('Saved {} reference datatypes in {}', referenceTypes.size(), Utils.timeTaken(subStart))

        subStart = System.currentTimeMillis()
        dataElementService.saveAll(dataElements, modelItemBatchSize)
        if (dataElements) log.debug('Saved {} dataElements in {}', dataElements.size(), Utils.timeTaken(subStart))


        if (Environment.current != Environment.TEST) {
            log.debug('Enabling database constraints')
            GormUtils.enableDatabaseConstraints(sessionFactory as SessionFactoryImplementor)
        }

        log.trace('Content save of DataModel complete in {}', Utils.timeTaken(start))
    }

    @Override
    void deleteModelsAndContent(Set<UUID> idsToDelete) {

        GormUtils.disableDatabaseConstraints(sessionFactory as SessionFactoryImplementor)

        log.trace('Removing other ModelItems in {} DataModels', idsToDelete.size())
        modelItemServices.findAll {
            !(it.domainClass in [DataClass, DataElement, DataType, EnumerationType, ModelDataType, PrimitiveType,
                                 ReferenceType, EnumerationValue])
        }.each {modelItemService ->
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
        Utils.executeInBatches(idsToDelete as List, {ids ->
            sessionFactory.currentSession
                .createSQLQuery('DELETE FROM datamodel.data_model WHERE id IN :ids')
                .setParameter('ids', ids)
                .executeUpdate()
        })

        log.trace('DataModels removed')

        log.trace('Removing BreadcrumbTrees')
        breadcrumbTreeService.deleteAllByDomainIds(idsToDelete)

        GormUtils.enableDatabaseConstraints(sessionFactory as SessionFactoryImplementor)
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
            DefaultDataTypeProvider provider = defaultDataTypeProviders.find {it.name == defaultDataTypeProvider}
            if (provider) {
                log.debug("Adding ${provider.displayName} default DataTypes")
                return dataTypeService.addDefaultListOfDataTypesToDataModel(resource, provider.defaultListOfDataTypes)
            }
        }
        resource
    }

    void deleteAllUnusedDataTypes(DataModel dataModel) {
        log.debug('Cleaning DataModel {} of DataTypes', dataModel.label)
        dataModel.dataTypes.findAll {!it.dataElements}.each {
            dataTypeService.delete(it)
        }
    }

    void deleteAllUnusedDataClasses(DataModel dataModel) {
        log.debug('Cleaning DataModel {} of DataClasses', dataModel.label)
        dataModel.dataClasses.findAll {dataClassService.isUnusedDataClass(it)}.each {
            dataClassService.delete(it)
        }
    }

    void checkImportedDataModelAssociations(User importingUser, DataModel dataModel, Map bindingMap = [:]) {
        dataModel.createdBy = importingUser.emailAddress
        checkFacetsAfterImportingCatalogueItem(dataModel)

        if (dataModel.dataClasses) {
            dataModel.fullSortOfChildren(dataModel.childDataClasses)
            Collection<DataClass> dataClasses = dataModel.childDataClasses
            dataClasses.each {dc ->
                dataClassService.checkImportedDataClassAssociations(importingUser, dataModel, dc, !bindingMap.isEmpty())
            }
        }

        if (bindingMap && dataModel.dataTypes) {
            Set<ReferenceType> referenceTypes = dataModel.dataTypes.findAll {it.instanceOf(ReferenceType)} as Set<ReferenceType>
            if (referenceTypes) {
                log.debug('Matching {} ReferenceType referenceClasses', referenceTypes.size())
                dataTypeService.matchReferenceClasses(dataModel, referenceTypes,
                                                      bindingMap.dataTypes.findAll {it.domainType == REFERENCE_DOMAIN_TYPE})
            }
        }

        // Make sure we have all the DTs inside the DM first as some will have been imported from the DEs
        if (dataModel.dataTypes) {
            dataModel.fullSortOfChildren(dataModel.dataTypes)
            dataModel.dataTypes.each {dt ->
                dataTypeService.checkImportedDataTypeAssociations(importingUser, dataModel, dt)
            }
        }

        log.debug('DataModel associations checked')
    }

    DataModel ensureAllEnumerationTypesHaveValues(DataModel dataModel) {
        dataModel.dataTypes.findAll {it.instanceOf(EnumerationType) && !(it as EnumerationType).getEnumerationValues()}.each {EnumerationType et ->
            et.addToEnumerationValues(key: '-', value: '-')
        }
        dataModel
    }

    List<DataElement> getAllDataElementsOfDataModel(DataModel dataModel) {
        List<DataElement> allElements = []
        dataModel.dataClasses.each {allElements += it.dataElements ?: []}
        allElements
    }

    List<DataElement> findAllDataElementsWithNames(DataModel dataModel, Set<String> dataElementNames, boolean caseInsensitive) {
        if (!dataElementNames) return []
        getAllDataElementsOfDataModel(dataModel).findAll {
            caseInsensitive ?
            it.label.toLowerCase() in dataElementNames.collect {it.toLowerCase()} :
            it.label in dataElementNames
        }
    }

    Set<EnumerationType> findAllEnumerationTypeByNames(DataModel dataModel, Set<String> enumerationTypeNames, boolean caseInsensitive) {
        if (!enumerationTypeNames) return []
        dataModel.dataTypes.findAll {it.instanceOf(EnumerationType)}.findAll {
            caseInsensitive ?
            it.label.toLowerCase() in enumerationTypeNames.collect {it.toLowerCase()} :
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
        CopyInformation dataClassCache = cacheFacetInformationForCopy(rootDataClasses.collect {it.id}, new CopyInformation(copyIndex: true))
        CopyInformation dataTypeCache = cacheFacetInformationForCopy(dataTypes.collect {it.id}, new CopyInformation(copyIndex: true))

        // Copy all the datatypes
        dataTypes.sort().each {dt ->
            dataTypeService.copyDataType(copy, dt, copier, userSecurityPolicyManager, copySummaryMetadata, dataTypeCache)
        }

        // Copy all the dataclasses (this will also match up the reference types)
        rootDataClasses.sort().each {dc ->
            dataClassService.copyDataClass(copy, dc, copier, userSecurityPolicyManager, null, copySummaryMetadata, dataClassCache)
        }

        List<DataType> importedDataTypes = dataTypeService.findAllByImportingDataModelId(original.id)
        copyImportedElements(copy, original, importedDataTypes, 'importedDataTypes', copier)

        List<DataClass> importedDataClasses = dataClassService.findAllByImportingDataModelId(original.id)
        copyImportedElements(copy, original, importedDataClasses, 'importedDataClasses', copier)

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

    void copyImportedElements(DataModel copiedDataModel, DataModel originalDataModel, Collection<ModelItem> importedElements, String field, User copier) {
        if (!importedElements) return

        // Add all imported elements to the copy
        importedElements.each {ie ->
            copiedDataModel.addTo(field, ie)
        }

        // Find all MD which is attached to the imported elements with a key containing the original id
        // This will get all MD which pertains to the element wrt the importing object
        List<Metadata> importRelevantMetadata = metadataService.findAllByMultiFacetAwareItemIdInListAndNamespaceLike(importedElements*.id, "%${originalDataModel.id}%")
        importRelevantMetadata.each {md ->
            String newNs = md.namespace.replace(originalDataModel.id.toString(), copiedDataModel.id.toString())
            String value = md.value
            if (md.value == originalDataModel.path.toString()) value = copiedDataModel.path
            else if (md.value == originalDataModel.id.toString()) value = copiedDataModel.id.toString()
            Metadata copiedMetadata = new Metadata(namespace: newNs, key: md.key, value: value, createdBy: copier.emailAddress)
            importedElements.find {it.id == md.multiFacetAwareItemId}.addToMetadata(copiedMetadata)
            metadataService.save(copiedMetadata)

        }
    }

    List<DataElementSimilarityResult> suggestLinksBetweenModels(DataModel dataModel, DataModel otherDataModel, int maxResults) {
        dataModel.getAllDataElements().collect {de ->
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
        criteria.list().collectEntries {[it[0], it[1]]}
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
            log.debug('Performing hs label search')
            long start = System.currentTimeMillis()
            results = DataModel.labelHibernateSearch(DataModel, searchTerm, readableIds.toList(), []).results
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
            .findAll {userSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, it.id)} as List<DataModel>
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
        models.collect {it.id}.findAll {dataClassService.countByDataModelId(it)}
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
    boolean useParentIdForSearching(UUID parentId) {
        folderService.exists(parentId)
    }

    @Override
    void updateModelItemPathsAfterFinalisationOfModel(DataModel model) {
        updateModelItemPathsAfterFinalisationOfModel model, 'datamodel', 'data_class', 'data_element', 'data_type', 'enumeration_value'
    }

    @Override
    void propagateContentsInformation(DataModel catalogueItem, DataModel previousVersionCatalogueItem) {

        previousVersionCatalogueItem.dataTypes.each {previousDataType ->
            DataType dataType = catalogueItem.dataTypes.find {it.label == previousDataType.label}
            if (dataType) {
                dataTypeService.propagateDataFromPreviousVersion(dataType, previousDataType)
            }
        }

        previousVersionCatalogueItem.childDataClasses.each {previousDataClass ->
            DataClass dataClass = catalogueItem.childDataClasses.find {it.label == previousDataClass.label}
            if (dataClass) {
                dataClassService.propagateDataFromPreviousVersion(dataClass, previousDataClass)
            }
        }

        previousVersionCatalogueItem.summaryMetadata.each {previousSummaryMetadata ->
            if (catalogueItem.summaryMetadata.any {it.label == previousSummaryMetadata.label}) return
            SummaryMetadata summaryMetadata = new SummaryMetadata(label: previousSummaryMetadata.label,
                                                                  description: previousSummaryMetadata.description,
                                                                  summaryMetadataType: previousSummaryMetadata.summaryMetadataType)

            previousSummaryMetadata.summaryMetadataReports.each {previousSummaryMetadataReport ->
                summaryMetadata.addToSummaryMetadataReports(reportDate: previousSummaryMetadataReport.reportDate,
                                                            reportValue: previousSummaryMetadataReport.reportValue,
                                                            createdBy: previousSummaryMetadataReport.createdBy
                )
            }
            catalogueItem.addToSummaryMetadata(summaryMetadata)
        }
    }

    @Override
    void customProcessPatchIntoModelForUnfoundPath(FieldPatchData fieldPatchData, DataModel targetModel) {

        Path fullPath = fieldPatchData.path
        String internalPath = fullPath.toString().find(/^.+\.\[(.+?)\]\..+$/) {it[1]}

        if (internalPath) {
            String newPath = fullPath.toString().replace("[$internalPath]", 'REPLACE_ME')
            fullPath = Path.from(newPath)
        }

        if (fullPath.last().prefix == 'md') {
            log.debug('Custom metadata for internal path')
            Path relativePath = fullPath.childPath
            Path importedElementPath = Path.from(internalPath)
            MdmDomain importingElement = pathService.findResourceByPathFromRootResource(targetModel, relativePath.parent)
            PathNode mdNode = relativePath.last()
            mdNode.identifier = mdNode.identifier.replace('REPLACE_ME', importingElement.id.toString())
            Path metadataPath = importedElementPath.resolve(Path.from(mdNode))
            Metadata metadata = pathService.findResourceByPath(metadataPath) as Metadata

            switch (fieldPatchData.type) {
                case 'creation':
                    MdmDomain importedElement = pathService.findResourceByPath(importedElementPath)
                    MdmDomain originalImportingElement = pathService.findResourceByPath(fullPath.parent)
                    log.debug('Creating Metadata into Model at [{}]', importedElementPath)
                    mdNode.identifier = mdNode.identifier.replace(importingElement.id.toString(), originalImportingElement.id.toString())
                    metadataPath = importedElementPath.resolve(Path.from(mdNode))
                    metadata = pathService.findResourceByPath(metadataPath) as Metadata
                    String namespace = metadata.namespace.replace(originalImportingElement.id.toString(), importingElement.id.toString())
                    Metadata copy = new Metadata(namespace: namespace, key: metadata.key, value: metadata.value, createdBy: metadata.createdBy)
                    importedElement.addToMetadata(copy)

                    if (!copy.validate())
                        throw new ApiInvalidModelException('MS01', 'Copied Facet is invalid', copy.errors, messageSource)

                    metadataService.save(copy, flush: false, validate: false)
                    break
                case 'deletion':
                    log.debug('Deleting Metadata from path [{}]', metadataPath)
                    metadataService.delete(metadata)
                    break
                case 'modification':
                    String fieldName = fieldPatchData.fieldName
                    log.debug('Modifying [{}] in [{}]', fieldName, metadataPath)
                    metadata."${fieldName}" = fieldPatchData.sourceValue
                    if (!metadata.validate())
                        throw new ApiInvalidModelException('MS01', 'Modified domain is invalid', metadata.errors, messageSource)
                    metadataService.save(metadata, flush: false, validate: false)
                    break
            }
        } else {
            super.customProcessPatchIntoModelForUnfoundPath(fieldPatchData, targetModel)
        }
    }

    @Override
    void processCreationPatchOfModelItem(ModelItem modelItemToCopy, Model targetModel, Path pathToCopy,
                                         UserSecurityPolicyManager userSecurityPolicyManager, boolean flush = false) {

        // If the path to copy endswith the model item's path being copied then the model item cannot be inside the target model as the model item would already exist
        // and the path to copy must also therefore include a fully resolved model path after a modelitem path
        // This is indicitive of an imported object
        if (modelItemToCopy.path.first() != targetModel.path.first() &&
            pathToCopy.endsWith(modelItemToCopy.path, pathToCopy.first().modelIdentifier)) {
            Path parentToImportIntoPath = pathToCopy.remove(modelItemToCopy.path, pathToCopy.first().modelIdentifier)

            if (parentToImportIntoPath.isEmpty() || parentToImportIntoPath.matches(targetModel.path)) {
                log.debug('Importing [{}] into [{}]', modelItemToCopy.path, targetModel.path)
                switch (modelItemToCopy.domainType) {
                    case DataClass.simpleName:
                        (targetModel as DataModel).addToImportedDataClasses(modelItemToCopy as DataClass)
                        break

                    case REFERENCE_DOMAIN_TYPE: case PRIMITIVE_DOMAIN_TYPE: case ENUMERATION_DOMAIN_TYPE: case MODEL_DATA_DOMAIN_TYPE:
                        (targetModel as DataModel).addToImportedDataTypes(modelItemToCopy as DataType)
                        break
                }
            } else {
                log.debug('Importing [{}] into [{}]', modelItemToCopy.path, parentToImportIntoPath)
                DataClass parentToImportInto = pathService.findResourceByPathFromRootResource(targetModel, parentToImportIntoPath) as DataClass
                switch (modelItemToCopy.domainType) {
                    case DataClass.simpleName:
                        parentToImportInto.addToImportedDataClasses(modelItemToCopy as DataClass)
                        break
                    case DataElement.simpleName:
                        parentToImportInto.addToImportedDataElements(modelItemToCopy as DataElement)
                        break
                }
            }
            return
        }
        super.processCreationPatchOfModelItem(modelItemToCopy, targetModel, pathToCopy, userSecurityPolicyManager, flush)
    }

    @Override
    void processDeletionPatchOfModelItem(ModelItem modelItem, Model targetModel, Path pathToDelete) {
        // If the path to copy endswith the model item's path being copied then the model item cannot be inside the target model as the model item would already exist
        // and the path to copy must also therefore include a fully resolved model path after a modelitem path
        // This is indicitive of an imported object
        if (modelItem.path.first() != targetModel.path.first() &&
            pathToDelete.endsWith(modelItem.path, pathToDelete.first().modelIdentifier)) {
            Path parentToRemoveImportFromPath = pathToDelete.remove(modelItem.path, pathToDelete.first().modelIdentifier)
            if (parentToRemoveImportFromPath.isEmpty() || parentToRemoveImportFromPath.matches(targetModel.path)) {
                log.debug('Removing import [{}] from [{}]', modelItem.path, targetModel.path)
                switch (modelItem.domainType) {
                    case DataClass.simpleName:
                        (targetModel as DataModel).removeFromImportedDataClasses(modelItem as DataClass)
                        break

                    case REFERENCE_DOMAIN_TYPE: case PRIMITIVE_DOMAIN_TYPE: case ENUMERATION_DOMAIN_TYPE: case MODEL_DATA_DOMAIN_TYPE:
                        (targetModel as DataModel).removeFromImportedDataTypes(modelItem as DataType)
                        break
                }
            } else {
                log.debug('Removing import [{}] from [{}]', modelItem.path, parentToRemoveImportFromPath)
                DataClass parentToImportInto = pathService.findResourceByPathFromRootResource(targetModel, parentToRemoveImportFromPath) as DataClass
                switch (modelItem.domainType) {
                    case DataClass.simpleName:
                        parentToImportInto.removeFromImportedDataClasses(modelItem as DataClass)
                        break
                    case DataElement.simpleName:
                        parentToImportInto.removeFromImportedDataElements(modelItem as DataElement)
                        break
                }
            }
            return
        }
        super.processDeletionPatchOfModelItem(modelItem, targetModel, pathToDelete)
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

    /**
     * Subset a DataModel by creating and deleting Data Elements in targetDataModel based on those found in sourceDataModel.
     * The purpose of the targetModel is to record a subset of Data Elements which have been chosen from the sourceDataModel.
     * @param sourceDataModel
     * @param targetDataModel
     * @param subsetData
     * @param user
     * @param userSecurityPolicyManager
     * @return
     */
    def subset(DataModel sourceDataModel, DataModel targetDataModel, Subset subsetData, UserSecurityPolicyManager userSecurityPolicyManager) {
        subsetData.additions.each {dataElementId ->
            subsetAddition(sourceDataModel, targetDataModel, Utils.toUuid(dataElementId), userSecurityPolicyManager)
        }

        subsetData.deletions.each {dataElementId ->
            subsetDeletion(sourceDataModel, targetDataModel, Utils.toUuid(dataElementId))
        }

        if (subsetData.deletions.size() > 0) {
            // Remove any Data Classes which are no longer used
            deleteUnSubsettedDataClasses(targetDataModel)
        }
    }

    /**
     * Remove from a Data Model any Data Classes which contain no Data Classes, Data Elements or Reference Types
     * @param dataModel
     * @return
     */
    private deleteUnSubsettedDataClasses(DataModel dataModel) {
        Collection<DataClass> dataClassesToDelete = dataModel.dataClasses.findAll {
            !it.dataClasses && !it.dataElements && !it.referenceTypes
        }

        if (dataClassesToDelete.size() > 0) {
            dataClassesToDelete.each {
                dataClassService.delete(it)
            }

            // By removing a Data Class we may have emptied a parent Data Class, so recurse
            deleteUnSubsettedDataClasses(dataModel)
        }
    }

    /**
     * Make a copy of a Data Element from the sourceDataModel in the targetDataModel.
     * 1. Check that the Data Element with ID dataElementId exists in the sourceDataModel
     * 2. Get the path of this Data Element in the sourceDataModel
     * 3. Find if there is already a Data Element matching the same path in targetDataModel
     * 4. If not then create one, creating any necessary Data Class hierarchy
     * @param sourceDataModel
     * @param targetDataModel
     * @param dataElementId
     * @param user
     * @param userSecurityPolicyManager
     * @return
     */
    private subsetAddition(DataModel sourceDataModel, DataModel targetDataModel, UUID dataElementId,
                           UserSecurityPolicyManager userSecurityPolicyManager) {
        DataElement dataElementInSource = getDataElementInModel(sourceDataModel, dataElementId)

        Path pathInSource = dataElementInSource.getPath()

        DataElement dataElementInTarget = pathService.findResourceByPathFromRootResource(targetDataModel, pathInSource.getChildPath())

        if (!dataElementInTarget) {
            // The path of the DataElement that we want to see in the target.
            // It should look like dc:label of a data class|optionally more dc nodes|de:label of data element
            Path pathInTarget = pathInSource.getChildPath()

            if (pathInTarget.size() < 2)
                throw new ApiInternalException('DMSXX', "Path ${pathInTarget} was shorter than expected")

            // Now iterate forwards through the path, making sure that a domain exists for each node of the path
            // Assumption is that the first node of pathInTarget is a dc.
            dataClassService.subset(sourceDataModel, targetDataModel, dataElementInSource, pathInTarget, userSecurityPolicyManager)
        }
    }

    /**
     * Delete a Data Element identified by dataElementId in sourceDataModel from the targetDataModel
     * 1. Check that the Data Element with ID dataElementId exists in the sourceDataModel
     * 2. Get the path of this Data Element in the sourceDataModel
     * 3. Find if there is a Data Element matching the same path in targetDataModel
     * 4. If yes then delete that Data Element from targetDataModel
     * @param sourceDataModel
     * @param targetDataModel
     * @param dataElementId
     * @return
     */
    private subsetDeletion(DataModel sourceDataModel, DataModel targetDataModel, UUID dataElementId) {
        DataElement dataElementInSource = getDataElementInModel(sourceDataModel, dataElementId)

        Path pathInSource = dataElementInSource.getPath()

        DataElement dataElementInTarget = pathService.findResourceByPathFromRootResource(targetDataModel, pathInSource.getChildPath())

        if (dataElementInTarget) {
            dataElementService.delete(dataElementInTarget.id)
        }
    }

    /**
     * Get a Data Element by ID and verify it belongs to the expected Data Model.
     * @param dataModel
     * @param dataElementId
     * @return Data Element
     */
    private DataElement getDataElementInModel(DataModel dataModel, UUID dataElementId) {
        DataElement dataElementInModel = dataElementService.get(dataElementId)
        if (!dataElementInModel)
            throw new ApiInternalException('DMSXX', "Data Element ${dataElementId} not found")

        if (dataElementInModel.modelId != dataModel.id)
            throw new ApiInternalException('DMSXX', "Data Element ${dataElementId} does not belong to specified model")

        dataElementInModel
    }

    /**
     * Return the Data Element intersection between source and target Data Models. i.e. a collection of Data Elements
     * in sourceDataModel which are also present (i.e. match by path) in targetDataModel
     * @param sourceDataModel
     * @param targetDataModel
     * @return
     */
    Collection<UUID> intersects(DataModel sourceDataModel, DataModel targetDataModel) {
        Collection<UUID> intersection = []

        dataElementService.findAllByDataModelId(sourceDataModel.id).each {
            Path pathInSource = it.getPath()

            DataElement dataElementInTarget = pathService.findResourceByPathFromRootResource(targetDataModel, pathInSource.getChildPath())

            if (dataElementInTarget) {
                intersection << it.id
            }
        }

        intersection
    }

    /**
     * Return the Data Element intersection between source and many target Data Models, checking only the list
     * of Data Elements provided i.e. for each target model listed in intersects.targetDataModelIds, of the Data Elements listed
     * in intersects.dataElementsIds from the sourceDataModel, which are also present (i.e. match by path) in the targetDataModel
     * @param currentUserSecurityPolicyManager - needed to check read access on the target data models
     * @param sourceDataModel
     * @param intersects
     * @return
     */
    Collection<UUID> intersectsMany(UserSecurityPolicyManager currentUserSecurityPolicyManager, DataModel sourceDataModel, Intersects intersects) {
        Collection<SourceTargetIntersects> manySourceTargetIntersects = []

        // Get all the data elements once. Using findByDataModelIdAndId means we are checking that data element
        // does belong to the source data model, which is the secured item
        Collection<DataElement> sourceDataElements = []
        intersects.dataElementIds.each {dataElementId ->
            DataElement sourceDataElement = dataElementService.findByDataModelIdAndId(sourceDataModel.id, dataElementId)

            if (sourceDataElement) {
                sourceDataElements << sourceDataElement
            }
        }

        intersects.targetDataModelIds.each {targetDataModelId ->
            // The interceptor did not check read access on the target, so check it now
            // Note that an invalid UUID can cause userCanReadSecuredResourceId to return true, so later we
            // check if (targetDataModel)
            boolean canReadTarget = currentUserSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, Utils.toUuid(targetDataModelId))
            if (canReadTarget) {
                DataModel targetDataModel = get(targetDataModelId)

                if (targetDataModel) {
                    SourceTargetIntersects sourceTargetIntersects = new SourceTargetIntersects()
                    sourceTargetIntersects.sourceDataModelId = sourceDataModel.id
                    sourceTargetIntersects.targetDataModelId = targetDataModelId

                    sourceDataElements.each {sourceDataElement ->
                        Path pathInSource = sourceDataElement.getPath()

                        DataElement dataElementInTarget = pathService.findResourceByPathFromRootResource(targetDataModel, pathInSource.getChildPath())

                        if (dataElementInTarget) {
                            sourceTargetIntersects.intersects << sourceDataElement.id
                        }
                    }
                    manySourceTargetIntersects << sourceTargetIntersects
                }
            }
        }

        manySourceTargetIntersects
    }

    @Override
    CachedDiffable<DataModel> loadEntireModelIntoDiffCache(UUID modelId) {
        long start = System.currentTimeMillis()
        DataModel loadedModel = get(modelId)
        log.debug('Loading entire model [{}] into memory', loadedModel.path)

        // Load direct content

        log.trace('Loading DataType')
        List<DataType> dataTypes = dataTypeService.findAllByDataModelId(loadedModel.id)
        List<DataType> importedDataTypes = dataTypeService.findAllByImportingDataModelId(loadedModel.id)
        List<Metadata> importedDataTypesMetadata = Metadata.extractMetadataForDiff(modelId, importedDataTypes)

        log.trace('Loading EnumerationValue')
        List<EnumerationValue> enumerationValues = enumerationValueService.findAllByDataModelId(modelId)
        Map<UUID, List<EnumerationValue>> enumerationValuesMap = enumerationValues.groupBy {it.enumerationType.id}

        log.trace('Loading DataClass')
        List<DataClass> dataClasses = dataClassService.findAllByDataModelId(loadedModel.id)
        List<DataClass> importedDataClasses = dataClassService.findAllByImportingDataModelId(loadedModel.id)
        List<Metadata> importedDataClassesMetadata = Metadata.extractMetadataForDiff(modelId, importedDataClasses)
        Map<UUID, List<DataClass>> dataClassesMap = dataClasses.groupBy {it.parentDataClass?.id}
        List<DataClass> importedIntoDataClassDataClasses = dataClassService.findAllByImportingDataClassIds(dataClasses.collect {it.id})
        Map<UUID, List<DataClass>> importedDataClassesMap = [:]
        importedIntoDataClassDataClasses.each {ide ->
            ide.importingDataClasses.each {idc ->
                importedDataClassesMap.compute(idc.id, {key, dataClassList ->
                    if (!dataClassList) return [ide]
                    dataClassList.add(ide)
                    dataClassList
                })
            }
        }


        log.trace('Loading DataElement')
        List<DataElement> dataElements = dataElementService.findAllByDataModelId(loadedModel.id)
        Map<UUID, List<DataElement>> dataElementsMap = dataElements.groupBy {it.dataClass.id}
        List<DataElement> importedDataElements = dataElementService.findAllByImportingDataClassIds(dataClasses.collect {it.id})
        Map<UUID, List<DataElement>> importedDataElementsMap = [:]
        importedDataElements.each {ide ->
            ide.importingDataClasses.each {idc ->
                importedDataElementsMap.compute(idc.id, {key, dataElementList ->
                    if (!dataElementList) return [ide]
                    dataElementList.add(ide)
                    dataElementList
                })
            }
        }

        log.trace('Loading Facets')
        List<UUID> allIds = Utils.gatherIds(Collections.singleton(modelId),
                                            dataClasses.collect {it.id},
                                            dataElements.collect {it.id},
                                            dataTypes.collect {it.id},
                                            enumerationValues.collect {it.id})
        Map<String, Map<UUID, List<Diffable>>> facetData = loadAllDiffableFacetsIntoMemoryByIds(allIds)

        DiffCache diffCache = createCatalogueItemDiffCache(null, loadedModel, facetData)
        createDataTypeDiffCaches(diffCache, dataTypes, enumerationValuesMap, facetData)
        createDataClassDiffCaches(diffCache, dataClassesMap, dataElementsMap, facetData, null,
                                  importedDataClassesMap, importedDataElementsMap)
        diffCache.addField('importedDataTypes', importedDataTypes)
        diffCache.addField('importedDataClasses', importedDataClasses)
        diffCache.addField('importedDataTypesMetadata', importedDataTypesMetadata)
        diffCache.addField('importedDataClassesMetadata', importedDataClassesMetadata)

        log.debug('Model loaded into memory, took {}', Utils.timeTaken(start))
        new CachedDiffable(loadedModel, diffCache)
    }

    private void createDataClassDiffCaches(DiffCache diffCache, Map<UUID, List<DataClass>> dataClassesMap,
                                           Map<UUID, List<DataElement>> dataElementsMap,
                                           Map<String, Map<UUID, List<Diffable>>> facetData, UUID dataClassId,
                                           Map<UUID, List<DataClass>> importedDataClasses, Map<UUID, List<DataElement>> importedDataElements) {

        List<DataClass> dataClasses = dataClassesMap[dataClassId]
        diffCache.addField('dataClasses', dataClasses)

        // Allow for the root addition where the dataClassId is null so we're adding to the DM diffCache
        if (dataClassId) {
            addFacetDataToDiffCache(diffCache, facetData, dataClassId)
            createCatalogueItemDiffCaches(diffCache, 'dataElements', dataElementsMap[dataClassId], facetData)
            diffCache.addField('importedDataClasses', importedDataClasses[dataClassId] ?: [])
            diffCache.addField('importedDataElements', importedDataElements[dataClassId] ?: [])
            List<Metadata> importedDataClassesMetadata = Metadata.extractMetadataForDiff(dataClassId, importedDataClasses[dataClassId])
            List<Metadata> importedDataElementsMetadata = Metadata.extractMetadataForDiff(dataClassId, importedDataElements[dataClassId])
            diffCache.addField('importedDataClassesMetadata', importedDataClassesMetadata)
            diffCache.addField('importedDataElementsMetadata', importedDataElementsMetadata)
        }

        dataClasses.each {dc ->
            DiffCache dcDiffCache = createCatalogueItemDiffCache(diffCache, dc, facetData)
            createDataClassDiffCaches(dcDiffCache, dataClassesMap, dataElementsMap, facetData, dc.id, importedDataClasses, importedDataElements)
        }
    }

    private void createDataTypeDiffCaches(DiffCache diffCache, List<DataType> dataTypes, Map<UUID, List<EnumerationValue>> enumerationValuesMap,
                                          Map<String, Map<UUID, List<Diffable>>> facetData) {
        diffCache.addField('dataTypes', dataTypes)
        dataTypes.each {dt ->
            DiffCache dtDiffCache = createCatalogueItemDiffCache(diffCache, dt, facetData)
            if (dt.instanceOf(EnumerationType)) {
                createCatalogueItemDiffCaches(dtDiffCache, 'enumerationValues', enumerationValuesMap[dt.id], facetData)
            }
        }
    }

    boolean validateImportAddition(DataModel instance, ModelItem importingItem) {
        if (importingItem.model.id == instance.id) {
            instance.errors.reject('invalid.imported.modelitem.same.datamodel',
                                   [importingItem.class.simpleName, importingItem.id].toArray(),
                                   '{0} [{1}] to be imported belongs to the DataModel already')
        }
        if (!importingItem.model.finalised && !areModelsInsideSameVersionedFolder(instance, importingItem.model)) {
            instance.errors.reject('invalid.imported.modelitem.model.not.finalised',
                                   [importingItem.class.simpleName, importingItem.id].toArray(),
                                   '{0} [{1}] to be imported does not belong to a finalised DataModel or reside inside the same VersionedFolder')
        }
        !instance.hasErrors()
    }

    boolean validateImportRemoval(DataModel instance, ModelItem importingItem) {
        if (importingItem.model.id == instance.id) {
            instance.errors.reject('invalid.imported.deletion.modelitem.same.datamodel',
                                   [importingItem.class.simpleName, importingItem.id].toArray(),
                                   '{0} [{1}] belongs to the DataModel and cannot be removed as an import')
        }
        !instance.hasErrors()
    }

    @Override
    void updateCopiedCrossModelLinks(DataModel copiedDataModel, DataModel originalDataModel) {
        super.updateCopiedCrossModelLinks(copiedDataModel, originalDataModel)

        updateCopiedCrossModelDataTypeLinks(copiedDataModel, originalDataModel)
        updateCopiedCrossModelImportedLinks(copiedDataModel, originalDataModel)

        save(copiedDataModel, flush: false, validate: false)
    }

    void updateCopiedCrossModelDataTypeLinks(DataModel copiedDataModel, DataModel originalDataModel) {
        if (!copiedDataModel.modelDataTypes) return
        log.debug('Updating all ModelDataTypes inside copied DataModel')
        copiedDataModel.modelDataTypes.each {ModelDataType mdt ->

            ModelService modelService = modelServices.find {service ->
                service.handles(mdt.modelResourceDomainType)
            }

            if (!modelService) throw new ApiInternalException('DMSXX',
                                                              "No domain service to handle repointing of modelResourceDomainType " +
                                                              "[${mdt.modelResourceDomainType}]")

            // The model pointed to originally
            Model originalModelResource = modelService.get(mdt.modelResourceId) as Model

            Path fullContextOriginalModelResourcePath = getFullPathForModel(originalModelResource)
            Path fullContextOriginalDataModelPath = getFullPathForModel(originalDataModel)

            // Is the original model resource in the same versioned folder as the original data model?
            PathNode originalModelResourceVersionedFolderPathNode = fullContextOriginalModelResourcePath.find {it.prefix == 'vf'}
            PathNode originalDataModelVersionedFolderPathNode = fullContextOriginalDataModelPath.find {it.prefix == 'vf'}


            if (originalModelResourceVersionedFolderPathNode && originalModelResourceVersionedFolderPathNode ==
                originalDataModelVersionedFolderPathNode) {
                log.debug('Original model resource is inside the same context path as data model for modelDataType [{}]', mdt.path)

                // Construct a path from the prefix and label of the model pointed to originally, but with the branch name now used,
                // to get the copy of the model in the copied folder
                // Note: Using a method in PathService which does not check security on the securable resource owning the model
                PathNode originalModelPathNode = fullContextOriginalModelResourcePath.last()
                MdmDomain replacementModelResource =
                    pathService.findResourceByPath(
                        Path.from(originalModelPathNode.prefix, originalModelPathNode.getFullIdentifier(copiedDataModel.branchName)))

                if (!replacementModelResource) {
                    throw new ApiInternalException('DMSXX',
                                                   "Could not find branched model resource ${originalModelResource.label} in branch " +
                                                   "${copiedDataModel.branchName}")
                }

                // Update the model data type to point to the copy of the model
                mdt.modelResourceId = replacementModelResource.id
            }
        }
    }

    void updateCopiedCrossModelImportedLinks(DataModel copiedDataModel, DataModel originalDataModel) {
        log.debug('Updating all imported elements inside copied DataModel')
        Path copiedDataModelPath = getFullPathForModel(copiedDataModel)
        Path originalDataModelPath = getFullPathForModel(originalDataModel)

        List<DataType> importedDataTypes = dataTypeService.findAllByImportingDataModelId(copiedDataModel.id)
        List<DataClass> importedDataClasses = dataClassService.findAllByImportingDataModelId(copiedDataModel.id)

        log.debug('Updating imported DTs')
        importedDataTypes.each {mi ->
            updateCopiedImportedModelItemLink(copiedDataModel, copiedDataModel, mi, copiedDataModelPath, originalDataModelPath, 'importedDataTypes')
        }

        log.debug('Updating imported DCs')
        importedDataClasses.each {mi ->
            updateCopiedImportedModelItemLink(copiedDataModel, copiedDataModel, mi, copiedDataModelPath, originalDataModelPath, 'importedDataClasses')
        }

        copiedDataModel.dataClasses.each {dc ->
            importedDataClasses = dataClassService.findAllByImportingDataClassId(dc.id)
            List<DataElement> importedDataElements = dataElementService.findAllByImportingDataClassId(dc.id)

            log.debug('Updating imported DCs inside DC {}', dc.path)
            importedDataClasses.each {mi ->
                updateCopiedImportedModelItemLink(copiedDataModel, dc, mi, copiedDataModelPath, originalDataModelPath, 'importedDataClasses')
            }
            log.debug('Updating imported DEs inside DC {}', dc.path)
            importedDataElements.each {mi ->
                updateCopiedImportedModelItemLink(copiedDataModel, dc, mi, copiedDataModelPath, originalDataModelPath, 'importedDataElements')
            }
        }
    }

    void updateCopiedImportedModelItemLink(DataModel copiedDataModel,
                                           CatalogueItem importingCatalogueItem, ModelItem importedModelItem,
                                           Path copiedDataModelPath, Path originalDataModelPath,
                                           String importCollectionName) {
        // Get the model path of the imported MI
        Path fullContextDataModelPath = getFullPathForModel(importedModelItem.model)
        // Need to check if the CS is inside the same VF as the terminology
        PathNode dataModelVersionedFolderPathNode = fullContextDataModelPath.find {it.prefix == 'vf'}
        if (dataModelVersionedFolderPathNode && originalDataModelPath.any {it == dataModelVersionedFolderPathNode}) {
            log.debug('Original DataModel is inside the same context path as DataModel for ModelItem [{}]', importedModelItem.path)
            ModelItem branchedModelItem = pathService.findResourceByPathFromRootResource(copiedDataModel, importedModelItem.path,
                                                                                         copiedDataModelPath.last().modelIdentifier)
            if (branchedModelItem) {
                importingCatalogueItem.removeFrom(importCollectionName, importedModelItem)
                importingCatalogueItem.addTo(importCollectionName, branchedModelItem)

                // Tidy up metadata
                Set<Metadata> importedMiMetadata = importedModelItem.metadata.findAll {it.namespace.matches(/(.+\.)?${importingCatalogueItem.id}(\..+)?/)}
                if (importedMiMetadata) {
                    importedMiMetadata.each {md ->
                        metadataService.delete(md)
                        importedModelItem.removeFrom('metadata', md)
                        branchedModelItem.addToMetadata(new Metadata(namespace: md.namespace, key: md.key, value: md.value, createdBy: md.createdBy))
                    }
                }


            } else {
                log.error('Branched ModelItem not found [{}]', importedModelItem.path.getPathString(copiedDataModelPath.last().modelIdentifier))
            }
        }
    }
}
