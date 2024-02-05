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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.CopyInformation
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.databinding.converters.DataTypeValueConverter
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClassService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElementService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration.EnumerationValueService
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.DefaultDataTypeProvider
import uk.ac.ox.softeng.maurodatamapper.datamodel.rest.transport.DefaultDataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.traits.service.SummaryMetadataAwareService
import uk.ac.ox.softeng.maurodatamapper.gorm.HQLPagedResultList
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

import static org.grails.orm.hibernate.cfg.GrailsHibernateUtil.ARGUMENT_SORT

@SuppressWarnings('ClashingTraitMethods')
@Slf4j
@Transactional
class DataTypeService extends ModelItemService<DataType> implements DefaultDataTypeProvider, SummaryMetadataAwareService {

    DataElementService dataElementService
    DataClassService dataClassService
    PrimitiveTypeService primitiveTypeService
    ReferenceTypeService referenceTypeService
    EnumerationTypeService enumerationTypeService
    ModelDataTypeService modelDataTypeService
    SummaryMetadataService summaryMetadataService
    EnumerationValueService enumerationValueService
    DataModelService dataModelService

    @Override
    DataType get(Serializable id) {
        DataType.get(id)
    }

    @Override
    boolean handlesPathPrefix(String pathPrefix) {
        // Have to override as the DataType class is abstract and can therefore not be instantiated
        pathPrefix == 'dt'
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
        dataElements.each {dataElementService.delete(it)}

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

    @Override
    void deleteAllByModelIds(Set<UUID> dataModelIds) {
        //Assume DataElements gone by this point

        List<UUID> dataTypeIds = DataType.byDataModelIdInList(dataModelIds).id().list() as List<UUID>

        if (dataTypeIds) {
            enumerationValueService.deleteAllByModelIds(dataModelIds)

            log.trace('Removing facets for {} DataTypes', dataTypeIds.size())

            deleteAllFacetsByMultiFacetAwareIds(dataTypeIds, 'delete from datamodel.join_datatype_to_facet where datatype_id in :ids')

            log.trace('Removing {} DataTypes', dataTypeIds.size())

            Utils.executeInBatches(dataModelIds as List, {ids ->
                sessionFactory.currentSession
                    .createSQLQuery('DELETE FROM datamodel.data_type WHERE data_model_id IN :ids')
                    .setParameter('ids', ids)
                    .executeUpdate()

            })

            log.trace('DataTypes removed')
        }
    }

    @Override
    void deleteAllFacetDataByMultiFacetAwareIds(List<UUID> catalogueItemIds) {
        super.deleteAllFacetDataByMultiFacetAwareIds(catalogueItemIds)
        summaryMetadataService.deleteAllByMultiFacetAwareItemIds(catalogueItemIds)
    }

    @Override
    boolean hasTreeTypeModelItems(DataType catalogueItem, boolean fullTreeRender, boolean includeImportedItems) {
        fullTreeRender && catalogueItem instanceof EnumerationType ? catalogueItem.enumerationValues : false
    }

    @Override
    List<ModelItem> findAllTreeTypeModelItemsIn(DataType catalogueItem, boolean fullTreeRender, boolean includeImportedItems) {
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
    List<DataType> findAllByClassifier(Classifier classifier) {
        DataType.byClassifierId(DataType, classifier.id).list()
    }

    @Override
    List<DataType> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        findAllByClassifier(classifier).findAll {
            userSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, it.model.id)
        }
    }

    @Override
    Boolean shouldPerformSearchForTreeTypeCatalogueItems(String domainType) {
        domainType == DataType.simpleName
    }

