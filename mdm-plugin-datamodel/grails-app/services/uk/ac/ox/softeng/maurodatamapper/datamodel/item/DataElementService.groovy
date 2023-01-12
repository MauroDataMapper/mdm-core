/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.core.path.PathService
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.merge.FieldPatchData
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.CopyInformation
import uk.ac.ox.softeng.maurodatamapper.core.similarity.SimilarityPair
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataTypeService
import uk.ac.ox.softeng.maurodatamapper.datamodel.similarity.DataElementSimilarityResult
import uk.ac.ox.softeng.maurodatamapper.datamodel.traits.service.SummaryMetadataAwareService
import uk.ac.ox.softeng.maurodatamapper.gorm.HQLPagedResultList
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.engine.search.predicate.FilterFactory
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.engine.search.predicate.IdPathSecureFilterFactory
import uk.ac.ox.softeng.maurodatamapper.lucene.queries.mlt.BoostedMoreLikeThisQuery
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.apache.lucene.analysis.Analyzer
import org.hibernate.search.backend.lucene.LuceneBackend
import org.hibernate.search.backend.lucene.LuceneExtension
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep
import org.hibernate.search.engine.search.query.SearchResult
import org.hibernate.search.mapper.orm.Search
import org.hibernate.search.mapper.orm.mapping.SearchMapping
import org.hibernate.search.mapper.orm.session.SearchSession

import java.util.function.BiFunction

import static org.grails.orm.hibernate.cfg.GrailsHibernateUtil.ARGUMENT_SORT

@Slf4j
@Transactional
class DataElementService extends ModelItemService<DataElement> implements SummaryMetadataAwareService {

    DataClassService dataClassService
    DataTypeService dataTypeService
    SummaryMetadataService summaryMetadataService
    DataModelService dataModelService
    PathService pathService

    @Override
    DataElement get(Serializable id) {
        DataElement.get(id)
    }

    Long count() {
        DataElement.count()
    }

    @Override
    List<DataElement> list(Map args) {
        DataElement.list(args)
    }

    @Override
    List<DataElement> getAll(Collection<UUID> ids) {
        DataElement.getAll(ids).findAll()
    }

    @Override
    void deleteAll(Collection<DataElement> dataElements) {
        dataElements.each {delete(it)}
    }

    void delete(UUID id) {
        delete(get(id), true)
    }

    void delete(DataElement dataElement, boolean flush = false) {
        if (!dataElement) return
        dataElement.breadcrumbTree.removeFromParent()
        dataElement.dataType = null
        dataElement.dataClass?.removeFromDataElements(dataElement)
        dataElement.delete(flush: flush)
    }

    @Override
    void deleteAllByModelIds(Set<UUID> dataModelIds) {
        List<UUID> dataElementIds = DataElement.by().where {
            dataClass {
                inList('dataModel.id', dataModelIds)
            }
        }.id().list() as List<UUID>

        if (dataElementIds) {
            log.trace('Removing facets for {} DataElements', dataElementIds.size())
            deleteAllFacetsByMultiFacetAwareIds(dataElementIds,
                                                'delete from datamodel.join_dataelement_to_facet where dataelement_id in :ids')

            log.trace('Removing {} DataElements', dataElementIds.size())
            sessionFactory.currentSession
                .createSQLQuery('DELETE FROM datamodel.data_element WHERE id IN :ids')
                .setParameter('ids', dataElementIds)
                .executeUpdate()
        }
        log.trace('DataElements removed')
    }

    DataElement validate(DataElement dataElement) {
        dataElement.validate()
        dataElement
    }

    @Override
    void deleteAllFacetDataByMultiFacetAwareIds(List<UUID> catalogueItemIds) {
        super.deleteAllFacetDataByMultiFacetAwareIds(catalogueItemIds)
        summaryMetadataService.deleteAllByMultiFacetAwareItemIds(catalogueItemIds)
    }

    @Override
    DataElement save(Map args, DataElement dataElement) {
        if (!dataElement.dataType.ident()) {
            dataTypeService.save(dataElement.dataType)
        }
        super.save(args, dataElement)
    }

