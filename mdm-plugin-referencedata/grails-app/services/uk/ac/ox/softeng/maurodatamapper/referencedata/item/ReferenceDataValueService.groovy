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


import uk.ac.ox.softeng.maurodatamapper.core.traits.service.DomainService
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

import org.hibernate.SessionFactory

@Slf4j
@Transactional
class ReferenceDataValueService implements DomainService<ReferenceDataValue> {

    SessionFactory sessionFactory

    @Override
    ReferenceDataValue get(Serializable id) {
        ReferenceDataValue.get(id)
    }

    Long count() {
        ReferenceDataValue.count()
    }

    @Override
    List<ReferenceDataValue> list(Map args) {
        ReferenceDataValue.list(args)
    }

    //@Override
    List<ReferenceDataValue> getAll(Collection<UUID> ids) {
        ReferenceDataValue.getAll(ids).findAll()
    }

    //@Override
    void deleteAll(Collection<ReferenceDataValue> referenceDataValues) {
        referenceDataValues.each { delete(it) }
    }

    void delete(UUID id) {
        delete(get(id), true)
    }

    void delete(ReferenceDataValue referenceDataValue, boolean flush = false) {
        if (!referenceDataValue) return
        referenceDataValue.breadcrumbTree.removeFromParent()
        referenceDataValue.referenceDataElement = null
        referenceDataValue.referenceDataModel?.removeFromReferenceDataValues(referenceDataValue)
        referenceDataValue.delete(flush: flush)
    }

    def saveAll(Collection<ReferenceDataValue> referenceDataValues) {
        Collection<ReferenceDataValue> alreadySaved = referenceDataValues.findAll { it.ident() && it.isDirty() }
        Collection<ReferenceDataValue> notSaved = referenceDataValues.findAll { !it.ident() }

        if (alreadySaved) {
            log.debug('Straight saving {} already saved ReferenceDataValues', alreadySaved.size())
            ReferenceDataValue.saveAll(alreadySaved)
        }

        if (notSaved) {
            log.debug('Batch saving {} new ReferenceDataValues in batches of {}', notSaved.size(), ReferenceDataValue.BATCH_SIZE)
            List batch = []
            int count = 0

            notSaved.each { rdv ->
                batch += rdv
                count++
                if (count % ReferenceDataValue.BATCH_SIZE == 0) {
                    batchSave(batch)
                    batch.clear()
                }

            }
            batchSave(batch)
            batch.clear()
        }
    }

    void batchSave(List<ReferenceDataValue> referenceDataValues) {
        long start = System.currentTimeMillis()
        log.debug('Performing batch save of {} ReferenceDataValues', referenceDataValues.size())

        ReferenceDataValue.saveAll(referenceDataValues)

        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()

        log.debug('Batch save took {}', Utils.timeTaken(start))
    }    

    List<ReferenceDataValue> findAllByReferenceDataModelId(Serializable referenceDataModelId, Map pagination = [:]) {
        findAllByReferenceDataModelId(referenceDataModelId, pagination, pagination)
    }

    List<ReferenceDataValue> findAllByReferenceDataModelId(Serializable referenceDataModelId, Map filter, Map pagination) {
        ReferenceDataValue.withFilter(ReferenceDataValue.byReferenceDataModelId(referenceDataModelId), filter).list(pagination)
    }

    List<ReferenceDataValue> findAllByReferenceDataModelIdAndRowNumber(Serializable referenceDataModelId, Integer fromRowNumber, Integer toRowNumber, Map params) {
        ReferenceDataValue.withFilter(ReferenceDataValue.byReferenceDataModelIdAndRowNumber(referenceDataModelId, fromRowNumber, toRowNumber), params).list()
    }

    List<ReferenceDataValue> findAllByReferenceDataModelIdAndRowNumberIn(Serializable referenceDataModelId, List rowNumbers, Map params = [:]) {
        ReferenceDataValue.withFilter(ReferenceDataValue.byReferenceDataModelIdAndRowNumberIn(referenceDataModelId, rowNumbers), params).list(params)
    }    

    Integer countRowsByReferenceDataModelId(Serializable referenceDataModelId) {
        ReferenceDataValue.countByReferenceDataModelId(referenceDataModelId).list()[0]
    }

    List<Integer> findDistinctRowNumbersByReferenceDataModelIdAndValueIlike(Serializable referenceDataModelId, String valueSearch) {
        ReferenceDataValue.distinctRowNumbersByReferenceDataModelIdAndValueIlike(referenceDataModelId, valueSearch).list()
    }

    List<ReferenceDataValue> findAllByReferenceDataModelIdAndValueIlike(Serializable referenceDataModelId, String valueSearch, Map pagination = [:]) {
        ReferenceDataValue.byReferenceDataModelIdAndValueIlike(referenceDataModelId, valueSearch).list(pagination)
    }

    void checkImportedReferenceDataValueAssociations(User importingUser, ReferenceDataModel referenceDataModel, ReferenceDataValue referenceDataValue) {
        referenceDataModel.addToReferenceDataValues(referenceDataValue)
        referenceDataValue.createdBy = importingUser.emailAddress

        //Get the reference data element for this value by getting the matching reference data element for the model
        referenceDataValue.referenceDataElement = referenceDataModel.referenceDataElements.find {it.label == referenceDataValue.referenceDataElement.label}
    }    
}