    @Override
    void propagateContentsInformation(DataType catalogueItem, DataType previousVersionCatalogueItem) {
        if (previousVersionCatalogueItem.instanceOf(EnumerationType)) {
            enumerationTypeService.propagateContentsInformation(catalogueItem as EnumerationType, previousVersionCatalogueItem as EnumerationType)
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
    List<DataType> findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                              String searchTerm, String domainType) {
        List<UUID> readableIds = userSecurityPolicyManager.listReadableSecuredResourceIds(DataModel)
        if (!readableIds) return []

        List<DataType> results = []
        if (shouldPerformSearchForTreeTypeCatalogueItems(domainType)) {
            log.debug('Performing hs label search')
            long start = System.currentTimeMillis()
            results =
                DataType
                    .labelHibernateSearch(DataType, searchTerm, readableIds.toList(), dataModelService.getAllReadablePaths(readableIds)).results
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
        ].collect {new DefaultDataType(it)}
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

    @Override
    DataType save(Map args, DataType domain) {
        // If RT make sure the reference class is saved but dont try and save the datatypes as part of this
        // otherwise we end up in a loop
        if (domain.instanceOf(ReferenceType)) {
            Map dcArgs = [flush: false, validate: false, saveDataTypes: false]
            dataClassService.save(dcArgs, (domain as ReferenceType).referenceClass)
        }
        super.save(args, domain)
    }

    DataType validate(DataType dataType) {
        dataType.validate()
        dataType
    }

    @Override
    void checkBreadcrumbTreeAfterSavingCatalogueItem(DataType dataType) {
        super.checkBreadcrumbTreeAfterSavingCatalogueItem(dataType)

        if (dataType.instanceOf(EnumerationType)) {
            dataType.enumerationValues.each {enumerationValue ->
                super.checkBreadcrumbTreeAfterSavingCatalogueItem(enumerationValue)
            }
        }
    }

    @Override
    void checkBreadcrumbTreeAfterSavingCatalogueItems(Collection<DataType> dataTypes) {
        super.checkBreadcrumbTreeAfterSavingCatalogueItems(dataTypes)
        super.checkBreadcrumbTreeAfterSavingCatalogueItems(dataTypes.findAll {it.instanceOf(EnumerationType)}.collectMany {it.enumerationValues})
    }

    @Override
    DataType checkFacetsAfterImportingCatalogueItem(DataType catalogueItem) {
        if (catalogueItem.summaryMetadata) {
            catalogueItem.summaryMetadata.each {sm ->
                sm.multiFacetAwareItemId = catalogueItem.id
                sm.createdBy = sm.createdBy ?: catalogueItem.createdBy
                sm.summaryMetadataReports.each {smr ->
                    smr.createdBy = catalogueItem.createdBy
                }
            }
        }
        if (catalogueItem.instanceOf(EnumerationType)) {
            return enumerationTypeService.checkFacetsAfterImportingCatalogueItem(catalogueItem as EnumerationType)
        } else {
            return super.checkFacetsAfterImportingCatalogueItem(catalogueItem) as DataType
        }
    }

    def findByDataModelIdAndId(Serializable dataModelId, Serializable id) {
        DataType.byDataModelIdAndId(dataModelId, id).find()
    }

    void checkImportedDataTypeAssociations(User importingUser, DataModel dataModel, DataType dataType) {
        dataModel.addToDataTypes(dataType)
        dataType.checkPath()
        dataType.createdBy = importingUser.emailAddress
        if (dataType.instanceOf(EnumerationType)) {
            EnumerationType enumerationType = (dataType as EnumerationType)
            enumerationType.fullSortOfChildren(enumerationType.enumerationValues)
            enumerationType.enumerationValues.each {ev ->
                ev.createdBy = importingUser.emailAddress
                ev.checkPath()
            }
        }
        checkFacetsAfterImportingCatalogueItem(dataType)
    }

    def findAllByDataModelId(Serializable dataModelId, Map paginate = [:]) {
        DataType.withFilter(DataType.byDataModelId(dataModelId), paginate).list(paginate)
    }

    List<DataType> findAllByImportingDataModelId(UUID dataModelId) {
        DataType.byImportingDataModelId(dataModelId).list()
    }

    def findAllByDataModelIdAndLabelIlikeOrDescriptionIlike(Serializable dataModelId, String searchTerm, Map paginate = [:]) {
        DataType.byDataModelIdAndLabelIlikeOrDescriptionIlike(dataModelId, searchTerm).list(paginate)
    }

    HQLPagedResultList findAllByDataModelIdAndLabelIlikeOrDescriptionIlikeIncludingImported(UUID dataModelId, String searchTerm, Map paginate = [:]) {
        findAllByDataModelIdAndLabelIlikeOrDescriptionIlikeIncludingImported(dataModelId, searchTerm, paginate, paginate)
    }

    HQLPagedResultList findAllByDataModelIdAndLabelIlikeOrDescriptionIlikeIncludingImported(UUID dataModelId, String searchTerm, Map filters, Map pagination) {

        Map<String, Object> queryParams = [dataModelId: dataModelId,
                                           searchTerm : "%${searchTerm}%"]

        findAllDataTypesByHQLQuery('''
FROM DataType dt
LEFT JOIN dt.importingDataModels idm
WHERE
(
    dt.dataModel.id = :dataModelId
    OR
    idm.id = :dataModelId
)
AND (
    lower(dt.label) like lower(:searchTerm)
    OR
    lower(dt.description) like lower(:searchTerm)
)''', queryParams, filters, pagination)
    }

    def findAllByDataModelIdIncludingImported(UUID dataModelId, Map filters, Map pagination) {
        Map<String, Object> queryParams = [dataModelId: dataModelId]
        findAllDataTypesByHQLQuery('''
FROM DataType dt
LEFT JOIN dt.importingDataModels idm
WHERE
(
    dt.dataModel.id = :dataModelId
    OR
    idm.id = :dataModelId
)''', queryParams, filters, pagination)
    }

    private HQLPagedResultList<DataType> findAllDataTypesByHQLQuery(String baseQuery, Map<String, Object> queryParams, Map filters, Map pagination) {
        queryParams.putAll(extractFiltersAsHQLParameters(filters))

        String filteredQuery = applyHQLFilters(baseQuery, 'dt', filters)
        // Cannot sort DCs including imported using idx combined with any other field
        String sortedQuery = applyHQLSort(filteredQuery, 'dt', pagination[ARGUMENT_SORT] ?: ['label': 'asc'], pagination, true)

        new HQLPagedResultList<DataType>(DataType)
            .list("SELECT DISTINCT dt ${sortedQuery}".toString())
            .count("SELECT COUNT(DISTINCT dt.id) ${filteredQuery}".toString())
            .queryParams(queryParams)
            .paginate(pagination)
    }

    void matchReferenceClasses(DataModel dataModel, Collection<ReferenceType> referenceTypes, Collection<Map> bindingMaps = []) {
        referenceTypes.sort {it.label}.each {rdt ->
            Map dataTypeBindingMap = bindingMaps.find {it.label == rdt.label} ?: [:]
            Map refClassBindingMap = dataTypeBindingMap.referenceClass ?: [:]
            matchReferenceClass(dataModel, rdt, refClassBindingMap)
        }
    }

    @SuppressFBWarnings('UPM_UNCALLED_PRIVATE_METHOD')
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
                def possibles = dataModel.dataClasses.findAll {it.label == referenceType.referenceClass.label}
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
            DataClass dataClass = dataModel.dataClasses.find {it.label == bindingMap.referenceClass.label}
            if (dataClass) dataClass.addToReferenceTypes(referenceType)
        }
    }

