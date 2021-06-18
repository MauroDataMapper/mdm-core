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
package uk.ac.ox.softeng.maurodatamapper.core.container

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItemService
import uk.ac.ox.softeng.maurodatamapper.core.model.ContainerService
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.gorm.PaginatedResultList
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

@Transactional
@Slf4j
class ClassifierService extends ContainerService<Classifier> {

    @Autowired(required = false)
    List<CatalogueItemService> catalogueItemServices

    @Override
    boolean handles(Class clazz) {
        clazz == Classifier
    }

    @Override
    boolean handles(String domainType) {
        domainType == Classifier.simpleName
    }

    @Override
    Class<Classifier> getContainerClass() {
        Classifier
    }

    @Override
    boolean isContainerVirtual() {
        true
    }

    @Override
    String getContainerPropertyNameInModel() {
        'classifiers'
    }

    @Override
    List<Classifier> getAll(Collection<UUID> containerIds) {
        Classifier.getAll(containerIds)
    }

    @Override
    List<Classifier> findAllReadableByEveryone() {
        Classifier.findAllByReadableByEveryone(true)
    }

    @Override
    List<Classifier> findAllReadableByAuthenticatedUsers() {
        Classifier.findAllByReadableByAuthenticatedUsers(true)
    }

    Classifier get(Serializable id) {
        Classifier.get(id)
    }

    List<Classifier> list(Map pagination = [:]) {
        Classifier.list(pagination)
    }

    @Override
    List<Classifier> findAllContainersInside(UUID containerId) {
        Classifier.findAllContainedInClassifierId(containerId)
    }

    @Override
    Classifier findDomainByLabel(String label) {
        return null
    }

    @Override
    Classifier findDomainByParentIdAndLabel(UUID parentId, String label) {
        return null
    }

    @Override
    List<Classifier> findAllByParentId(UUID parentId) {
        return null
    }

    @Override
    List<Classifier> findAllByParentId(UUID parentId, Map pagination) {
        return null
    }

    @Override
    DetachedCriteria<Classifier> getCriteriaByParent(Classifier domain) {
        return null
    }

    @Override
    List<Classifier> findAllReadableContainersBySearchTerm(UserSecurityPolicyManager userSecurityPolicyManager, String searchTerm) {
        log.debug('Searching readable classifiers for search term in label')
        List<UUID> readableIds = userSecurityPolicyManager.listReadableSecuredResourceIds(Classifier)
        Classifier.luceneTreeLabelSearch(readableIds.collect {it.toString()}, searchTerm)
    }

    @Override
    List<Classifier> findAllWhereDirectParentOfModel(Model model) {
        []
    }

    @Override
    List<Classifier> findAllWhereDirectParentOfContainer(Classifier classifier) {
        List<Classifier> classifiers = []
        if (classifier.parentClassifier) {
            classifiers << classifier.parentClassifier
            classifiers.addAll(findAllWhereDirectParentOfContainer(classifier.parentClassifier))
        }
        classifiers
    }

    @Override
    List<Classifier> findAllByMetadataNamespaceAndKey(String namespace, String key, Map pagination = [:]) {
        Classifier.byMetadataNamespaceAndKey(namespace, key).list(pagination)
    }

    @Override
    List<Classifier> findAllByMetadataNamespace(String namespace, Map pagination = [:]) {
        Classifier.byMetadataNamespace(namespace).list(pagination)
    }

    Long count() {
        Classifier.count()
    }

    Classifier save(Classifier classifier) {
        classifier.save()
    }

    def saveAll(Collection<Classifier> classifiers) {

        Collection<Classifier> alreadySaved = classifiers.findAll {it.ident() && it.isDirty()}
        Collection<Classifier> notSaved = classifiers.findAll {!it.ident()}

        if (alreadySaved) {
            log.debug('Straight saving {} classifiers', alreadySaved.size())
            Classifier.saveAll(alreadySaved)
        }

        if (notSaved) {
            log.debug('Batch saving {} classifiers', notSaved.size())
            List batch = []
            int count = 0

            notSaved.each {de ->

                batch += de
                count++
                if (count % Classifier.BATCH_SIZE == 0) {
                    batchSave(batch)
                    batch.clear()
                }
            }
            batchSave(batch)
            batch.clear()
        }
    }

    void batchSave(List<Classifier> classifiers) {
        long start = System.currentTimeMillis()
        log.trace('Batch saving {} classifiers', classifiers.size())

        Classifier.saveAll(classifiers)

        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()

        log.trace('Batch save took {}', Utils.getTimeString(System.currentTimeMillis() - start))
    }

    void delete(Serializable id) {
        delete(get(id))
    }

    void delete(Classifier classifier, boolean root = true) {
        if (!classifier) {
            log.warn('Attempted to delete Classifier which doesnt exist')
            return
        }
        cleanoutClassifier(classifier)
        if (root) classifier.delete(flush: true)
    }

