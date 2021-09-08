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
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
abstract class ModelItemService<K extends ModelItem> extends CatalogueItemService<K> {

    public final static Integer BATCH_SIZE = 5000

    @Autowired(required = false)
    List<ModelItemService> modelItemServices

    void deleteAllByModelId(UUID modelId) {
        deleteAllByModelIds(Collections.singleton(modelId))
    }

    void deleteAllByModelIds(Set<UUID> modelIds) {
        throw new ApiNotYetImplementedException('MIS01', "deleteAllByModelIds for ${getDomainClass().simpleName}")
    }

    K validate(K modelItem) {
        throw new ApiNotYetImplementedException('MIS01', "validate for ${getDomainClass().simpleName}")
    }

    K copy(Model copiedModelInto, K original, CatalogueItem nonModelParent, UserSecurityPolicyManager userSecurityPolicyManager) {
        throw new ApiNotYetImplementedException('MIS03', "copy [for ModelItem ${getDomainClass().simpleName}]")
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
            log.debug('Straight saving {} already saved {}', alreadySaved.size(), getDomainClass().simpleName)
            getDomainClass().saveAll(alreadySaved)
        }

        if (notSaved) {
            if (batching) {
                log.debug('Batch saving {} new {} in batches of {}', notSaved.size(), getDomainClass().simpleName, BATCH_SIZE)
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
                log.debug('Straight saving {} new {}', notSaved.size(), getDomainClass().simpleName)
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
        log.debug('Performing batch save of {} {}', modelItems.size(), getDomainClass().simpleName)
        List<Boolean> inserts = modelItems.collect { !it.id }
        preBatchSaveHandling(modelItems)
        getDomainClass().saveAll(modelItems)
        modelItems.eachWithIndex { mi, i ->
            if (inserts[i]) updateFacetsAfterInsertingCatalogueItem(mi)
            checkBreadcrumbTreeAfterSavingCatalogueItem(mi)
        }

        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()

        log.debug('Batch save took {}', Utils.timeTaken(start))
    }

    void preBatchSaveHandling(List<K> modelItems) {
        //noop
    }

    @Override
    boolean isMultiFacetAwareFinalised(K multiFacetAwareItem) {
        multiFacetAwareItem.model.finalised
    }
}