    @Override
    DataElement checkFacetsAfterImportingCatalogueItem(DataElement catalogueItem) {
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
    DataElement findByIdJoinClassifiers(UUID id) {
        DataElement.findById(id, [fetch: [classifiers: 'join']])
    }

    @Override
    void removeAllFromClassifier(Classifier classifier) {
        DataElement.byClassifierId(classifier.id).list().each {
            it.removeFromClassifiers(classifier)
        }
    }

    @Override
    List<DataElement> findAllByClassifier(Classifier classifier) {
        DataElement.byClassifierId(classifier.id).list()
    }

    @Override
    List<DataElement> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        findAllByClassifier(classifier).findAll {userSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, it.model.id)}
    }

    @Override
    Boolean shouldPerformSearchForTreeTypeCatalogueItems(String domainType) {
        domainType == DataElement.simpleName
    }


    @Override
    List<DataElement> findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                                 String searchTerm, String domainType) {
        List<UUID> readableIds = userSecurityPolicyManager.listReadableSecuredResourceIds(DataModel)
        if (!readableIds) return []

        List<DataElement> results = []
        if (shouldPerformSearchForTreeTypeCatalogueItems(domainType)) {
            log.debug('Performing hs label search')
            long start = System.currentTimeMillis()
            results =
                DataElement
                    .labelHibernateSearch(DataElement, searchTerm, readableIds.toList(), dataModelService.getAllReadablePaths(readableIds))
                    .results
            log.debug("Search took: ${Utils.getTimeString(System.currentTimeMillis() - start)}. Found ${results.size()}")
        }

        results
    }

    void matchUpDataTypes(DataModel dataModel, Collection<DataElement> dataElements) {
        if (dataElements) {
            log.debug('Matching up {} DataElements to a possible {} DataTypes', dataElements.size(), dataModel.dataTypes.size())
            def grouped = dataElements.groupBy {it.dataType.label}.sort {a, b ->
                def res = a.value.size() <=> b.value.size()
                if (res == 0) res = a.key <=> b.key
                res
            }
            log.debug('Grouped {} DataElements by DataType label', grouped.size())
            grouped.each {label, elements ->
                log.trace('Matching {} elements to DataType label {}', elements.size(), label)
                DataType dataType = dataModel.findDataTypeByLabel(label)

                if (!dataType) {
                    log.debug('No DataType for {} in DataModel, using first DataElement DataType as base', label)
                    DataElement dataElement = elements.first()
                    dataType = dataElement.dataType
                    dataType.createdBy = dataElement.createdBy
                    dataModel.addToDataTypes(dataType)
                }
                elements.each {dataType.addToDataElements(it)}
            }
        }
    }

    DataElement findByDataClassIdAndId(Serializable dataClassId, Serializable id) {
        DataElement.byDataClassIdAndId(dataClassId, id).find()
    }

    DataElement findByDataTypeIdAndId(Serializable dataTypeId, Serializable id) {
        DataElement.byDataTypeIdAndId(dataTypeId, id).find()
    }

    List<DataElement> findAllByDataClassId(Serializable dataClassId, Map pagination = [:]) {
        findAllByDataClassId(dataClassId, pagination, pagination)
    }

    List<DataElement> findAllByDataClassId(Serializable dataClassId, Map filter, Map pagination) {
        DataElement.withFilter(DataElement.byDataClassId(dataClassId), filter).list(pagination)
    }

    List<DataElement> findAllByDataClassIdIncludingImported(UUID dataClassId, Map filters, Map pagination) {
        Map<String, Object> queryParams = [dataClassId: dataClassId]
        queryParams.putAll(extractFiltersAsHQLParameters(filters, 'dataType'))

        String baseQuery = applyHQLFilters('''
FROM DataElement de
LEFT JOIN de.importingDataClasses idc
INNER JOIN de.dataType dt
WHERE (de.dataClass.id = :dataClassId OR idc.id = :dataClassId)''', 'de', filters)

        // Cannot sort DEs including imported using idx
        String sortedQuery = applyHQLSort(baseQuery, 'de', pagination[ARGUMENT_SORT] ?: ['label': 'asc'], pagination, true)

        new HQLPagedResultList<DataElement>(DataElement)
            .list("SELECT DISTINCT de ${sortedQuery}".toString())
            .count("SELECT COUNT(DISTINCT de.id) ${baseQuery}".toString())
            .queryParams(queryParams)
            .paginate(pagination)
            .postProcess {
                it.dataType = proxyHandler.unwrapIfProxy(it.dataType)
                it.trackChanges() // unwrapping the proxy changes the object and therefore is detected as a "change" this call undos this change as its not actually one
            }
    }

    String applyHQLFilters(String originalQuery, String ciQueryPrefix, Map filters) {
        StringBuilder filteredQuery = new StringBuilder(super.applyHQLFilters(originalQuery, ciQueryPrefix, filters))
        if (filters.dataType) filteredQuery.append '\nAND lower(dt.label) LIKE lower(:dataType)'
        filteredQuery.toString()
    }

    List<DataElement> findAllByDataClassIdJoinDataType(Serializable dataClassId) {
        DataElement.byDataClassId(dataClassId).join('dataType').sort('label').list()
    }

    List<DataElement> findAllByDataTypeId(Serializable dataTypeId, Map pagination = [:]) {
        DataElement.withFilter(DataElement.byDataTypeId(dataTypeId), pagination).list(pagination)
    }

    List<DataElement> findAllByDataType(DataType dataType) {
        DataElement.byDataType(dataType).list()
    }

    List<DataElement> findAllByDataClass(DataClass dataClass) {
        DataElement.byDataClass(dataClass).list()
    }

    List<DataElement> findAllByImportingDataClassId(UUID dataClassId) {
        DataElement.byImportingDataClassId(dataClassId).list()
    }

    List<DataElement> findAllByImportingDataClassIds(List<UUID> dataClassIds) {
        if (!dataClassIds) return []
        DataElement.byImportingDataClassIdInList(dataClassIds).list()
    }

    List<DataElement> findAllByDataModelId(Serializable dataModelId, Map pagination = [:]) {
        DataElement.byDataModelId(dataModelId).list(pagination)
    }

    List<DataElement> findAllByDataModelIdAndLabelIlike(Serializable dataModelId, String labelSearch, Map pagination = [:]) {
        DataElement.byDataModelIdAndLabelIlike(dataModelId, labelSearch).list(pagination)
    }

    Number countByDataClassId(Serializable dataClassId) {
        DataElement.byDataClassId(dataClassId).count()
    }

    Number countByDataTypeId(Serializable dataTypeId) {
        DataElement.byDataTypeId(dataTypeId).count()
    }

    Number countByDataModelId(Serializable dataModelId) {
        DataElement.byDataModelId(dataModelId).count()
    }

    DataElement findByDataClassPathAndLabel(DataModel dataModel, List<String> dataClassPath, String label) {
        DataClass dataClass = dataClassService.findDataClassByPath(dataModel, dataClassPath)
        dataClass ? findByDataClassIdAndLabel(dataClass.id, label) : null
    }

    DataElement findByDataClassIdAndLabel(Serializable dataClassId, String label) {
        DataElement.byDataClassIdAndLabel(dataClassId, label).get()
    }

    DataElement findByDataModelIdAndId(Serializable dataModelId, Serializable id) {
        DataElement.byDataModelIdAndId(dataModelId, id).get()
    }

    DataElement findOrCreateDataElementForDataClass(DataClass parentClass, String label, String description, User createdBy,
                                                    DataType dataType,
                                                    Integer minMultiplicity = 0, Integer maxMultiplicity = 1) {
        String cleanLabel = label.trim()
        DataElement dataElement = parentClass.findDataElement(cleanLabel)

        if (!dataElement) {
            dataElement = new DataElement(label: cleanLabel, description: description, createdBy: createdBy.emailAddress,
                                          minMultiplicity: minMultiplicity,
                                          maxMultiplicity: maxMultiplicity)

            if (!dataType.label) dataType.setLabel("$cleanLabel-dataType")
            dataType.addToDataElements(dataElement)
            parentClass.addToDataElements(dataElement)
        }
        if (dataElement.dataType.label != dataType.label) {
            return findOrCreateDataElementForDataClass(parentClass, "${cleanLabel}.1", description, createdBy, dataType, minMultiplicity,
                                                       maxMultiplicity)
        }
        dataElement
    }

    Path buildDataElementPath(DataElement dataElement) {
        DataClass parent = dataElement.dataClass
        List<DataClass> parents = []

        while (parent) {
            parents << parent
            parent = parent.parentDataClass
        }

        List<MdmDomain> pathObjects = []
        pathObjects << dataElement.model
        pathObjects.addAll(parents.reverse())
        pathObjects << dataElement
        Path.from(pathObjects)
    }

    @Override
    DataElement copy(Model copiedDataModel, DataElement original, CatalogueItem parentDataClass,
                     UserSecurityPolicyManager userSecurityPolicyManager) {
        DataElement copy = copyDataElement(copiedDataModel as DataModel, original, userSecurityPolicyManager.user, userSecurityPolicyManager)
        if (parentDataClass) {
            (parentDataClass as DataClass).addToDataElements(copy)
        }
        copy
    }

    DataElement copyDataElement(DataModel copiedDataModel, DataElement original, User copier,
                                UserSecurityPolicyManager userSecurityPolicyManager, boolean copySummaryMetadata = false,
                                CopyInformation copyInformation = new CopyInformation()) {
        DataElement copy = new DataElement(minMultiplicity: original.minMultiplicity,
                                           maxMultiplicity: original.maxMultiplicity)

        copy = copyModelItemInformation(original, copy, copier, userSecurityPolicyManager, copySummaryMetadata, copyInformation)
        setCatalogueItemRefinesCatalogueItem(copy, original, copier)

        DataType dataType = copiedDataModel.findDataTypeByLabel(original.dataType.label)

        // If theres no DataType then copy the original's DataType into the DataModel
        if (!dataType) {
            dataType = dataTypeService.copyDataType(copiedDataModel, original.dataType, copier,
                                                    userSecurityPolicyManager)
        }

        dataType.addToDataElements(copy)

        copy
    }

    DataElement copyModelItemInformation(DataElement original,
                                             DataElement copy,
                                             User copier,
                                             UserSecurityPolicyManager userSecurityPolicyManager,
                                             boolean copySummaryMetadata,
                                             CopyInformation copyInformation) {
        copy = super.copyModelItemInformation(original, copy, copier, userSecurityPolicyManager, copyInformation)
        if (copySummaryMetadata) {
            copySummaryMetadataFromOriginal(original, copy, copier, copyInformation)
        }
        copy
    }

    @Override
    DataElement copyModelItemInformation(DataElement original,
                                             DataElement copy,
                                             User copier,
                                             UserSecurityPolicyManager userSecurityPolicyManager,
                                             CopyInformation copyInformation) {
        copyModelItemInformation(original, copy, copier, userSecurityPolicyManager, false, copyInformation)
    }

    DataElementSimilarityResult findAllSimilarDataElementsInDataModel(DataModel dataModelToSearch, DataElement dataElementToCompare, maxResults = 5) {

        SearchSession searchSession = Search.session(sessionFactory.currentSession)
        SearchMapping searchMapping = Search.mapping(sessionFactory)
        Analyzer wordDelimiter = searchMapping.backend().unwrap(LuceneBackend).analyzer('wordDelimiter').get()
        Analyzer standard = searchMapping.backend().unwrap(LuceneBackend).analyzer('standard').get()

        String[] fields = ['label', 'dataClass.label', 'dataType.label', 'description'] as String[]

        List<BoostedMoreLikeThisQuery> moreLikeThisQueries = [
            new BoostedMoreLikeThisQuery(wordDelimiter, 'label', dataElementToCompare.label, fields)
                .boostedTo(2f)
                .withMinWordLength(2)
                .withMinDocFrequency(2),
            new BoostedMoreLikeThisQuery(wordDelimiter, 'dataClass.label', dataElementToCompare.dataClass.label, fields)
                .boostedTo(1f)
                .withMinWordLength(2)
                .withMinDocFrequency(2),
            new BoostedMoreLikeThisQuery(wordDelimiter, 'dataType.label', dataElementToCompare.dataType.label, fields)
                .boostedTo(1f)
                .withMinWordLength(2)
                .withMinDocFrequency(2),
        ]
        if (dataElementToCompare.description)
            moreLikeThisQueries.add(new BoostedMoreLikeThisQuery(standard, 'description', dataElementToCompare.description, fields)
                                        .boostedTo(1f)
                                        .withMinWordLength(2)
            )

        SearchResult<SimilarityPair<DataElement>> searchResult = searchSession.search(DataElement)
            .extension(LuceneExtension.get())
            .select {pf ->
                pf.composite(new BiFunction<DataElement, Float, SimilarityPair<DataElement>>() {

                    @Override
                    SimilarityPair<DataElement> apply(DataElement dataElement, Float score) {
                        if (!dataElement) throw new ApiInternalException('DES', 'No DataElement passed to apply function in findAllSimilarDataElementsInDataModel')
                        dataElement.dataClass = proxyHandler.unwrapIfProxy(dataElement.dataClass)
                        dataElement.dataType = proxyHandler.unwrapIfProxy(dataElement.dataType)
                        dataElement.dataClass.dataModel = proxyHandler.unwrapIfProxy(dataElement.dataClass.dataModel)
                        new SimilarityPair<DataElement>(dataElement, score)
                    }
                }, pf.entity(), pf.score())
            }
            .where {lsf ->
                BooleanPredicateClausesStep boolStep = lsf
                    .bool()
                    .filter(IdPathSecureFilterFactory.createFilter(lsf, [dataModelToSearch.id], [dataModelToSearch.path]))
                    .filter(FilterFactory.mustNot(lsf, lsf.id().matching(dataElementToCompare.id)))

                moreLikeThisQueries.each {mlt ->
                    boolStep.should(lsf.bool().must(lsf.fromLuceneQuery(mlt)).boost(mlt.boost))
                }

                boolStep
            }
            .fetch(maxResults)

        DataElementSimilarityResult similarityResult = new DataElementSimilarityResult(dataElementToCompare, searchResult)
        log.debug('Found {} similar results in {}', similarityResult.totalSimilar(), Utils.durationToString(similarityResult.took()))
        similarityResult

    }

    DataElement findDataElementWithSameLabelTree(DataModel dataModel, DataElement original) {
        DataClass parent = dataClassService.findSameLabelTree(dataModel, original.dataClass)
        parent.findDataElement(original.label)
    }

    void addDataElementIsFromDataElements(DataElement dataElement, Collection<DataElement> fromDataElements, User user) {
        addDataElementsAreFromDataElements([dataElement], fromDataElements, user)
    }

    void addDataElementsAreFromDataElement(Collection<DataElement> dataElements, DataElement fromDataElement, User user) {
        addDataElementsAreFromDataElements(dataElements, [fromDataElement], user)
    }

    void addDataElementsAreFromDataElements(Collection<DataElement> dataElements, Collection<DataElement> fromDataElements, User user) {
        if (!dataElements || !fromDataElements) throw new ApiInternalException('DESXX', 'No DataElements or FromDataElements exist to create links')
        List<SemanticLink> alreadyExistingLinks =
            semanticLinkService.findAllBySourceMultiFacetAwareItemIdInListAndTargetMultiFacetAwareItemIdInListAndLinkType(
                dataElements*.id, fromDataElements*.id, SemanticLinkType.IS_FROM)
        dataElements.each {de ->
            fromDataElements.each {fde ->
                // If no link already exists then add a new one
                if (!alreadyExistingLinks.any {it.multiFacetAwareItemId == de.id && it.targetMultiFacetAwareItemId == fde.id}) {
                    setDataElementIsFromDataElement(de, fde, user)
                }
            }
        }
    }

    void removeDataElementIsFromDataElements(DataElement dataElement, Collection<DataElement> fromDataElements) {
        removeDataElementsAreFromDataElements([dataElement], fromDataElements)
    }

    void removeDataElementsAreFromDataElement(Collection<DataElement> dataElements, DataElement fromDataElement) {
        removeDataElementsAreFromDataElements(dataElements, [fromDataElement])
    }

    void removeDataElementsAreFromDataElements(Collection<DataElement> dataElements, Collection<DataElement> fromDataElements) {
        if (!dataElements || !fromDataElements) throw new ApiInternalException('DESXX', 'No DataElements or FromDataElements exist to remove links')
        List<SemanticLink> links = semanticLinkService.findAllBySourceMultiFacetAwareItemIdInListAndTargetMultiFacetAwareItemIdInListAndLinkType(
            dataElements*.id, fromDataElements*.id, SemanticLinkType.IS_FROM)
        semanticLinkService.deleteAll(links)
    }

    void setDataElementIsFromDataElement(DataElement source, DataElement target, User user) {
        source.addToSemanticLinks(linkType: SemanticLinkType.IS_FROM, createdBy: user.getEmailAddress(), targetMultiFacetAwareItem: target)
    }

    /**
     * Find a DataElement which is labeled with label and which belongs to dataClass.
     * @param dataClass The DataClass which is the parent of the DataElement being sought
     * @param label The label of the DataElement being sought
     */
    DataElement findDataElement(DataClass dataClass, String label) {
        dataClass.dataElements.find {it.label == label.trim()}
    }

    /**
     * Find a DataElement which is labeled with label and whose parent is parentCatalogueItem. The parentCatalogueItem
     * can be a DataClass.
     * @param parentCatalogueItem The DataClass which is the parent of the DataElement being sought
     * @param label The label of the DataElement being sought
     */
    @Override
    DataElement findByParentIdAndLabel(UUID parentId, String label) {
        findByDataClassIdAndLabel(parentId, label)
    }

    @Override
    List<DataElement> findAllByMetadataNamespaceAndKey(String namespace, String key, Map pagination) {
        DataElement.byMetadataNamespaceAndKey(namespace, key).list(pagination)
    }

    @Override
    List<DataElement> findAllByMetadataNamespace(String namespace, Map pagination) {
        DataElement.byMetadataNamespace(namespace).list(pagination)
    }

    @Override
    void propagateContentsInformation(DataElement catalogueItem, DataElement previousVersionCatalogueItem) {
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
    CopyInformation cacheFacetInformationForCopy(List<UUID> originalIds, CopyInformation copyInformation = null) {
        CopyInformation cachedInformation = super.cacheFacetInformationForCopy(originalIds, copyInformation)
        cacheSummaryMetadataInformationForCopy(originalIds, cachedInformation)
    }

    @Override
    void preBatchSaveHandling(List<DataElement> modelItems) {
        // Fix HS issue around non-session loaded DT.
        // This may have an adverse effect on saving, which will need to be tested
        dataTypeService.getAll(modelItems.collect {it.dataType.id})
    }

    /**
     * Special handler to apply a modification patch to a DataType.
     * In the diff we set a mergeField called dataTypePath. In this method we use that dataTypePath to find the
     * relevant DataType.
     * 1. Find the data type in source
     * 2. Use the path of that data type to find the same data type in target
     * 3. Set the data type of the target data element to be that data type
     *
     * Note that this leads to a merge conflict in in the following scenario (as highlighted by test MD05 in DataModelFunctionalSpec):
     * 1. Common ancestor model has dataType1 and dataType2. dataElement has a dataType of dataType1.
     * 2. Create source and main branches from the common ancestor
     * 3. On source branch, change the dataElement to have a dataType of dataType2
     * 4. Merge the source into main.
     * 5. Make some more other changes to the source branch
     * 6. Get the mergeDiff for source into main again.
     * The mergeDiff looks like
     *{*   "fieldName": "dataTypePath",
     *    "path": "dm:Functional Test DataModel 1$source|dc:existingClass|de:existingDataElement@dataTypePath",
     *    "sourceValue": "dm:Functional Test DataModel 1$source|dt:existingDataType2",
     *    "targetValue": "dm:Functional Test DataModel 1$main|dt:existingDataType2",
     *    "commonAncestorValue": "dm:Functional Test DataModel 1$1.0.0|dt:existingDataType1",
     *    "isMergeConflict": true,
     *    "type": "modification"
     *}* because both source and main have a different data type to the one in common ancestor.
     * @param modificationPatch
     * @param targetDomain
     * @param fieldName
     * @return
     */
    @Override
    boolean handlesModificationPatchOfField(FieldPatchData modificationPatch, MdmDomain targetBeingPatched, DataElement targetDomain, String fieldName) {
        if (fieldName == 'dataTypePath') {
            // This is the dataType that has been changed on the source
            DataType sourceDataType = pathService.findResourceByPath(Path.from(modificationPatch.sourceValue))

            if (!sourceDataType) {
                throw new ApiInternalException('DES01', "Cannot find DataType with path ${modificationPatch.sourceValue}")
            }

            // We need the equivalent (i.e. matches by path) dataType on the target
            DataType targetDataType = pathService.findResourceByPathFromRootResource(targetDomain.dataClass.dataModel, sourceDataType.getPath()
                .getChildPath())

            if (targetDataType) {
                targetDomain.dataType = targetDataType
                return true
            } else {
                throw new ApiInternalException(
                    'DES02',
                    "Cannot find DataType with path ${sourceDataType.getPath().getChildPath()} on target DataModel ${targetDomain.dataClass.dataModel.id}")
            }
        }

        false
    }
}