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
package uk.ac.ox.softeng.maurodatamapper.core.model

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.merge.ObjectPatchData
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.merge.legacy.LegacyFieldPatchData
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
abstract class ModelItemService<K extends ModelItem> extends CatalogueItemService<K> {

    public final static Integer BATCH_SIZE = 5000

    @Autowired(required = false)
    List<ModelItemService> modelItemServices

    @Override
    Class<K> getCatalogueItemClass() {
        return getModelItemClass()
    }

    abstract Class<K> getModelItemClass()

    void deleteAllByModelId(UUID modelId) {
        deleteAllByModelIds(Collections.singleton(modelId))
    }

    void deleteAllByModelIds(Set<UUID> modelIds) {
        throw new ApiNotYetImplementedException('MIS01', "deleteAllByModelIds for ${getModelItemClass().simpleName}")
    }

    @Deprecated
    K copy(Model copiedModelInto, K original, UserSecurityPolicyManager userSecurityPolicyManager) {
        copy(copiedModelInto, original, null, userSecurityPolicyManager)
    }

    @Deprecated
    K copy(Model copiedModelInto, K original, UUID nonModelParentId, UserSecurityPolicyManager userSecurityPolicyManager) {
        throw new ApiNotYetImplementedException('MIS03', "copy [for ModelItem ${getModelItemClass().simpleName}] (with parent id)")
    }

    K copy(Model copiedModelInto, K original, CatalogueItem nonModelParent, UserSecurityPolicyManager userSecurityPolicyManager) {
        throw new ApiNotYetImplementedException('MIS03', "copy [for ModelItem ${getModelItemClass().simpleName}]")
    }

    Model mergeLegacyObjectPatchDataIntoModelItem(ObjectPatchData objectPatchData, K targetModelItem, Model targetModel,
                                                  UserSecurityPolicyManager userSecurityPolicyManager) {
        if (!objectPatchData.hasPatches()) return targetModel
        log.debug('Merging {} diffs into modelItem [{}]', objectPatchData.getDiffsWithContent().size(), targetModelItem.label)
        objectPatchData.getDiffsWithContent().each {mergeFieldDiff ->
            log.debug('{}', mergeFieldDiff.summary)

            if (mergeFieldDiff.isFieldChange()) {
                targetModelItem.setProperty(mergeFieldDiff.fieldName, mergeFieldDiff.value)
            } else if (mergeFieldDiff.isMetadataChange()) {
                mergeLegacyMetadataIntoCatalogueItem(mergeFieldDiff, targetModelItem, userSecurityPolicyManager)
            } else {
                ModelItemService modelItemService
                UUID parentId
                if (this.handles(mergeFieldDiff.fieldName)) {
                    modelItemService = this
                    parentId = targetModelItem.id
                } else {
                    modelItemService = modelItemServices.find { it.handles(mergeFieldDiff.fieldName) }
                    parentId = null
                }

                if (modelItemService) {
                    modelItemService.processLegacyFieldPatchData(mergeFieldDiff, targetModel, userSecurityPolicyManager, parentId)

                } else {
                    log.error('Unknown ModelItem field to merge [{}]', mergeFieldDiff.fieldName)
                }
            }
        }
        //Save the model item, this may break validation?
        save(flush: true, validate: false, targetModelItem)
        targetModel
    }

    void processLegacyFieldPatchData(LegacyFieldPatchData fieldPatchData, Model targetModel, UserSecurityPolicyManager userSecurityPolicyManager,
                                     UUID parentId = null) {
        // apply deletions of children to target object
        fieldPatchData.deleted.each {deletedItemPatchData ->
            ModelItem modelItem = get(deletedItemPatchData.id) as ModelItem
            delete(modelItem)
        }

        // copy additions from source to target object
        fieldPatchData.created.each {createdItemPatchData ->
            ModelItem modelItem = get(createdItemPatchData.id) as ModelItem
            ModelItem copyModelItem
            if (parentId) {
                copyModelItem = copy(targetModel, modelItem, parentId, userSecurityPolicyManager)
            } else {
                copyModelItem = copy(targetModel, modelItem, userSecurityPolicyManager)
            }
            save(copyModelItem)
        }

        // for modifications, recursively call this method
        fieldPatchData.modified.each {modifiedObjectPatchData ->
            ModelItem modelItem = get(modifiedObjectPatchData.targetId) as ModelItem
            mergeLegacyObjectPatchDataIntoModelItem(modifiedObjectPatchData, modelItem, targetModel, userSecurityPolicyManager)
        }
    }

    boolean isModelItemInSameModelOrInFinalisedModel(K modelItem, K otherModelItem) {
        otherModelItem.model.id == modelItem.model.id || modelItem.model.finalised
    }

    def saveAll(Collection<K> modelItems, boolean batching = true) {

        List<Classifier> classifiers = modelItems.collectMany { it.classifiers ?: [] } as List<Classifier>
        if (classifiers) {
            log.trace('Saving {} classifiers')
            classifierService.saveAll(classifiers)
        }

        Collection<K> alreadySaved = modelItems.findAll { it.ident() && it.isDirty() }
        Collection<K> notSaved = modelItems.findAll { !it.ident() }

        if (alreadySaved) {
            log.debug('Straight saving {} already saved {}', alreadySaved.size(), getModelItemClass().simpleName)
            getModelItemClass().saveAll(alreadySaved)
        }

        if (notSaved) {
            if (batching) {
                log.debug('Batch saving {} new {} in batches of {}', notSaved.size(), getModelItemClass().simpleName, BATCH_SIZE)
                List batch = []
                int count = 0

                notSaved.each { mi ->

                    batch << mi
                    count++
                    if (count % BATCH_SIZE == 0) {
                        batchSave(batch)
                        batch.clear()
                    }
                }
                batchSave(batch)
                batch.clear()
            } else {
                log.debug('Straight saving {} new {}', notSaved.size(), getModelItemClass().simpleName)
                notSaved.each { mi ->
                    save(flush: false, validate: false, mi)
                    updateFacetsAfterInsertingCatalogueItem(mi)
                    checkBreadcrumbTreeAfterSavingCatalogueItem(mi)
                }
            }
        }
    }

    void batchSave(List<K> modelItems) {
        long start = System.currentTimeMillis()
        log.debug('Performing batch save of {} {}', modelItems.size(), getModelItemClass().simpleName)
        List<Boolean> inserts = modelItems.collect { !it.id }
        getModelItemClass().saveAll(modelItems)
        modelItems.eachWithIndex { mi, i ->
            if (inserts[i]) updateFacetsAfterInsertingCatalogueItem(mi)
            checkBreadcrumbTreeAfterSavingCatalogueItem(mi)
        }

        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()

        log.debug('Batch save took {}', Utils.timeTaken(start))
    }

    @Override
    boolean isMultiFacetAwareFinalised(K multiFacetAwareItem) {
        multiFacetAwareItem.model.finalised
    }
}