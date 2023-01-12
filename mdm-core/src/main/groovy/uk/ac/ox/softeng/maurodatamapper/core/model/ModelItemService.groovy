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
package uk.ac.ox.softeng.maurodatamapper.core.model

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.CopyInformation
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import groovy.util.logging.Slf4j
import org.hibernate.Session
import org.hibernate.search.mapper.orm.Search
import org.hibernate.search.mapper.orm.session.SearchSession
import org.hibernate.search.mapper.orm.work.SearchIndexingPlan
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

    K copy(Model copiedModelInto, K original, CatalogueItem nonModelParent, UserSecurityPolicyManager userSecurityPolicyManager) {
        throw new ApiNotYetImplementedException('MIS03', "copy [for ModelItem ${getDomainClass().simpleName}]")
    }

    K copyModelItemInformation(K original, K copy, User copier, UserSecurityPolicyManager userSecurityPolicyManager,
                               CopyInformation copyInformation = null) {
        copy = super.copyCatalogueItemInformation(original, copy, copier, userSecurityPolicyManager, copyInformation)
        if (copyInformation?.copyIndex) {
            copy.idx = original.idx
        }
        copy
    }

    boolean isModelItemInSameModelOrInFinalisedModel(K modelItem, K otherModelItem) {
        otherModelItem.model.id == modelItem.model.id || modelItem.model.finalised
    }

    def saveAll(Collection<K> modelItems) {
        saveAll(modelItems, true)
    }

    def saveAll(Collection<K> modelItems, Integer batchSize) {
        saveAll(modelItems, batchSize, true)
    }

    def saveAll(Collection<K> modelItems, boolean batching) {
        saveAll(modelItems, BATCH_SIZE, batching)
    }

    def saveAll(Collection<K> modelItems, Integer maxBatchSize, boolean batching) {
        if (!modelItems) return []
        List<Classifier> classifiers = modelItems.collectMany {it.classifiers ?: []} as List<Classifier>
        if (classifiers) {
            log.trace('Saving {} classifiers')
            classifierService.saveAll(classifiers)
        }

        Collection<K> alreadySaved = modelItems.findAll {it.ident() && it.isDirty()}
        Collection<K> notSaved = modelItems.findAll {!it.ident()}

        Map<String, Object> gatheredContentsMap = [:]

        if (alreadySaved) {
            log.debug('Straight saving {} already saved {}', alreadySaved.size(), getDomainClass().simpleName)
            getDomainClass().saveAll(alreadySaved)
        }

        if (notSaved) {
            if (batching) {
                // Determine the ideal batch size and then use the smaller of the 2 values of the requested batch size and the ideal one
                int idealBatchSize = determineIdealBatchSize(notSaved)
                int batchSizeToUse = Math.min(maxBatchSize, idealBatchSize)
                log.debug('Batch saving {} new {} in batches of {}', notSaved.size(), getDomainClass().simpleName, batchSizeToUse)
                List batch = []
                int count = 0

                notSaved.each {mi ->

                    gatherContents(gatheredContentsMap, mi)

                    batch << mi
                    count++
                    if (count % batchSizeToUse == 0) {
                        batchSave(batch)
                        batch.clear()
                    }
                }
                batchSave(batch)
                batch.clear()
            } else {
                log.debug('Straight saving {} new {}', notSaved.size(), getDomainClass().simpleName)
                notSaved.each {mi ->
                    save(flush: false, validate: false, mi)
                    checkBreadcrumbTreeAfterSavingCatalogueItem(mi)
                }
            }
        }
        returnGatheredContents(gatheredContentsMap)
    }

    void batchSave(List<K> modelItems) {

        long start = System.currentTimeMillis()
        log.trace('Performing batch save of {} {}', modelItems.size(), getDomainClass().simpleName)
        preBatchSaveHandling(modelItems)

        Metadata.saveAll(modelItems.collectMany {it.metadata ?: []}.findAll())
        getDomainClass().saveAll(modelItems)
        checkBreadcrumbTreeAfterSavingCatalogueItems(modelItems)

        // Flush, clear session and write indexes to keep indexing document small
        Session currentSession = sessionFactory.currentSession
        currentSession.flush()
        currentSession.clear()
        SearchSession searchSession = Search.session(currentSession)
        SearchIndexingPlan indexingPlan = searchSession.indexingPlan()
        indexingPlan.execute()

        log.trace('Batch save of {} {} took {}', modelItems.size(), getDomainClass().simpleName, Utils.timeTaken(start))
    }

    def returnGatheredContents(Map<String, Object> gatheredContents) {
        null
    }

    void gatherContents(Map<String, Object> gatheredContents, K modelItem) {
        //no-op
    }

    void preBatchSaveHandling(List<K> modelItems) {
        //noop
    }

    int determineIdealBatchSize(Collection<K> modelItems) {
        // Batch size of ~5K seems to be perfect for our system but when you have lots of MD we need to reduce the batch size
        // to something which will result in ~5K objects being saved in each batch
        List<Integer> mdSizes = modelItems.collect {it.metadata?.size() ?: 0}
        Integer avgMetadataSize = (mdSizes.average() as BigDecimal).toInteger()
        Map<Integer, Integer> sizeCounts = mdSizes.groupBy {it}.collectEntries {k, v -> [k, v.size()]}
        int max = sizeCounts.max {it.value}.value
        Integer modeMetadataSize = sizeCounts.findAll {it.value == max}.max {it.key}.key
        int avgObjsPerBatch = 1 + Math.max(avgMetadataSize, modeMetadataSize)
        // Give some allowance back for varying obj per batch plus technically each MI will include a BT therefore we're usually saving 10K objects
        BigDecimal[] result = (BATCH_SIZE * 2).toBigDecimal().divideAndRemainder(avgObjsPerBatch.toBigDecimal())
        int numberOfModelItemsPerBatch = (result[1] ? result[0] + 1 : result[0]).toInteger()
        Math.min(BATCH_SIZE, numberOfModelItemsPerBatch)
    }

    @Override
    boolean isMultiFacetAwareFinalised(K multiFacetAwareItem) {
        multiFacetAwareItem.model.finalised
    }
}