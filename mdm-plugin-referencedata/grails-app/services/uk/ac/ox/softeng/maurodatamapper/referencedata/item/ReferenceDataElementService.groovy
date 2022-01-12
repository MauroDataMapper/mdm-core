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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.core.similarity.SimilarityPair
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.engine.search.predicate.FilterFactory
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.engine.search.predicate.IdPathSecureFilterFactory
import uk.ac.ox.softeng.maurodatamapper.lucene.queries.mlt.BoostedMoreLikeThisQuery
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadataService
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataTypeService
import uk.ac.ox.softeng.maurodatamapper.referencedata.similarity.ReferenceDataElementSimilarityResult
import uk.ac.ox.softeng.maurodatamapper.referencedata.traits.service.ReferenceSummaryMetadataAwareService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.apache.lucene.analysis.Analyzer
import org.hibernate.search.backend.lucene.LuceneBackend
import org.hibernate.search.backend.lucene.LuceneExtension
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep
import org.hibernate.search.engine.search.query.SearchResult
import org.hibernate.search.mapper.orm.mapping.SearchMapping
import org.hibernate.search.mapper.orm.session.SearchSession

import java.util.function.BiFunction

@Slf4j
@Transactional
class ReferenceDataElementService extends ModelItemService<ReferenceDataElement> implements ReferenceSummaryMetadataAwareService {

    ReferenceDataTypeService referenceDataTypeService
    ReferenceSummaryMetadataService referenceSummaryMetadataService

    @Override
    ReferenceDataElement get(Serializable id) {
        ReferenceDataElement.get(id)
    }

    Long count() {
        ReferenceDataElement.count()
    }

    @Override
    List<ReferenceDataElement> list(Map args) {
        ReferenceDataElement.list(args)
    }

    @Override
    List<ReferenceDataElement> getAll(Collection<UUID> ids) {
        ReferenceDataElement.getAll(ids).findAll()
    }

    @Override
    ReferenceDataElement save(Map args, ReferenceDataElement dataElement) {
        if (!dataElement.referenceDataType.ident()) {
            referenceDataTypeService.save(dataElement.referenceDataType)
        }
        super.save(args, dataElement)
    }

    @Override
    void deleteAll(Collection<ReferenceDataElement> dataElements) {
        dataElements.each {delete(it)}
    }

    void delete(UUID id) {
        delete(get(id), true)
    }

    void delete(ReferenceDataElement dataElement, boolean flush = false) {
        if (!dataElement) return
        dataElement.breadcrumbTree.removeFromParent()
        dataElement.referenceDataType = null
        dataElement.referenceDataModel?.removeFromReferenceDataElements(dataElement)
        dataElement.delete(flush: flush)
    }

    @Override
    ReferenceDataElement updateFacetsAfterInsertingCatalogueItem(ReferenceDataElement catalogueItem) {
        super.updateFacetsAfterInsertingCatalogueItem(catalogueItem)
        if (catalogueItem.referenceSummaryMetadata) {
            catalogueItem.referenceSummaryMetadata.each {
                if (!it.isDirty('multiFacetAwareItemId')) it.trackChanges()
                it.multiFacetAwareItemId = catalogueItem.getId()
            }
            ReferenceSummaryMetadata.saveAll(catalogueItem.referenceSummaryMetadata)
        }
        catalogueItem
    }

    @Override
    ReferenceDataElement findByIdJoinClassifiers(UUID id) {
        ReferenceDataElement.findById(id, [fetch: [classifiers: 'join']])
    }

    @Override
    void removeAllFromClassifier(Classifier classifier) {
        ReferenceDataElement.byClassifierId(classifier.id).list().each {
            it.removeFromClassifiers(classifier)
        }
    }

    @Override
    List<ReferenceDataElement> findAllByClassifier(Classifier classifier) {
        ReferenceDataElement.byClassifierId(classifier.id).list()
    }

