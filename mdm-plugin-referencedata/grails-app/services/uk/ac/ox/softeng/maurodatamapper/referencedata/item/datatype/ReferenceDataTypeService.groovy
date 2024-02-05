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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.CopyInformation
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModelService
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadataService
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElement
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElementService
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.enumeration.ReferenceEnumerationValueService
import uk.ac.ox.softeng.maurodatamapper.referencedata.provider.DefaultReferenceDataTypeProvider
import uk.ac.ox.softeng.maurodatamapper.referencedata.rest.transport.DefaultReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.traits.service.ReferenceSummaryMetadataAwareService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

@SuppressWarnings('ClashingTraitMethods')
@Slf4j
@Transactional
class ReferenceDataTypeService extends ModelItemService<ReferenceDataType> implements DefaultReferenceDataTypeProvider,
    ReferenceSummaryMetadataAwareService {

    ReferenceDataElementService referenceDataElementService
    ReferencePrimitiveTypeService referencePrimitiveTypeService
    ReferenceEnumerationTypeService referenceEnumerationTypeService
    ReferenceSummaryMetadataService referenceSummaryMetadataService
    ReferenceDataModelService referenceDataModelService
    ReferenceEnumerationValueService referenceEnumerationValueService

    @Override
    ReferenceDataType get(Serializable id) {
        ReferenceDataType.get(id)
    }

    @Override
    boolean handlesPathPrefix(String pathPrefix) {
        // Have to override as the DataType class is abstract and can therefore not be instantiated
        pathPrefix == 'rdt'
    }

    Long count() {
        ReferenceDataType.count()
    }

    @Override
    List<ReferenceDataType> list(Map args = [:]) {
        ReferenceDataType.list(args)
    }

    @Override
    List<ReferenceDataType> getAll(Collection<UUID> ids) {
        ReferenceDataType.getAll(ids).findAll()
    }

    @Override
    void deleteAll(Collection<ReferenceDataType> catalogueItems) {
        ReferenceDataType.deleteAll(catalogueItems)
    }

    void delete(ReferenceDataType referenceDataType, boolean flush = false) {
        if (!referenceDataType) return
        referenceDataType.referenceDataModel.removeFromReferenceDataTypes(referenceDataType)
        referenceDataType.breadcrumbTree.removeFromParent()

        List<ReferenceDataElement> referenceDataElements = referenceDataElementService.findAllByReferenceDataType(referenceDataType)
        referenceDataElements.each {referenceDataElementService.delete(it)}

        switch (referenceDataType.domainType) {
            case ReferenceDataType.PRIMITIVE_DOMAIN_TYPE:
                referencePrimitiveTypeService.delete(referenceDataType as ReferencePrimitiveType, flush)
                break
            case ReferenceDataType.ENUMERATION_DOMAIN_TYPE:
                referenceEnumerationTypeService.delete(referenceDataType as ReferenceEnumerationType, flush)
        }
    }

    @Override
    ReferenceDataType findByIdJoinClassifiers(UUID id) {
        ReferenceDataType.findById(id, [fetch: [classifiers: 'join']])
    }

    @Override
    void removeAllFromClassifier(Classifier classifier) {
        ReferenceDataType.byClassifierId(ReferenceDataType, classifier.id).list().each {
            it.removeFromClassifiers(classifier)
        }
    }

    @Override
    List<ReferenceDataType> findAllByClassifier(Classifier classifier) {
        ReferenceDataType.byClassifierId(ReferenceDataType, classifier.id).list()
    }

    @Override
    List<ReferenceDataType> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        findAllByClassifier(classifier).findAll {
            userSecurityPolicyManager.userCanReadSecuredResourceId(ReferenceDataModel, it.model.id)
        }
    }

    @Override
    Boolean shouldPerformSearchForTreeTypeCatalogueItems(String domainType) {
        domainType == ReferenceDataType.simpleName
    }

    @Override
    List<ReferenceDataType> findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                                       String searchTerm, String domainType) {
        List<UUID> readableIds = userSecurityPolicyManager.listReadableSecuredResourceIds(ReferenceDataModel)
        if (!readableIds) return []

        List<ReferenceDataType> results = []
        if (shouldPerformSearchForTreeTypeCatalogueItems(domainType)) {
            log.debug('Performing hs label search')
            long start = System.currentTimeMillis()
            results = ReferenceDataType.labelHibernateSearch(ReferenceDataType, searchTerm, readableIds.toList(),
                                                             referenceDataModelService.getAllReadablePaths(readableIds)).results
            log.debug("Search took: ${Utils.getTimeString(System.currentTimeMillis() - start)}. Found ${results.size()}")
        }
        results
    }

    @Override
    List<DefaultReferenceDataType> getDefaultListOfReferenceDataTypes() {
        [
            new ReferencePrimitiveType(label: 'Text', description: 'A piece of text'),
            new ReferencePrimitiveType(label: 'Number', description: 'A whole number'),
            new ReferencePrimitiveType(label: 'Decimal', description: 'A decimal number'),
            new ReferencePrimitiveType(label: 'Date', description: 'A date'),
            new ReferencePrimitiveType(label: 'DateTime', description: 'A date with a timestamp'),
            new ReferencePrimitiveType(label: 'Timestamp', description: 'A timestamp'),
            new ReferencePrimitiveType(label: 'Boolean', description: 'A true or false value'),
            new ReferencePrimitiveType(label: 'Duration', description: 'A time period in arbitrary units')
        ].collect {new DefaultReferenceDataType(it)}
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
        ReferenceDataType dataType = get(id)
        if (dataType) delete(dataType)
    }

    @Override
    ReferenceDataType checkFacetsAfterImportingCatalogueItem(ReferenceDataType dataType) {
        if (dataType.instanceOf(ReferenceEnumerationType)) {
            return referenceEnumerationTypeService.checkFacetsAfterImportingCatalogueItem(dataType as ReferenceEnumerationType)
        } else {
            return super.checkFacetsAfterImportingCatalogueItem(dataType) as ReferenceDataType
        }
    }

    def findByReferenceDataModelIdAndId(Serializable referenceDataModelId, Serializable id) {
        ReferenceDataType.byReferenceDataModelIdAndId(referenceDataModelId, id).find()
    }

    void checkImportedReferenceDataTypeAssociations(User importingUser, ReferenceDataModel referenceDataModel, ReferenceDataType referenceDataType) {
        referenceDataModel.addToReferenceDataTypes(referenceDataType)
        referenceDataType.createdBy = importingUser.emailAddress
        if (referenceDataType.instanceOf(ReferenceEnumerationType)) {
            (referenceDataType as ReferenceEnumerationType).referenceEnumerationValues.each {ev ->
                ev.createdBy = importingUser.emailAddress
            }
        }
        checkFacetsAfterImportingCatalogueItem(referenceDataType)
    }

    def findAllByReferenceDataModelId(Serializable referenceDataModel, Map paginate = [:]) {
        ReferenceDataType.withFilter(ReferenceDataType.byReferenceDataModelId(referenceDataModel), paginate).list(paginate)
    }

    def findAllByReferenceDataModelIdAndLabelIlikeOrDescriptionIlike(Serializable referenceDataModelId, String searchTerm, Map paginate = [:]) {
        ReferenceDataType.byReferenceDataModelIdAndLabelIlikeOrDescriptionIlike(referenceDataModelId, searchTerm).list(paginate)
    }

    @Override
    ReferenceDataType copy(Model copiedDataModel, ReferenceDataType original, CatalogueItem nonModelParent, UserSecurityPolicyManager userSecurityPolicyManager) {
        copyReferenceDataType(copiedDataModel as ReferenceDataModel, original, userSecurityPolicyManager.user, userSecurityPolicyManager)
    }

    ReferenceDataType copyReferenceDataType(ReferenceDataModel copiedReferenceDataModel, ReferenceDataType original, User copier,
                                            UserSecurityPolicyManager userSecurityPolicyManager, boolean copySummaryMetadata = false, CopyInformation copyInformation = null) {
        ReferenceDataType copy

        String domainType = original.domainType
        switch (domainType) {
            case ReferenceDataType.PRIMITIVE_DOMAIN_TYPE:
                copy = new ReferencePrimitiveType(units: original.units)
                break
            case ReferenceDataType.ENUMERATION_DOMAIN_TYPE:
                copy = new ReferenceEnumerationType()
                CopyInformation referenceEnumerationTypeCopyInformation = new CopyInformation(copyIndex: true)
                original.referenceEnumerationValues.sort().each {ev ->
                    copy.addToReferenceEnumerationValues(
                        referenceEnumerationValueService
                            .copyReferenceEnumerationValue(copiedReferenceDataModel, ev, copy, userSecurityPolicyManager.user, userSecurityPolicyManager,
                                                           referenceEnumerationTypeCopyInformation))
                }
                break
            default:
                throw new ApiInternalException('DTSXX', 'DataType domain type is unknown and therefore cannot be copied')
        }

        copy = copyModelItemInformation(original, copy, copier, userSecurityPolicyManager, copySummaryMetadata, copyInformation)
        setCatalogueItemRefinesCatalogueItem(copy, original, copier)

        copiedReferenceDataModel.addToReferenceDataTypes(copy)

        copy
    }

    @Override
    ReferenceDataType copyModelItemInformation(ReferenceDataType original,
                                               ReferenceDataType copy,
                                               User copier,
                                               UserSecurityPolicyManager userSecurityPolicyManager,
                                               boolean copySummaryMetadata = false, CopyInformation copyInformation) {
        copy = super.copyModelItemInformation(original, copy, copier, userSecurityPolicyManager, copyInformation)
        if (copySummaryMetadata) {
            referenceSummaryMetadataService.findAllByMultiFacetAwareItemId(original.id).each {
                copy.addToReferenceSummaryMetadata(label: it.label, summaryMetadataType: it.summaryMetadataType, createdBy: copier.emailAddress)
            }
        }
        copy
    }

    ReferenceDataModel addDefaultListOfReferenceDataTypesToReferenceDataModel(ReferenceDataModel referenceDataModel,
                                                                              List<DefaultReferenceDataType> defaultReferenceDataTypes) {
        defaultReferenceDataTypes.each {
            ReferenceDataType dataType
            switch (it.domainType) {
                case ReferencePrimitiveType.simpleName:
                    dataType = new ReferencePrimitiveType(units: it.units)
                    break
                case ReferenceEnumerationType.simpleName:
                    dataType = new ReferenceEnumerationType(referenceEnumerationValues: it.enumerationValues)
                    break
                default:
                    throw new ApiInternalException('DTSXX', "Unknown DataType [${it.domainType}] used for default datatype")
            }
            dataType.createdBy = referenceDataModel.createdBy
            dataType.label = it.label
            dataType.description = it.description
            referenceDataModel.addToReferenceDataTypes(dataType)
        }
        referenceDataModel
    }

    private def <T extends ReferenceDataType> T mergeDataTypes(List<T> dataTypes) {
        mergeDataTypes(dataTypes.first(), dataTypes)
    }

    private def <T extends ReferenceDataType> T mergeDataTypes(T keep, List<T> dataTypes) {
        for (int i = 1; i < dataTypes.size(); i++) {
            mergeDataTypes(keep, dataTypes[i])
            delete(dataTypes[i])
        }
        keep
    }

    private void mergeDataTypes(ReferenceDataType keep, ReferenceDataType replace) {
        replace.referenceDataElements?.each {de ->
            keep.addToReferenceDataElements(de)
        }
        List<Metadata> mds = []
        mds += replace.metadata ?: []
        mds.findAll {!keep.findMetadataByNamespaceAndKey(it.namespace, it.key)}.each {md ->
            replace.removeFromMetadata(md)
            keep.addToMetadata(md.namespace, md.key, md.value, md.createdBy)
        }
    }


    @Override
    List<ReferenceDataType> findAllByMetadataNamespaceAndKey(String namespace, String key, Map pagination) {
        ReferenceDataType.byMetadataNamespaceAndKey(namespace, key).list(pagination)
    }

    @Override
    List<ReferenceDataType> findAllByMetadataNamespace(String namespace, Map pagination) {
        ReferenceDataType.byMetadataNamespace(namespace).list(pagination)
    }

    ReferenceDataType findByReferenceDataModelIdAndLabel(Serializable referenceDataModelId, String label) {
        ReferenceDataType.byReferenceDataModelIdAndLabel(referenceDataModelId, label).find()
    }

    @Override
    ReferenceDataType findByParentIdAndLabel(UUID parentId, String label) {
        ReferenceDataType.byReferenceDataModelIdAndLabel(parentId, label).get()
    }
}