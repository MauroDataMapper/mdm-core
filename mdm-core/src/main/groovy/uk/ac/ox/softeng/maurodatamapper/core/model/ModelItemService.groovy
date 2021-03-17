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
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.MergeFieldDiffData
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.MergeObjectDiffData
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
abstract class ModelItemService<K extends ModelItem> extends CatalogueItemService<K> {

    @Autowired(required = false)
    List<ModelItemService> modelItemServices

    @Override
    Class<K> getCatalogueItemClass() {
        return getModelItemClass()
    }

    abstract Class<K> getModelItemClass()

    void deleteAllByModelId(UUID modelId) {
        throw new ApiNotYetImplementedException('MIS01', "deleteAllByModelId for ${getModelItemClass().simpleName}")
    }

    K copy(Model copiedModelInto, K original, UserSecurityPolicyManager userSecurityPolicyManager) {
        throw new ApiNotYetImplementedException('MIS02', "copy [for ModelItem ${getModelItemClass().simpleName}]")
    }

    K copy(Model copiedModelInto, K original, UserSecurityPolicyManager userSecurityPolicyManager, UUID parentId) {
        throw new ApiNotYetImplementedException('MIS03', "copy [for ModelItem ${getModelItemClass().simpleName}] (with parent id)")
    }

    Model mergeObjectDiffIntoModelItem(MergeObjectDiffData mergeObjectDiff, K targetModelItem, Model targetModel,
                                       UserSecurityPolicyManager userSecurityPolicyManager) {
        //TODO validation on saving merges
        if (!mergeObjectDiff.hasDiffs()) return targetModel
        log.debug('Merging {} diffs into modelItem [{}]', mergeObjectDiff.getValidDiffs().size(), targetModelItem.label)
        mergeObjectDiff.getValidDiffs().each {mergeFieldDiff ->
            log.debug('{}', mergeFieldDiff.summary)

            if (mergeFieldDiff.isFieldChange()) {
                targetModelItem.setProperty(mergeFieldDiff.fieldName, mergeFieldDiff.value)
            } else if (mergeFieldDiff.isMetadataChange()) {
                mergeMetadataIntoCatalogueItem(mergeFieldDiff, targetModelItem, userSecurityPolicyManager)
            } else {
                ModelItemService modelItemService
                UUID parentId
                if (this.handles(mergeFieldDiff.fieldName)) {
                    modelItemService = this
                    parentId = targetModelItem.id
                } else {
                    modelItemService = modelItemServices.find {it.handles(mergeFieldDiff.fieldName)}
                    parentId = null
                }

                if (modelItemService) {
                    modelItemService.processMergeFieldDiff(mergeFieldDiff, targetModel, userSecurityPolicyManager, parentId)

                } else {
                    log.error('Unknown ModelItem field to merge [{}]', mergeFieldDiff.fieldName)
                }
            }
        }
        //Save the model item, this may break validation?
        save(flush: true, validate: false, targetModelItem)
        targetModel
    }

    void processMergeFieldDiff(MergeFieldDiffData mergeFieldDiff, Model targetModel, UserSecurityPolicyManager userSecurityPolicyManager,
                               UUID parentId = null) {
        // apply deletions of children to target object
        mergeFieldDiff.deleted.each {mergeItemData ->
            ModelItem modelItem = get(mergeItemData.id) as ModelItem
            delete(modelItem)
        }

        // copy additions from source to target object
        mergeFieldDiff.created.each {mergeItemData ->
            ModelItem modelItem = get(mergeItemData.id) as ModelItem
            ModelItem copyModelItem
            if (parentId) {
                copyModelItem = copy(targetModel, modelItem, userSecurityPolicyManager, parentId)
            } else {
                copyModelItem = copy(targetModel, modelItem, userSecurityPolicyManager)
            }
            save(copyModelItem)
        }

        // for modifications, recursively call this method
        mergeFieldDiff.modified.each {mergeObjectDiffData ->
            ModelItem modelItem = get(mergeObjectDiffData.leftId) as ModelItem
            mergeObjectDiffIntoModelItem(mergeObjectDiffData, modelItem, targetModel, userSecurityPolicyManager)
        }
    }
}