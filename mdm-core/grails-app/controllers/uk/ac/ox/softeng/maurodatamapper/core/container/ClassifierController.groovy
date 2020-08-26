/*
 * Copyright 2020 University of Oxford
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

import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController
import uk.ac.ox.softeng.maurodatamapper.security.SecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import org.springframework.beans.factory.annotation.Autowired

class ClassifierController extends EditLoggingController<Classifier> {

    static responseFormats = ['json', 'xml']

    ClassifierService classifierService

    @Autowired(required = false)
    SecurityPolicyManagerService securityPolicyManagerService

    ClassifierController() {
        super(Classifier)
    }

    def catalogueItems() {
        respond catalogueItemList: classifierService.findAllReadableCatalogueItemsByClassifierId(currentUserSecurityPolicyManager, Utils.toUuid(params.classifierId), params)
    }

    @Transactional
    def readByEveryone() {
        Classifier instance = queryForResource(params.classifierId)

        if (!instance) return notFound(params.classifierId)

        instance.readableByEveryone = request.method == 'PUT'

        updateResource(instance)
        updateResponse(instance)
    }

    @Transactional
    def readByAuthenticated() {
        Classifier instance = queryForResource(params.classifierId)

        if (!instance) return notFound(params.classifierId)

        instance.readableByAuthenticatedUsers = request.method == 'PUT'

        updateResource(instance)
        updateResponse(instance)
    }

    @Override
    protected List<Classifier> listAllReadableResources(Map params) {
        params.sort = params.sort ?: 'label'
        if (params.catalogueItemId) {
            return classifierService.findAllByCatalogueItemId(currentUserSecurityPolicyManager, params.catalogueItemId, params)
        }
        if (params.classifierId) {
            return classifierService.findAllByParentClassifierId(params.classifierId, params)
        }
        classifierService.findAllByUser(currentUserSecurityPolicyManager, params)
    }

    @Override
    protected void serviceDeleteResource(Classifier resource) {
        if (securityPolicyManagerService) {
            currentUserSecurityPolicyManager = securityPolicyManagerService.removeSecurityForSecurableResource(resource, currentUser)
        }
        classifierService.delete(resource)
    }

    @Override
    protected Classifier createResource() {
        Classifier resource = super.createResource() as Classifier
        if (params.classifierId) {
            resource.parentClassifier = classifierService.get(params.classifierId)
        }
        resource
    }

    @Override
    protected Classifier saveResource(Classifier resource) {
        Classifier classifier = super.saveResource(resource) as Classifier

        if (params.catalogueItemId) {
            classifierService.addClassifierToCatalogueItem(params.catalogueItemClass, params.catalogueItemId, classifier)
        }
        if (securityPolicyManagerService) {
            currentUserSecurityPolicyManager = securityPolicyManagerService.addSecurityForSecurableResource(classifier, currentUser, classifier.label)
        }
        classifier
    }

    @Override
    protected void deleteResource(Classifier resource) {
        if (params.catalogueItemId) {
            classifierService.removeClassifierFromCatalogueItem(params.catalogueItemClass, params.catalogueItemId, resource)
        } else {
            super.deleteResource(resource)
        }
    }

    @Override
    protected Classifier updateResource(Classifier resource) {
        Set<String> changedProperties = resource.getDirtyPropertyNames()
        Classifier classifier = super.updateResource(resource) as Classifier
        if (securityPolicyManagerService) {
            currentUserSecurityPolicyManager = securityPolicyManagerService.updateSecurityForSecurableResource(classifier,
                                                                                                               changedProperties,
                                                                                                               currentUser)
        }
        classifier
    }
}
