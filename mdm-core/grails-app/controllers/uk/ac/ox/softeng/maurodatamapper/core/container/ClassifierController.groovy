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
import uk.ac.ox.softeng.maurodatamapper.util.Utils

class ClassifierController extends EditLoggingController<Classifier> {

    static responseFormats = ['json', 'xml']

    ClassifierService classifierService

    ClassifierController() {
        super(Classifier)
    }

    def catalogueItems() {
        classifierService.findAllReadableCatalogueItemsByClassifierId(currentUserSecurityPolicyManager, Utils.toUuid(params.classifierId), params)
    }

    @Override
    protected List<Classifier> listAllReadableResources(Map params) {
        params.sort = params.sort ?: 'label'
        if (params.catalogueItemId) {
            return classifierService.findAllByCatalogueItemId(currentUserSecurityPolicyManager, Utils.toUuid(params.catalogueItemId), params)
        }
        classifierService.findAllByUser(currentUserSecurityPolicyManager, params)
    }

    @Override
    protected void serviceDeleteResource(Classifier resource) {
        classifierService.delete(resource)
    }

    @Override
    protected Classifier saveResource(Classifier resource) {
        Classifier classifier = super.saveResource(resource) as Classifier

        if (params.catalogueItemId) {
            classifierService.addClassifierToCatalogueItem(params.catalogueItemDomainType, params.catalogueItemId, classifier)
        }
        classifier
    }

    @Override
    protected void deleteResource(Classifier resource) {
        if (params.catalogueItemId) {
            classifierService.removeClassifierFromCatalogueItem(params.catalogueItemDomainType, params.catalogueItemId, resource)
        } else {
            super.deleteResource(resource)
        }
    }
}