    @Override
    List<ReferenceDataElement> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        findAllByClassifier(classifier).findAll {userSecurityPolicyManager.userCanReadSecuredResourceId(ReferenceDataModel, it.model.id)}
    }

    @Override
    Boolean shouldPerformSearchForTreeTypeCatalogueItems(String domainType) {
        domainType == ReferenceDataElement.simpleName
    }


    @Override
    List<ReferenceDataElement> findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                                          String searchTerm, String domainType) {
        List<UUID> readableIds = userSecurityPolicyManager.listReadableSecuredResourceIds(ReferenceDataModel)
        if (!readableIds) return []

        List<ReferenceDataElement> results = []
        if (shouldPerformSearchForTreeTypeCatalogueItems(domainType)) {
            log.debug('Performing lucene label search')
            long start = System.currentTimeMillis()
            results = ReferenceDataElement.luceneLabelSearch(ReferenceDataElement, searchTerm, readableIds.toList()).results
            log.debug("Search took: ${Utils.getTimeString(System.currentTimeMillis() - start)}. Found ${results.size()}")
        }

        results
    }

    void matchUpDataTypes(ReferenceDataModel referenceDataModel, Collection<ReferenceDataElement> dataElements) {
        if (referenceDataModel.referenceDataTypes == null) referenceDataModel.referenceDataTypes = [] as HashSet
        if (dataElements) {
            log.debug("Matching up {} DataElements to a possible {} DataTypes", dataElements.size(), referenceDataModel.referenceDataTypes.size())
            def grouped = dataElements.groupBy {it.referenceDataType.label}.sort {a, b ->
                def res = a.value.size() <=> b.value.size()
                if (res == 0) res = a.key <=> b.key
                res
            }
            log.debug('Grouped {} DataElements by DataType label', grouped.size())
            grouped.each {label, elements ->
                log.trace('Matching {} elements to DataType label {}', elements.size(), label)
                ReferenceDataType dataType = referenceDataModel.findDataTypeByLabel(label)

                if (!dataType) {
                    log.debug('No DataType for {} in DataModel, using first DataElement DataType as base', label)
                    dataType = elements.first().referenceDataType
                    referenceDataModel.addToReferenceDataTypes(dataType)
                }
                elements.each {dataType.addToDataElements(it)}
            }
        }
    }

    ReferenceDataElement findByReferenceDataModelIdAndId(Serializable referenceDataModelId, Serializable id) {
        ReferenceDataElement.byReferenceDataModelIdAndId(referenceDataModelId, id).find()
    }

    ReferenceDataElement findByReferenceDataTypeIdAndId(Serializable referenceDataTypeId, Serializable id) {
        ReferenceDataElement.byReferenceDataTypeIdAndId(referenceDataTypeId, id).find()
    }

    List<ReferenceDataElement> findAllByReferenceDataModelId(Serializable referenceDataModelId, Map pagination = [:]) {
        findAllByReferenceDataModelId(referenceDataModelId, pagination, pagination)
    }

    List<ReferenceDataElement> findAllByReferenceDataModelId(Serializable referenceDataModelId, Map filter, Map pagination) {
        ReferenceDataElement.withFilter(ReferenceDataElement.byReferenceDataModelId(referenceDataModelId), filter).list(pagination)
    }

    List<ReferenceDataElement> findAllByReferenceDataModelIdJoinDataType(Serializable referenceDataModelId) {
        ReferenceDataElement.byReferenceDataModelId(referenceDataModelId).join('dataType').sort('label').list()
    }

    List<ReferenceDataElement> findAllByReferenceDataTypeId(Serializable referenceDataTypeId, Map pagination = [:]) {
        ReferenceDataElement.withFilter(ReferenceDataElement.byReferenceDataTypeId(referenceDataTypeId), pagination).list(pagination)
    }

    List<ReferenceDataElement> findAllByReferenceDataType(ReferenceDataType referenceDataType) {
        ReferenceDataElement.byReferenceDataType(referenceDataType).list()
    }

    List<ReferenceDataElement> findAllByReferenceDataModel(ReferenceDataModel referenceDataModel) {
        ReferenceDataElement.byReferenceDataModel(referenceDataModel).list()
    }


    List<ReferenceDataElement> findAllByReferenceDataModelIdAndLabelIlike(Serializable referenceDataModelId, String labelSearch, Map pagination = [:]) {
        ReferenceDataElement.byReferenceDataModelIdAndLabelIlike(referenceDataModelId, labelSearch).list(pagination)
    }

    Number countByReferenceDataTypeId(Serializable referenceDataTypeId) {
        ReferenceDataElement.byReferenceDataTypeId(referenceDataTypeId).count()
    }

    Number countByReferenceDataModelId(Serializable referenceDataModelId) {
        ReferenceDataElement.byReferenceDataModelId(referenceDataModelId).count()
    }


    ReferenceDataElement findOrCreateDataElementForReferenceDataModel(ReferenceDataModel referenceDataModel, String label, String description, User createdBy,
                                                                      ReferenceDataType dataType,
                                                                      Integer minMultiplicity = 0, Integer maxMultiplicity = 1) {
        String cleanLabel = label.trim()
        ReferenceDataElement dataElement = referenceDataModel.findDataElement(cleanLabel)

        if (!dataElement) {
            dataElement = new ReferenceDataElement(label: cleanLabel, description: description, createdBy: createdBy.emailAddress,
                                                   minMultiplicity: minMultiplicity,
                                                   maxMultiplicity: maxMultiplicity)

            if (!dataType.label) dataType.setLabel("$cleanLabel-dataType")
            dataType.addToDataElements(dataElement)
            referenceDataModel.addToReferenceDataElements(dataElement)
        }
        if (dataElement.referenceDataType.label != dataType.label) {
            return findOrCreateDataElementForReferenceDataModel(referenceDataModel, "${cleanLabel}.1", description, createdBy, dataType, minMultiplicity,
                                                                maxMultiplicity)
        }
        dataElement
    }

    ReferenceDataElement copyReferenceDataElement(ReferenceDataModel copiedReferenceDataModel, ReferenceDataElement original, User copier,
                                                  UserSecurityPolicyManager userSecurityPolicyManager) {
        ReferenceDataElement copy = new ReferenceDataElement(minMultiplicity: original.minMultiplicity,
                                                             maxMultiplicity: original.maxMultiplicity)

        copy = copyCatalogueItemInformation(original, copy, copier, userSecurityPolicyManager)
        setCatalogueItemRefinesCatalogueItem(copy, original, copier)

        ReferenceDataType referenceDataType = copiedReferenceDataModel.findReferenceDataTypeByLabel(original.referenceDataType.label)

        // If theres no DataType then copy the original's DataType into the DataModel
        if (!referenceDataType) {
            referenceDataType = referenceDataTypeService.copyReferenceDataType(copiedReferenceDataModel, original.referenceDataType, copier,
                                                                               userSecurityPolicyManager)
        }

        copy.referenceDataType = referenceDataType

        copiedReferenceDataModel.addToReferenceDataElements(copy)

        copy
    }

    @Override
    ReferenceDataElement copyCatalogueItemInformation(ReferenceDataElement original,
                                                      ReferenceDataElement copy,
                                                      User copier,
                                                      UserSecurityPolicyManager userSecurityPolicyManager,
                                                      boolean copySummaryMetadata = false) {
        copy = super.copyCatalogueItemInformation(original, copy, copier, userSecurityPolicyManager)
        if (copySummaryMetadata) {
            referenceSummaryMetadataService.findAllByMultiFacetAwareItemId(original.id).each {
                copy.addToReferenceSummaryMetadata(label: it.label, summaryMetadataType: it.summaryMetadataType, createdBy: copier.emailAddress)
            }
        }
        copy
    }

    ReferenceDataElementSimilarityResult findAllSimilarReferenceDataElementsInReferenceDataModel(ReferenceDataModel referenceDataModelToSearch,
                                                                                                 ReferenceDataElement referenceDataElementToCompare,
                                                                                                 maxResults = 5) {

        SearchSession searchSession = org.hibernate.search.mapper.orm.Search.session(sessionFactory.currentSession)
        SearchMapping searchMapping = org.hibernate.search.mapper.orm.Search.mapping(sessionFactory)
        Analyzer wordDelimiter = searchMapping.backend().unwrap(LuceneBackend).analyzer('wordDelimiter').get()
        Analyzer standard = searchMapping.backend().unwrap(LuceneBackend).analyzer('standard').get()

        String[] fields = ['label', 'referenceDataType.label', 'description'] as String[]

        List<BoostedMoreLikeThisQuery> moreLikeThisQueries = [
            new BoostedMoreLikeThisQuery(wordDelimiter, 'label', referenceDataElementToCompare.label, fields)
                .boostedTo(2f)
                .withMinWordLength(2)
                .withMinDocFrequency(2),
            new BoostedMoreLikeThisQuery(wordDelimiter, 'referenceDataType.label', referenceDataElementToCompare.referenceDataType.label, fields)
                .boostedTo(1f)
                .withMinWordLength(2)
                .withMinDocFrequency(2),
        ]
        if (referenceDataElementToCompare.description)
            moreLikeThisQueries.add(new BoostedMoreLikeThisQuery(standard, 'description', referenceDataElementToCompare.description, fields)
                                        .boostedTo(1f)
                                        .withMinWordLength(2)
            )

        SearchResult<SimilarityPair<ReferenceDataElement>> searchResult = searchSession.search(ReferenceDataElement)
            .extension(LuceneExtension.get())
            .select {pf ->
                pf.composite(new BiFunction<ReferenceDataElement, Float, SimilarityPair<ReferenceDataElement>>() {

                    @Override
                    SimilarityPair<ReferenceDataElement> apply(ReferenceDataElement dataElement, Float score) {
                        dataElement.referenceDataType = proxyHandler.unwrapIfProxy(dataElement.referenceDataType)
                        new SimilarityPair<ReferenceDataElement>(dataElement, score)
                    }
                }, pf.entity(), pf.score())
            }
            .where {lsf ->
                BooleanPredicateClausesStep boolStep = lsf
                    .bool()
                    .filter(IdPathSecureFilterFactory.createFilter(lsf, [referenceDataModelToSearch.id],  [referenceDataModelToSearch.path.last()]))
                    .filter(FilterFactory.mustNot(lsf, lsf.id().matching(referenceDataElementToCompare.id)))

                moreLikeThisQueries.each {mlt ->
                    boolStep.should(lsf.bool().must(lsf.fromLuceneQuery(mlt)).boost(mlt.boost))
                }

                boolStep
            }
            .fetch(maxResults)

        ReferenceDataElementSimilarityResult similarityResult = new ReferenceDataElementSimilarityResult(referenceDataElementToCompare, searchResult)
        log.debug('Found {} similar results in {}', similarityResult.totalSimilar(), Utils.durationToString(similarityResult.took()))
        similarityResult
    }


    void addDataElementIsFromDataElements(ReferenceDataElement dataElement, Collection<ReferenceDataElement> fromDataElements, User user) {
        addDataElementsAreFromDataElements([dataElement], fromDataElements, user)
    }

    void addDataElementsAreFromDataElement(Collection<ReferenceDataElement> dataElements, ReferenceDataElement fromDataElement, User user) {
        addDataElementsAreFromDataElements(dataElements, [fromDataElement], user)
    }

    void addDataElementsAreFromDataElements(Collection<ReferenceDataElement> dataElements, Collection<ReferenceDataElement> fromDataElements, User user) {
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

    void removeDataElementIsFromDataElements(ReferenceDataElement dataElement, Collection<ReferenceDataElement> fromDataElements) {
        removeDataElementsAreFromDataElements([dataElement], fromDataElements)
    }

    void removeDataElementsAreFromDataElement(Collection<ReferenceDataElement> dataElements, ReferenceDataElement fromDataElement) {
        removeDataElementsAreFromDataElements(dataElements, [fromDataElement])
    }

    void removeDataElementsAreFromDataElements(Collection<ReferenceDataElement> dataElements, Collection<ReferenceDataElement> fromDataElements) {
        if (!dataElements || !fromDataElements) throw new ApiInternalException('DESXX', 'No DataElements or FromDataElements exist to remove links')
        List<SemanticLink> links = semanticLinkService.findAllBySourceMultiFacetAwareItemIdInListAndTargetMultiFacetAwareItemIdInListAndLinkType(
            dataElements*.id, fromDataElements*.id, SemanticLinkType.IS_FROM)
        semanticLinkService.deleteAll(links)
    }

    void setDataElementIsFromDataElement(ReferenceDataElement source, ReferenceDataElement target, User user) {
        source.addToSemanticLinks(linkType: SemanticLinkType.IS_FROM, createdBy: user.getEmailAddress(), targetMultiFacetAwareItem: target)
    }

    void checkImportedReferenceDataElementAssociations(User importingUser, ReferenceDataModel referenceDataModel, ReferenceDataElement referenceDataElement) {
        referenceDataModel.addToReferenceDataElements(referenceDataElement)
        referenceDataElement.createdBy = importingUser.emailAddress
        referenceDataElement.referenceDataType.createdBy = importingUser.emailAddress

        //Get the reference data type for this element by getting the matching reference data type for the model
        referenceDataElement.referenceDataType = referenceDataModel.referenceDataTypes.find {it.label == referenceDataElement.referenceDataType.label}

        checkFacetsAfterImportingCatalogueItem(referenceDataElement)
    }

    @Override
    List<ReferenceDataElement> findAllByMetadataNamespaceAndKey(String namespace, String key, Map pagination) {
        ReferenceDataElement.byMetadataNamespaceAndKey(namespace, key).list(pagination)
    }

    @Override
    List<ReferenceDataElement> findAllByMetadataNamespace(String namespace, Map pagination) {
        ReferenceDataElement.byMetadataNamespace(namespace).list(pagination)
    }
}