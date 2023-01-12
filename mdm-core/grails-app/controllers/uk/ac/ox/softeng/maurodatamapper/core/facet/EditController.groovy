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
package uk.ac.ox.softeng.maurodatamapper.core.facet


import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmController

import grails.rest.RestfulController

class EditController extends RestfulController<Edit> implements MdmController {

    static responseFormats = ['json', 'xml']

    static allowedMethods = [save: [], update: [], patch: [], delete: [], show: [], index: 'GET']

    EditService editService

    EditController() {
        super(Edit)
    }

    EditController(boolean readOnly) {
        super(Edit, readOnly)
    }

    @Override
    protected List<Edit> listAllResources(Map params) {
        params.sort = params.sort ?: 'dateCreated'
        params.order = params.order ?: 'asc'

        List<Edit> edits = editService.findAllByResource(params.resourceDomainType, params.resourceId, params)
        edits.collect {editService.populateEditUser(it)}
    }
}