    Classifier findOrCreateByLabel(String label) {
        Classifier.findOrCreateByLabel(label)
    }

    Classifier findOrCreateByLabel(String label, User createdBy) {
        Classifier classifier = Classifier.findByLabel(label)
        if (classifier) return classifier
        classifier = new Classifier(label: label, createdBy: createdBy.emailAddress)
        if (classifier.validate()) {
            classifier.save(flush: true)
            classifier.addToEdits(createdBy: createdBy.emailAddress, description: "Classifier ${label} created")
        } else {
            throw new ApiInvalidModelException('CSXX', 'Could not create new Classifier', classifier.errors)
        }
        classifier
    }

    Set<Classifier> findOrCreateAllByLabels(Collection<String> labels, User catalogueUser) {
        labels.collect {
            findOrCreateByLabel(it, catalogueUser)
        } as Set
    }

    Classifier findOrCreateClassifier(User catalogueUser, Classifier classifier) {
        Classifier exists = Classifier.findByLabel(classifier.label)
        if (exists) return exists
        classifier.createdBy = catalogueUser.emailAddress
        classifier
    }

    /**
     * Find all resources by the defined user security policy manager. If none provided then assume no security policy in place in which case
     * everything is public.
     * @param userSecurityPolicyManager
     * @param pagination
     * @return
     */
    List<Classifier> findAllByUser(UserSecurityPolicyManager userSecurityPolicyManager, Map pagination = [:]) {
        List<UUID> ids = userSecurityPolicyManager.listReadableSecuredResourceIds(Classifier)
        ids ? Classifier.findAllByIdInList(ids, pagination) : []
    }

    List<Classifier> findAllByCatalogueItemId(UserSecurityPolicyManager userSecurityPolicyManager, UUID catalogueItemId, Map pagination = [:]) {
        CatalogueItem catalogueItem

        for (CatalogueItemService service : catalogueItemServices) {
            if (catalogueItem) break
            catalogueItem = service.findByIdJoinClassifiers(catalogueItemId)
        }

        if (!catalogueItem || !catalogueItem.classifiers) return []

        // Filter out all the classifiers which the user can't read
        Collection<Classifier> allClassifiersInItem = catalogueItem.classifiers
        List<UUID> readableIds = userSecurityPolicyManager.listReadableSecuredResourceIds(Classifier)
        new PaginatedResultList(allClassifiersInItem.findAll {it.id in readableIds}.toList(), pagination)
    }

    List<Classifier> findAllByParentClassifierId(UUID parentClassifierId, Map pagination = [:]) {
        Classifier.byParentClassifierId(parentClassifierId).list(pagination)
    }

    Classifier editInformation(Classifier classifier, String label, String description) {
        classifier.label = label
        classifier.description = description
        classifier.validate()
        classifier
    }

    def <C extends CatalogueItem> Classifier addClassifierToCatalogueItem(Class<C> catalogueItemClass, UUID catalogueItemId, Classifier classifier) {
        CatalogueItemService service = catalogueItemServices.find {it.handles(catalogueItemClass)}
        service.addClassifierToCatalogueItem(catalogueItemId, classifier)
        classifier
    }

    def <C extends CatalogueItem> void removeClassifierFromCatalogueItem(Class<C> catalogueItemClass, UUID catalogueItemId, Classifier classifier) {
        CatalogueItemService service = catalogueItemServices.find {it.handles(catalogueItemClass)}
        service.removeClassifierFromCatalogueItem(catalogueItemId, classifier)
        classifier
    }

    void checkClassifiers(User catalogueUser, def classifiedItem) {
        if (!classifiedItem.classifiers) return

        classifiedItem.classifiers.each {it ->
            it.createdBy = it.createdBy ?: classifiedItem.createdBy
        }

        Set<Classifier> classifiers = [] as HashSet
        classifiers.addAll(classifiedItem.classifiers ?: [])

        classifiedItem.classifiers?.clear()

        List<Classifier> foundOrCreated = classifiers.collect {cls ->
            findOrCreateClassifier(catalogueUser, cls)
        }

        batchSave(foundOrCreated)

        foundOrCreated.each {cls ->
            classifiedItem.addToClassifiers(cls)
        }
    }

    List<CatalogueItem> findAllReadableCatalogueItemsByClassifierId(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                    UUID classifierId, Map pagination = [:]) {
        Classifier classifier = get(classifierId)
        catalogueItemServices.collect {service ->
            service.findAllReadableByClassifier(userSecurityPolicyManager, classifier)
        }.findAll().flatten()
    }

    private void cleanoutClassifier(Classifier classifier) {
        classifier.childClassifiers.each {cleanoutClassifier(it)}
        catalogueItemServices.each {it.removeAllFromClassifier(classifier)}
    }
}
