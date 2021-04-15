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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClassService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElementService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration.EnumerationValueService
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.DefaultDataTypeProvider
import uk.ac.ox.softeng.maurodatamapper.datamodel.rest.transport.DefaultDataType
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

@SuppressWarnings("ClashingTraitMethods")
@Slf4j
@Transactional
class DataTypeService extends ModelItemService<DataType> implements DefaultDataTypeProvider {

    DataElementService dataElementService
    DataClassService dataClassService
    PrimitiveTypeService primitiveTypeService
    ReferenceTypeService referenceTypeService
    EnumerationTypeService enumerationTypeService
    ModelDataTypeService modelDataTypeService
    SummaryMetadataService summaryMetadataService
    EnumerationValueService enumerationValueService

    @Override
    DataType get(Serializable id) {
        DataType.get(id)
    }

    @Override
    boolean handlesPathPrefix(String pathPrefix) {
        pathPrefix == "dt"
    }

    Long count() {
        DataType.count()
    }

    @Override
    List<DataType> list(Map args) {
        DataType.list(args)
    }

    @Override
    List<DataType> getAll(Collection<UUID> ids) {
        DataType.getAll(ids).findAll()
    }

    @Override
    void deleteAll(Collection<DataType> catalogueItems) {
        DataType.deleteAll(catalogueItems)
    }

    void delete(DataType dataType, boolean flush = false) {
        if (!dataType) return
        dataType.dataModel.removeFromDataTypes(dataType)
        dataType.breadcrumbTree.removeFromParent()

        List<DataElement> dataElements = dataElementService.findAllByDataType(dataType)
        dataElements.each { dataElementService.delete(it) }

        switch (dataType.domainType) {
            case DataType.PRIMITIVE_DOMAIN_TYPE:
                primitiveTypeService.delete(dataType as PrimitiveType, flush)
                break
            case DataType.ENUMERATION_DOMAIN_TYPE:
                enumerationTypeService.delete(dataType as EnumerationType, flush)
                break
            case DataType.REFERENCE_DOMAIN_TYPE:
                referenceTypeService.delete(dataType as ReferenceType, flush)
                break
            case DataType.MODEL_DATA_DOMAIN_TYPE:
                modelDataTypeService.delete(dataType as ModelDataType, flush)
        }
    }

    void deleteAllByModelId(UUID dataModelId) {
        //Assume DataElements gone by this point

        List<UUID> dataTypeIds = DataType.byDataModelId(dataModelId).id().list() as List<UUID>

        if (dataTypeIds) {
            enumerationValueService.deleteAllByModelId(dataModelId)

            log.trace('Removing facets for {} DataTypes', dataTypeIds.size())

            deleteAllFacetsByCatalogueItemIds(dataTypeIds, 'delete from datamodel.join_datatype_to_facet where datatype_id in :ids')

            log.trace('Removing {} DataTypes', dataTypeIds.size())

            sessionFactory.currentSession
                .createSQLQuery('DELETE FROM datamodel.data_type WHERE data_model_id = :id')
                .setParameter('id', dataModelId)
                .executeUpdate()

            log.trace('DataTypes removed')
        }
    }

    void removeSummaryMetadataFromCatalogueItem(UUID catalogueItemId, SummaryMetadata summaryMetadata) {
        removeFacetFromDomain(catalogueItemId, summaryMetadata.id, 'summaryMetadata')
    }

    @Override
    void deleteAllFacetDataByCatalogueItemIds(List<UUID> catalogueItemIds) {
        super.deleteAllFacetDataByCatalogueItemIds(catalogueItemIds)
        summaryMetadataService.deleteAllByCatalogueItemIds(catalogueItemIds)
    }

    @Override
    boolean hasTreeTypeModelItems(DataType catalogueItem, boolean fullTreeRender, boolean includeImported) {
        fullTreeRender && catalogueItem instanceof EnumerationType ? catalogueItem.enumerationValues : false
    }

    @Override
    List<ModelItem> findAllTreeTypeModelItemsIn(DataType catalogueItem, boolean fullTreeRender, boolean includeImported) {
        fullTreeRender && catalogueItem instanceof EnumerationType ? catalogueItem.enumerationValues : []
    }

    @Override
    DataType findByIdJoinClassifiers(UUID id) {
        DataType.findById(id, [fetch: [classifiers: 'join']])
    }

    @Override
    void removeAllFromClassifier(Classifier classifier) {
        DataType.byClassifierId(DataType, classifier.id).list().each {
            it.removeFromClassifiers(classifier)
        }
    }

