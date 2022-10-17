/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.core.model

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.ClassifierService
import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffCache
import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.AnnotationService
import uk.ac.ox.softeng.maurodatamapper.core.facet.BreadcrumbTree
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFileService
import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.facet.RuleService
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkService
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.CopyInformation
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MdmDomainService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MultiFacetAwareService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager

import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.hibernate.SessionFactory

import static org.grails.orm.hibernate.cfg.GrailsHibernateUtil.ARGUMENT_IGNORE_CASE
import static org.grails.orm.hibernate.cfg.GrailsHibernateUtil.ARGUMENT_ORDER
import static org.grails.orm.hibernate.cfg.GrailsHibernateUtil.ORDER_ASC
import static org.grails.orm.hibernate.cfg.GrailsHibernateUtil.ORDER_DESC

@Slf4j
abstract class CatalogueItemService<K extends CatalogueItem> implements MdmDomainService<K>, MultiFacetAwareService<K> {

    SessionFactory sessionFactory
    ClassifierService classifierService
    MetadataService metadataService
    RuleService ruleService
    SemanticLinkService semanticLinkService
    AnnotationService annotationService
    ReferenceFileService referenceFileService

    Class<K> getMultiFacetAwareClass() {
        getDomainClass()
    }

    abstract void deleteAll(Collection<K> catalogueItems)

    /**
     * Use domain.getAll(ids) to retrieve objects from the database.
     *
     * Make sure you use findAll() on the output of this, its possible to get ids which dont exist in this domain and the Grails implementation
     * of getAll(ids) will return a list of null elements
     * @param ids
     * @return
     */
    abstract List<K> getAll(Collection<UUID> ids)

    abstract K findByIdJoinClassifiers(UUID id)

    abstract void removeAllFromClassifier(Classifier classifier)

    abstract List<K> findAllByClassifier(Classifier classifier)