    @Override
    DataType copy(Model copiedDataModel, DataType original, CatalogueItem nonModelParent, UserSecurityPolicyManager userSecurityPolicyManager) {
        copyDataType(copiedDataModel as DataModel, original, userSecurityPolicyManager.user, userSecurityPolicyManager)
    }

    DataType copyDataType(DataModel copiedDataModel, DataType original, User copier, UserSecurityPolicyManager userSecurityPolicyManager,
                          boolean copySummaryMetadata = false, CopyInformation copyInformation = new CopyInformation()) {

        DataType copy = createNewDataTypeFromOriginal(copiedDataModel, original, userSecurityPolicyManager)

        copy = copyModelItemInformation(original, copy, copier, userSecurityPolicyManager, copySummaryMetadata, copyInformation)
        setCatalogueItemRefinesCatalogueItem(copy, original, copier)

        copiedDataModel.addToDataTypes(copy)

        copy
    }

    DataType createNewDataTypeFromOriginal(DataModel copiedDataModel, DataType original, UserSecurityPolicyManager userSecurityPolicyManager) {
        DataType copy

        String domainType = original.domainType
        switch (domainType) {
            case DataType.PRIMITIVE_DOMAIN_TYPE:
                copy = new PrimitiveType(units: original.units)
                break
            case DataType.ENUMERATION_DOMAIN_TYPE:
                copy = new EnumerationType()
                CopyInformation copyInformation = new CopyInformation(copyIndex: true)
                original.enumerationValues.sort().each {ev ->
                    copy.addToEnumerationValues(
                        enumerationValueService.copyEnumerationValue(copiedDataModel, ev, copy, userSecurityPolicyManager.user, userSecurityPolicyManager, copyInformation))
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
        copy
    }

    DataType copyModelItemInformation(DataType original,
                                      DataType copy,
                                      User copier,
                                      UserSecurityPolicyManager userSecurityPolicyManager,
                                      boolean copySummaryMetadata,
                                      copyInformation) {
        copy = super.copyModelItemInformation(original, copy, copier, userSecurityPolicyManager, copyInformation)
        if (copySummaryMetadata) {
            copy = copySummaryMetadataFromOriginal(original, copy, copier, copyInformation)
        }
        copy
    }

    @Override
    DataType copyModelItemInformation(DataType original,
                                      DataType copy,
                                      User copier,
                                      UserSecurityPolicyManager userSecurityPolicyManager, CopyInformation copyInformation) {
        copyModelItemInformation(original, copy, copier, userSecurityPolicyManager, false, copyInformation)
    }

    DataModel addDefaultListOfDataTypesToDataModel(DataModel dataModel, List<DefaultDataType> defaultDataTypes) {
        defaultDataTypes.each {
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

    def <T extends DataType> T mergeDataTypes(List<T> dataTypes) {
        mergeDataTypes(dataTypes.first(), dataTypes)
    }

    def <T extends DataType> T mergeDataTypes(T keep, List<T> dataTypes) {
        for (int i = 1; i < dataTypes.size(); i++) {
            mergeDataTypes(keep, dataTypes[i])
            delete(dataTypes[i])
        }
        keep
    }

    void mergeDataTypes(DataType keep, DataType replace) {
        replace.dataElements?.each {de ->
            keep.addToDataElements(de)
        }
        List<Metadata> mds = []
        mds += replace.metadata ?: []
        mds.findAll {!keep.findMetadataByNamespaceAndKey(it.namespace, it.key)}.each {md ->
            replace.removeFromMetadata(md)
            keep.addToMetadata(md.namespace, md.key, md.value, md.createdBy)
        }
    }

    /*
     * Find a DataType which is labeled with label and whose parent is parentCatalogueItem. The parentCatalogueItem
     * can be a DataModel.
     * @param parentCatalogueItem The DataModel which is the parent of the DataType being sought
     * @param label The label of the DataType being sought
     */

    @Override
    DataType findByParentIdAndLabel(UUID parentId, String label) {
        DataType.byDataModelId(parentId).eq('label', label).get()
    }

    @Override
    List<DataType> findAllByMetadataNamespaceAndKey(String namespace, String key, Map pagination) {
        DataType.byMetadataNamespaceAndKey(namespace, key).list(pagination)
    }

    @Override
    List<DataType> findAllByMetadataNamespace(String namespace, Map pagination) {
        DataType.byMetadataNamespace(namespace).list(pagination)
    }

    boolean isDataTypeBeingUsedAsImport(DataType dataType) {
        DataModel.byImportedDataTypeId(dataType.id).count()
    }

    @Override
    CopyInformation cacheFacetInformationForCopy(List<UUID> originalIds, CopyInformation copyInformation = null) {
        CopyInformation cachedInformation = super.cacheFacetInformationForCopy(originalIds, copyInformation)
        cacheSummaryMetadataInformationForCopy(originalIds, cachedInformation)
    }

    DataType findByDataModelIdAndDomainTypeAndLabel(UUID dataModelId, String domainType, String label) {
        DataType.byDataModelId(dataModelId)
            .eq('domainType', domainType)
            .eq('label', label)
            .get()
    }

    DataType checkBoundDataType(UUID dataModelId, DataType dataType) {
        // If no DT or DT has an id then this has bound by id and doesnt need any further checking
        if (!dataType || dataType.id) return dataType
        DataType updated = checkBoundDataTypeLabel(dataModelId, dataType, false)
        DataType alreadyExisting = findByDataModelIdAndDomainTypeAndLabel(dataModelId, updated.domainType, updated.label)
        if (alreadyExisting) return alreadyExisting
        updated
    }

    DataType bindDataType(def objectToBind, UUID dataModelId, User user) throws ApiInvalidModelException {
        DataTypeValueConverter converter = new DataTypeValueConverter()
        def body = objectToBind
        if (!converter.canConvert(body)) return null

        DataType resource = converter.convert(body)
        resource.createdBy = user.emailAddress
        dataModelService.get(dataModelId)?.addToDataTypes(resource)

        checkBoundDataTypeLabel(dataModelId, resource)

        resource
    }

    DataType checkBoundDataTypeLabel(UUID dataModelId, DataType resource, boolean generateLabel = true) {
        // Allow label to not be set and use the label of the referencing object
        if (resource.instanceOf(ReferenceType) && !resource.label) {
            String label = "Reference to ${(resource as ReferenceType).referenceClass.label}"
            resource.label = generateLabel ? generateDefaultLabel(dataModelId, label) : label
        } else if (resource.instanceOf(ModelDataType) && !resource.label) {
            ModelDataType modelDataType = resource as ModelDataType
            Model model = modelDataTypeService.findModelByDomainTypeAndDomainId(modelDataType.modelResourceDomainType, modelDataType.modelResourceId)
            if (model) {
                String label = "Reference to ${model.label}"
                resource.label = generateLabel ? generateDefaultLabel(dataModelId, label) : label
            } else {
                // If no model then unset everything to make sure the validation fails
                modelDataType.modelResourceDomainType = null
                modelDataType.modelResourceId = null
            }
        }
        resource
    }

    /**
     * Query the datamodel for all datatypes with the provided label and increment if required.
     * Looks for any DTs with (like) the same label, if none exist then use the provided label.
     * If any exist then find the last use number .. e.g. "reference to x 1" has last number "1",
     * and generate the next label in the order .. e.g. "reference to x 2"
     * @param dataModelId
     * @param defaultLabel
     * @return
     */
    String generateDefaultLabel(UUID dataModelId, String defaultLabel) {
        List<String> siblingLabels = DataType.byDataModelId(dataModelId)
            .property('label')
            .like('label', "${defaultLabel}%")
            .sort('label')
            .list()

        if (!siblingLabels) {
            return defaultLabel
        }

        String lastLabel = siblingLabels.last()
        int lastNum
        lastLabel.find(/${defaultLabel}( \((\d+)\))?/) {
            lastNum = it[1] ? it[2].toInteger() : 0
        }

        "${defaultLabel} (${++lastNum})"
    }
}