    @Override
    List<DataType> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        DataType.byClassifierId(DataType, classifier.id).list().findAll {
            userSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, it.model.id)
        }
    }

    @Override
    Class<DataType> getModelItemClass() {
        DataType
    }

    @Override
    Boolean shouldPerformSearchForTreeTypeCatalogueItems(String domainType) {
        domainType == DataType.simpleName
    }

    @Override
    List<DataType> findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                              String searchTerm, String domainType) {
        List<UUID> readableIds = userSecurityPolicyManager.listReadableSecuredResourceIds(DataModel)
        if (!readableIds) return []

        List<DataType> results = []
        if (shouldPerformSearchForTreeTypeCatalogueItems(domainType)) {
            log.debug('Performing lucene label search')
            long start = System.currentTimeMillis()
            results = DataType.luceneLabelSearch(DataType, searchTerm, readableIds.toList()).results
            log.debug("Search took: ${Utils.getTimeString(System.currentTimeMillis() - start)}. Found ${results.size()}")
        }
        results
    }

    @Override
    List<DefaultDataType> getDefaultListOfDataTypes() {
        [
            new PrimitiveType(label: 'Text', description: 'A piece of text'),
            new PrimitiveType(label: 'Number', description: 'A whole number'),
            new PrimitiveType(label: 'Decimal', description: 'A decimal number'),
            new PrimitiveType(label: 'Date', description: 'A date'),
            new PrimitiveType(label: 'DateTime', description: 'A date with a timestamp'),
            new PrimitiveType(label: 'Timestamp', description: 'A timestamp'),
            new PrimitiveType(label: 'Boolean', description: 'A true or false value'),
            new PrimitiveType(label: 'Duration', description: 'A time period in arbitrary units')
        ].collect { new DefaultDataType(it) }
    }

    @Override
    String getDisplayName() {
        'Basic Default DataTypes'
    }

    @Override
    String getVersion() {
        '1.0.0'
    }

    void delete(Serializable id) {
        DataType dataType = get(id)
        if (dataType) delete(dataType)
    }

    def saveAll(Collection<DataType> dataTypes) {
        List<Classifier> classifiers = dataTypes.collectMany { it.classifiers ?: [] } as List<Classifier>
        if (classifiers) {
            log.trace('Saving {} classifiers')
            classifierService.saveAll(classifiers)
        }

        Collection<DataType> alreadySaved = dataTypes.findAll { it.ident() && it.isDirty() }
        Collection<DataType> notSaved = dataTypes.findAll { !it.ident() }

        if (alreadySaved) {
            log.trace('Straight saving {} already saved DataTypes ', alreadySaved.size())
            DataType.saveAll(alreadySaved)
        }

        if (notSaved) {
            log.trace('Batch saving {} new DataTypes in batches of {}', notSaved.size(), DataType.BATCH_SIZE)
            List batch = []
            int count = 0

            notSaved.each { dt ->
                dt.dataElements?.clear()
                batch += dt
                count++
                if (count % DataType.BATCH_SIZE == 0) {
                    batchSave(batch)
                    batch.clear()
                }

            }
            batchSave(batch)
            batch.clear()
        }
    }

    void batchSave(List<DataType> dataTypes) {
        long start = System.currentTimeMillis()
        log.trace('Performing batch save of {} DataTypes', dataTypes.size())

        DataType.saveAll(dataTypes)
        dataTypes.each {dt ->
            updateFacetsAfterInsertingCatalogueItem(dt)
        }

        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()

        log.trace('Batch save took {}', Utils.getTimeString(System.currentTimeMillis() - start))
    }

    DataType validate(DataType dataType) {
        if (dataType.domainType == ReferenceType.simpleName) return referenceTypeService.validate(dataType as ReferenceType)
        dataType.validate()
        dataType
    }

    @Override
    DataType updateFacetsAfterInsertingCatalogueItem(DataType dataType) {
        if (dataType.instanceOf(EnumerationType)) {
            enumerationTypeService.updateFacetsAfterInsertingCatalogueItem(dataType as EnumerationType)
        } else {
            super.updateFacetsAfterInsertingCatalogueItem(dataType) as DataType
        }
        if (dataType.summaryMetadata) {
            dataType.summaryMetadata.each {
                if (!it.isDirty('catalogueItemId')) it.trackChanges()
                it.catalogueItemId = dataType.getId()
            }
            SummaryMetadata.saveAll(dataType.summaryMetadata)
        }
        dataType
    }

    @Override
    DataType checkFacetsAfterImportingCatalogueItem(DataType dataType) {
        if (dataType.instanceOf(EnumerationType)) {
            return enumerationTypeService.checkFacetsAfterImportingCatalogueItem(dataType as EnumerationType)
        } else {
            return super.checkFacetsAfterImportingCatalogueItem(dataType) as DataType
        }
    }

    def findByDataModelIdAndId(Serializable dataModelId, Serializable id) {
        DataType.byDataModelIdAndId(dataModelId, id).find()
    }

    void checkImportedDataTypeAssociations(User importingUser, DataModel dataModel, DataType dataType) {
        dataModel.addToDataTypes(dataType)
        dataType.buildPath()
        dataType.createdBy = importingUser.emailAddress
        if (dataType.instanceOf(EnumerationType)) {
            EnumerationType enumerationType = (dataType as EnumerationType)
            enumerationType.fullSortOfChildren(enumerationType.enumerationValues)
            enumerationType.enumerationValues.each { ev ->
                ev.createdBy = importingUser.emailAddress
                ev.buildPath()
            }
        }
        checkFacetsAfterImportingCatalogueItem(dataType)
    }

    private void setCreatedBy(User creator, DataType dataType) {
        throw new ApiNotYetImplementedException('DTSXX', 'DataType setting created by')
    }

    private def findAllByDataModelId(Serializable dataModelId, Map paginate = [:], boolean includeImported = false) {
        DataType.withFilter(DataType.byDataModelId(dataModelId, includeImported), paginate).list(paginate)
    }   

    private def findAllByDataModelIdAndLabelIlikeOrDescriptionIlike(Serializable dataModelId, String searchTerm, Map paginate = [:], boolean includeImported = false) {
        DataType.byDataModelIdAndLabelIlikeOrDescriptionIlike(dataModelId, searchTerm, includeImported).list(paginate)
    }

    void matchReferenceClasses(DataModel dataModel, Collection<ReferenceType> referenceTypes, Collection<Map> bindingMaps = []) {
        referenceTypes.sort {it.label}.each {rdt ->
            Map dataTypeBindingMap = bindingMaps.find {it.label == rdt.label} ?: [:]
            Map refClassBindingMap = dataTypeBindingMap.referenceClass ?: [:]
            matchReferenceClass(dataModel, rdt, refClassBindingMap)
        }
    }

    private void matchReferenceClass(DataModel dataModel, ReferenceType referenceType, Map bindingMap = [:]) {

        if (bindingMap.dataClassPath) {
            String dataClassPath = bindingMap.dataClassPath
            List<String> dataClassPaths = dataClassPath.split(/\|/)?.toList() ?: []
            DataClass dataClass = dataClassService.findDataClassByPath(dataModel, dataClassPaths)
            if (dataClass) dataClass.addToReferenceTypes(referenceType)
        } else if (referenceType.referenceClass) {
            DataClass dataClass = dataClassService.findSameLabelTree(dataModel, referenceType.referenceClass)
            if (dataClass) dataClass.addToReferenceTypes(referenceType)
            else {
                log.
                    trace('No referenceClass could be found to match label tree for {}, attempting no label tree', referenceType.referenceClass.label)
                def possibles = dataModel.dataClasses.findAll { it.label == referenceType.referenceClass.label }
                if (possibles.size() == 1) {
                    log.trace('Single possible referenceClass found, safely using')
                    possibles.first().addToReferenceTypes(referenceType)
                } else if (possibles.size() > 1) {
                    log.warn('Multiple possibilities found for referenceClass, using first found however this could be wrong')
                    possibles.first().addToReferenceTypes(referenceType)
                } else {
                    log.error('No referenceClass {} could be found for referenceType {}', referenceType.referenceClass.label, referenceType.label)
                    referenceType.referenceClass = null
                }
            }
        } else {
            log.trace('Making best guess for matching reference class as no path nor bound class')
            DataClass dataClass = dataModel.dataClasses.find { it.label == bindingMap.referenceClass.label }
            if (dataClass) dataClass.addToReferenceTypes(referenceType)
        }
    }

    DataType copy(Model copiedDataModel, DataType original, UserSecurityPolicyManager userSecurityPolicyManager) {
        copyDataType(copiedDataModel as DataModel, original, userSecurityPolicyManager.user, userSecurityPolicyManager)
    }

    DataType copyDataType(DataModel copiedDataModel, DataType original, User copier, UserSecurityPolicyManager userSecurityPolicyManager,
                          boolean copySummaryMetadata = false) {

        DataType copy

        String domainType = original.domainType
        switch (domainType) {
            case DataType.PRIMITIVE_DOMAIN_TYPE:
                copy = new PrimitiveType(units: original.units)
                break
            case DataType.ENUMERATION_DOMAIN_TYPE:
                copy = new EnumerationType()
                original.enumerationValues.each { ev ->
                    copy.addToEnumerationValues(key: ev.key, value: ev.value, category: ev.category)
                }
                break
            case DataType.REFERENCE_DOMAIN_TYPE:
                copy = new ReferenceType()
                // Merge dataclasses in after they've all been copied
                break
            case DataType.MODEL_DATA_DOMAIN_TYPE:
                copy = new ModelDataType(modelResourceId: original.modelResourceId, modelResourceDomainType: original.modelResourceDomainType)
                break
            default:
                throw new ApiInternalException('DTSXX', 'DataType domain type is unknown and therefore cannot be copied')
        }

        copy = copyCatalogueItemInformation(original, copy, copier, userSecurityPolicyManager, copySummaryMetadata)
        setCatalogueItemRefinesCatalogueItem(copy, original, copier)

        copiedDataModel.addToDataTypes(copy)

        copy
    }

    @Override
    DataType copyCatalogueItemInformation(DataType original,
                                          DataType copy,
                                          User copier,
                                          UserSecurityPolicyManager userSecurityPolicyManager,
                                          boolean copySummaryMetadata = false) {
        copy = super.copyCatalogueItemInformation(original, copy, copier, userSecurityPolicyManager)
        if (copySummaryMetadata) {
            summaryMetadataService.findAllByCatalogueItemId(original.id).each {
                copy.addToSummaryMetadata(label: it.label, summaryMetadataType: it.summaryMetadataType, createdBy: copier.emailAddress)
            }
        }
        copy
    }

    DataModel addDefaultListOfDataTypesToDataModel(DataModel dataModel, List<DefaultDataType> defaultDataTypes) {
        defaultListOfDataTypes.each {
            DataType dataType
            switch (it.domainType) {
                case PrimitiveType.simpleName:
                    dataType = new PrimitiveType(units: it.units)
                    break
                case EnumerationType.simpleName:
                    dataType = new EnumerationType(enumerationValues: it.enumerationValues)
                    break
                default:
                    throw new ApiInternalException('DTSXX', "Unknown DataType [${it.domainType}] used for default datatype")
            }
            dataType.createdBy = dataModel.createdBy
            dataType.label = it.label
            dataType.description = it.description
            dataModel.addToDataTypes(dataType)
        }
        dataModel
    }

    private def <T extends DataType> T mergeDataTypes(List<T> dataTypes) {
        mergeDataTypes(dataTypes.first(), dataTypes)
    }

    private def <T extends DataType> T mergeDataTypes(T keep, List<T> dataTypes) {
        for (int i = 1; i < dataTypes.size(); i++) {
            mergeDataTypes(keep, dataTypes[i])
            delete(dataTypes[i])
        }
        keep
    }

    private void mergeDataTypes(DataType keep, DataType replace) {
        replace.dataElements?.each { de ->
            keep.addToDataElements(de)
        }
        List<Metadata> mds = []
        mds += replace.metadata ?: []
        mds.findAll { !keep.findMetadataByNamespaceAndKey(it.namespace, it.key) }.each { md ->
            replace.removeFromMetadata(md)
            keep.addToMetadata(md.namespace, md.key, md.value, md.createdBy)
        }
    }

    DataType findDataType(DataModel dataModel, String label) {
        dataModel.dataTypes.find { it.label == label.trim() }
    }

    /*
     * Find a DataType which is labeled with label and whose parent is parentCatalogueItem. The parentCatalogueItem
     * can be a DataModel.
     * @param parentCatalogueItem The DataModel which is the parent of the DataType being sought
     * @param label The label of the DataType being sought
     */

    @Override
    DataType findByParentAndLabel(CatalogueItem parentCatalogueItem, String label) {
        findDataType(parentCatalogueItem, label)
    }

    @Override
    List<DataType> findAllByMetadataNamespaceAndKey(String namespace, String key, Map pagination) {
        DataType.byMetadataNamespaceAndKey(namespace, key).list(pagination)
    }

    @Override
    List<DataType> findAllByMetadataNamespace(String namespace, Map pagination) {
        DataType.byMetadataNamespace(namespace).list(pagination)
    }

}