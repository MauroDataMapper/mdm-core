/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.core.facet


import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmController

import grails.rest.RestfulController

class ChangelogController extends RestfulController<Edit> implements MdmController {

    static responseFormats = ['json', 'xml']

    static allowedMethods = [save: ['POST'], update: [], patch: [], delete: [], show: 'GET', index: 'GET']

    EditService editService

    ChangelogController() {
        super(Edit)
    }

    @Override
    protected Edit createResource() {
        Edit resource = super.createResource() as Edit
        resource.title = EditTitle.CHANGELOG
        resource.createdBy = currentUser.emailAddress
        resource.resourceId = params.resourceId
        resource.resourceDomainType = params.resourceDomainType

        resource
    }

    @Override
    protected List<Edit> listAllResources(Map params) {
        params.sort = params.sort ?: 'dateCreated'
        params.order = params.order ?: 'asc'

        editService.findAllByResourceAndTitle(params.resourceDomainType, params.resourceId, EditTitle.CHANGELOG, params)
    }
}