    abstract List<K> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier)

    abstract List<K> findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                                String searchTerm, String domainType)

    abstract Boolean shouldPerformSearchForTreeTypeCatalogueItems(String domainType)

    @Override
    K save(Map args, K catalogueItem) {
        Map saveArgs = new HashMap(args)
        if (args.flush) {
            saveArgs.remove('flush')
            (catalogueItem as GormEntity).save(saveArgs)
            // We do need to ensure the BT hasnt changed (e.g. a move of a MI inside an M)
            if (!args.ignoreBreadcrumbs) checkBreadcrumbTreeAfterSavingCatalogueItem(catalogueItem)
            sessionFactory.currentSession.flush()
        } else {
            (catalogueItem as GormEntity).save(args)
            // We do need to ensure the BT hasnt changed (e.g. a move of a MI inside an M)
            if (!args.ignoreBreadcrumbs) checkBreadcrumbTreeAfterSavingCatalogueItem(catalogueItem)
        }
        catalogueItem
    }

    boolean hasTreeTypeModelItems(K catalogueItem) {
        hasTreeTypeModelItems(catalogueItem, false)
    }

    boolean isCatalogueItemImportedIntoCatalogueItem(CatalogueItem catalogueItem, K owningCatalogueItem) {
        false
    }

    boolean hasTreeTypeModelItems(K catalogueItem, boolean fullTreeRender) {
        hasTreeTypeModelItems(catalogueItem, fullTreeRender, false)
    }

    boolean hasTreeTypeModelItems(K catalogueItem, boolean fullTreeRender, boolean includeImportedItems) {
        false
    }

    List<ModelItem> findAllTreeTypeModelItemsIn(K catalogueItem) {
        findAllTreeTypeModelItemsIn(catalogueItem, false)
    }

    List<ModelItem> findAllTreeTypeModelItemsIn(K catalogueItem, boolean fullTreeRender) {
        findAllTreeTypeModelItemsIn(catalogueItem, fullTreeRender, false)
    }

    List<ModelItem> findAllTreeTypeModelItemsIn(K catalogueItem, boolean fullTreeRender, boolean includeImportedItems) {
        []
    }

    void addClassifierToCatalogueItem(UUID catalogueItemId, Classifier classifier) {
        get(catalogueItemId).addToClassifiers(classifier)
    }

    void removeClassifierFromCatalogueItem(UUID catalogueItemId, Classifier classifier) {
        removeFacetFromDomain(catalogueItemId, classifier.id, 'classifiers')
    }

    K copyCatalogueItemInformation(K original, K copy, User copier, UserSecurityPolicyManager userSecurityPolicyManager,
                                   CopyInformation copyInformation = null) {
        copy.createdBy = copier.emailAddress
        copy.description = original.description

        // Allow copying with a new label
        if (copyInformation && copyInformation.validate()) {
            copy.label = copyInformation.copyLabel
        } else {
            copy.label = original.label
        }

        classifierService.findAllReadableByCatalogueItem(userSecurityPolicyManager, original).each {copy.addToClassifiers(it)}

        // Allow facets to be preloaded from the db and passed in via the copy information
        // Facets loaded in this way could be more than just those belonging to the item being copied so we need to extract only those relevant
        List<Metadata> metadata
        List<Rule> rules
        List<SemanticLink> semanticLinks
        if (copyInformation) {
            metadata =
                copyInformation.hasFacetData('metadata') ? copyInformation.extractPreloadedFacetsForTypeAndId(Metadata, 'metadata', original.id) :
                metadataService.findAllByMultiFacetAwareItemId(original.id)
            rules = copyInformation.hasFacetData('rules') ? copyInformation.extractPreloadedFacetsForTypeAndId(Rule, 'rules', original.id) :
                    ruleService.findAllByMultiFacetAwareItemId(original.id)
            semanticLinks = copyInformation.hasFacetData('semanticLinks') ?
                            copyInformation.extractPreloadedFacetsForTypeAndId(SemanticLink, 'semanticLinks', original.id) :
                            semanticLinkService.findAllBySourceMultiFacetAwareItemId(original.id)
        } else {
            metadata = metadataService.findAllByMultiFacetAwareItemId(original.id)
            rules = ruleService.findAllByMultiFacetAwareItemId(original.id)
            semanticLinks = semanticLinkService.findAllBySourceMultiFacetAwareItemId(original.id)
        }

        metadata.each {copy.addToMetadata(it.namespace, it.key, it.value, copier.emailAddress)}
        rules.each {rule ->
            Rule copiedRule = new Rule(name: rule.name, description: rule.description, createdBy: copier.emailAddress)
            rule.ruleRepresentations.each {ruleRepresentation ->
                copiedRule.addToRuleRepresentations(language: ruleRepresentation.language,
                                                    representation: ruleRepresentation.representation,
                                                    createdBy: copier.emailAddress)
            }
            copy.addToRules(copiedRule)
        }
        semanticLinks.each {link ->
            if (link.targetMultiFacetAwareItem) {
                copy.addToSemanticLinks(createdBy: copier.emailAddress, linkType: link.linkType,
                                        targetMultiFacetAwareItem: link.targetMultiFacetAwareItem,
                                        unconfirmed: true)
            } else {
                copy.addToSemanticLinks(createdBy: copier.emailAddress, linkType: link.linkType,
                                        targetMultiFacetAwareItemId: link.targetMultiFacetAwareItemId,
                                        targetMultiFacetAwareItemDomainType: link.targetMultiFacetAwareItemDomainType,
                                        unconfirmed: true)
            }

        }
        copy
    }

    void propagateDataFromPreviousVersion(K catalogueItem, K previousVersionCatalogueItem) {
        K previousVersion = unwrapIfProxy(previousVersionCatalogueItem)
        if (!catalogueItem.description) catalogueItem.description = previousVersion.description

        //propagate generic facet information
        propagateFacetInformation(catalogueItem, previousVersion)

        propagateContentsInformation(catalogueItem, previousVersion)
    }

    K propagateFacetInformation(K catalogueItem, K previousCatalogueItem) {
        propagateCatalogueItemClassifiers(catalogueItem, previousCatalogueItem)
        propagateCatalogueItemMetadata(catalogueItem, previousCatalogueItem)
        propagateCatalogueItemRules(catalogueItem, previousCatalogueItem)
        propagateCatalogueItemSemanticLinks(catalogueItem, previousCatalogueItem)
        propagateCatalogueItemAnnotations(catalogueItem, previousCatalogueItem)
        propagateCatalogueItemReferenceFiles(catalogueItem, previousCatalogueItem)
        catalogueItem
    }

    void propagateCatalogueItemClassifiers(K catalogueItem, K previousCatalogueItem) {
        //if a classifier does not exists with the same label add it to catalogueItem
        previousCatalogueItem.classifiers.each {previous ->
            if (catalogueItem.classifiers.any {it.label == previous.label}) return
            catalogueItem.addToClassifiers(previous)
        }
    }

    void propagateCatalogueItemMetadata(K catalogueItem, K previousCatalogueItem) {
        metadataService.findAllByMultiFacetAwareItemId(previousCatalogueItem.id).each {previous ->
            if (catalogueItem.metadata.any {it.key == previous.key}) return
            catalogueItem.addToMetadata(previous.namespace, previous.key, previous.value, previous.createdBy)
        }

    }

    void propagateCatalogueItemRules(K catalogueItem, K previousCatalogueItem) {

        ruleService.findAllByMultiFacetAwareItemId(previousCatalogueItem.id).each {previousRule ->

            //if rule exists, check and copy missing representations
            Rule existingRule = catalogueItem.rules.find {it.name == previousRule.name}
            if (existingRule) {
                previousRule.ruleRepresentations.each {previousRuleRepresentation ->
                    if (existingRule.ruleRepresentations.any {it.representation == previousRuleRepresentation.representation}) return
                    existingRule.addToRuleRepresentations(language: previousRuleRepresentation.language,
                                                          representation: previousRuleRepresentation.representation,
                                                          createdBy: previousRuleRepresentation.createdBy)
                }
            } else {

                //if rule does not exist, copy rule first and then representations
                Rule copiedRule = new Rule(name: previousRule.name, description: previousRule.description, createdBy: previousRule.createdBy)
                previousRule.ruleRepresentations.each {ruleRepresentation ->
                    copiedRule.addToRuleRepresentations(language: ruleRepresentation.language,
                                                        representation: ruleRepresentation.representation,
                                                        createdBy: ruleRepresentation.createdBy)
                }
                catalogueItem.addToRules(copiedRule)
            }
        }

    }

    void propagateCatalogueItemSemanticLinks(CatalogueItem catalogueItem, CatalogueItem previousCatalogueItem) {

        semanticLinkService.findAllBySourceMultiFacetAwareItemId(previousCatalogueItem.id).each {previousLink ->

            if (catalogueItem.semanticLinks.any {
                semanticLinkService.areLinksIdenticalBetweenSameSourceLabelAndTargetItem(it, catalogueItem.label, previousLink, previousCatalogueItem.label)
            }) return

            catalogueItem.addToSemanticLinks(createdBy: previousLink.createdBy, linkType: previousLink.linkType,
                                             targetMultiFacetAwareItemId: previousLink.targetMultiFacetAwareItemId,
                                             targetMultiFacetAwareItemDomainType: previousLink.targetMultiFacetAwareItemDomainType,
                                             unconfirmed: true)
        }

    }

    void propagateCatalogueItemAnnotations(CatalogueItem catalogueItem, CatalogueItem previousCatalogueItem) {

        previousCatalogueItem.annotations.each {previous ->
            //if it exists
            Annotation modelAnnotation = catalogueItem.annotations.find {it.label == previous.label}
            if (modelAnnotation) {
                //copy across child annotations
                previous.childAnnotations.each {previousChild ->
                    if (modelAnnotation.childAnnotations.any {it.label == previousChild.label}) return
                    modelAnnotation.addToChildAnnotations(label: previousChild.label, description: previousChild.description,
                                                          createdBy: previousChild.createdBy, parentAnnotation: modelAnnotation)

                }
            } else {
                //if not copy whole thing
                catalogueItem.addToAnnotations(label: previous.label, description: previous.description,
                                               createdBy: previous.createdBy, childAnnotations: previous.childAnnotations)
            }
        }
    }

    void propagateCatalogueItemReferenceFiles(CatalogueItem catalogueItem, CatalogueItem previousCatalogueItem) {
        previousCatalogueItem.referenceFiles.each {previous ->
            if (catalogueItem.referenceFiles.any {it.fileName == previous.fileName}) return
            catalogueItem.addToReferenceFiles(fileName: previous.fileName, fileType: previous.fileType, fileContents: previous.fileContents,
                                              fileSize: previous.fileSize, createdBy: previous.createdBy)

        }
    }

    void propagateContentsInformation(K catalogueItem, K previousVersionCatalogueItem) {
        // default no-op
    }

    void setCatalogueItemRefinesCatalogueItem(CatalogueItem source, CatalogueItem target, User catalogueUser) {
        source.addToSemanticLinks(linkType: SemanticLinkType.REFINES, createdBy: catalogueUser.emailAddress, targetMultiFacetAwareItem: target)
    }

    void checkBreadcrumbTreeAfterSavingCatalogueItem(K catalogueItem) {
        catalogueItem.breadcrumbTree?.trackChanges()
        catalogueItem.breadcrumbTree?.beforeValidateCheck()
        catalogueItem.breadcrumbTree?.save(validate: false)
    }

    void checkBreadcrumbTreeAfterSavingCatalogueItems(Collection<K> catalogueItems) {
        List<BreadcrumbTree> bts = catalogueItems.collect {it.breadcrumbTree}.findAll().each {
            boolean skip = it.shouldSkipValidation()
            if(skip) it.skipValidation(false)
            it.trackChanges()
            it.beforeValidateCheck(false)
            if(skip) it.skipValidation(true)
        }
        BreadcrumbTree.saveAll(bts)
    }

    K checkFacetsAfterImportingCatalogueItem(K catalogueItem) {
        checkFacetsAfterImportingMultiFacetAware(catalogueItem)
        catalogueItem
    }

    /*
     * Find a CatalogueItem which is labeled with label and whose parent is parentCatalogueItem.
     * @param parentCatalogueItem The CatalogueItem which is the parent of the CatalogueItem being sought
     * @param label The label of the CatalogueItem being sought
     */

    K findByParentAndLabel(CatalogueItem parentCatalogueItem, String label) {
        findByParentIdAndLabel(parentCatalogueItem.id, label)
    }

    K findByParentIdAndLabel(UUID parentId, String label) {
        null
    }

    @Override
    K findByParentIdAndPathIdentifier(UUID parentId, String pathIdentifier, Map pathParams = [:]) {
        findByParentIdAndLabel(parentId, pathIdentifier)
    }

    void createCatalogueItemDiffCaches(DiffCache diffCache, String field, List<CatalogueItem> catalogueItems,
                                       Map<String, Map<UUID, List<Diffable>>> facetData) {
        catalogueItems.each {ci ->
            createCatalogueItemDiffCache(diffCache, ci, facetData)
        }
        diffCache.addField(field, catalogueItems)
    }

    DiffCache createCatalogueItemDiffCache(DiffCache parentCache, CatalogueItem catalogueItem,
                                           Map<String, Map<UUID, List<Diffable>>> facetData) {
        DiffCache ciDiffCache = new DiffCache()
        addFacetDataToDiffCache(ciDiffCache, facetData, catalogueItem.id)
        if(parentCache) parentCache.addDiffCache(catalogueItem.path, ciDiffCache)
        ciDiffCache
    }


    String applyHQLSort(String originalQuery, String ciQueryPrefix, def sortObj, Map otherArgs, boolean isDistinct) {
        if (sortObj == null) return originalQuery

        boolean ignoreCase = true
        Object caseArg = otherArgs[ARGUMENT_IGNORE_CASE]
        if (caseArg instanceof Boolean) {
            ignoreCase = (Boolean) caseArg
        }
        String orderParam = (String) otherArgs[ARGUMENT_ORDER]
        PersistentEntity persistentEntity = getDomainClass().getGormPersistentEntity()
        StringBuilder sortedQuery = new StringBuilder(originalQuery)
        if (sortObj instanceof Map) {
            sortedQuery.append '\nORDER BY '
            Map sortMap = (Map) sortObj
            Set keys = sortMap.keySet()
            for (i in 0..<keys.size()) {
                String sort = keys[i]
                String order = ORDER_DESC.equalsIgnoreCase(sortMap[sort] as String) ? ORDER_DESC : ORDER_ASC

                PersistentProperty persistentProperty = persistentEntity.getPropertyByName(sort)
                // Dont try and 'lower' non character fields
                if (ignoreCase && CharSequence.isAssignableFrom(persistentProperty.type)) {
                    sortedQuery = addLowercaseSort(sortedQuery, ciQueryPrefix, sort, order, isDistinct, false)
                } else sortedQuery.append(ciQueryPrefix).append('.').append(sort).append(' ').append(order)
                if (i < keys.size() - 1) sortedQuery.append(', ')
            }
            sortMap.each {sort, providedOrder ->

            }
        } else {
            String sort = (String) sortObj
            String order = ORDER_DESC.equalsIgnoreCase(orderParam) ? ORDER_DESC : ORDER_ASC
            PersistentProperty persistentProperty = persistentEntity.getPropertyByName(sort)
            // Dont try and 'lower' non character fields
            if (ignoreCase && CharSequence.isAssignableFrom(persistentProperty.type)) {
                sortedQuery = addLowercaseSort(sortedQuery, ciQueryPrefix, sort, order, isDistinct, true)
            } else sortedQuery.append('\nORDER BY ').append(ciQueryPrefix).append('.').append(sort).append(' ').append(order)
        }
        sortedQuery.toString()
    }

    private static StringBuilder addLowercaseSort(StringBuilder stringBuilder, String ciQueryPrefix, String sort, String order, boolean isDistinct, boolean isSingle) {
        StringBuilder sortedQuery = new StringBuilder()
        // Need to add the lower variant to the select query if isDistinct
        if (isDistinct) sortedQuery.append('\n, lower(').append(ciQueryPrefix).append('.').append(sort).append(') ')
        sortedQuery.append(stringBuilder.toString())
        if (isSingle) sortedQuery.append('\nORDER BY ')
        sortedQuery.append('lower(').append(ciQueryPrefix).append('.').append(sort).append(') ').append(order)
    }

    String applyHQLFilters(String originalQuery, String ciQueryPrefix, Map filters) {
        StringBuilder filteredQuery = new StringBuilder(originalQuery)
        if (filters.label) filteredQuery.append('\nAND lower(').append(ciQueryPrefix).append('.label) LIKE lower(:label)')
        if (filters.description) filteredQuery.append('\nAND lower(').append(ciQueryPrefix).append('.description) LIKE lower(:description)')
        if (filters.domainType) filteredQuery.append('\nAND lower(').append(ciQueryPrefix).append('.domainType) LIKE lower(:domainType)')
        filteredQuery.toString()
    }

    Map<String, Object> extractFiltersAsHQLParameters(Map filters, String... addtlFilters) {
        Map<String, Object> extractedFilters = [:]
        ['label', 'description', 'domainType'].each {f ->
            if (filters.containsKey(f)) {
                extractedFilters[f] = "%${filters[f]}%".toString()
            }
        }

        if (addtlFilters) {
            addtlFilters.each {f ->
                if (filters.containsKey(f)) {
                    extractedFilters[f] = "%${filters[f]}%".toString()
                }
            }
        }
        extractedFilters
    }
}
