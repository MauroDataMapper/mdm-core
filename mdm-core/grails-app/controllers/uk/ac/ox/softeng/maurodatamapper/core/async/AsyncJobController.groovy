/*
 * Copyright 2020-2024 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.core.async

import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmController

import grails.gorm.transactions.Transactional
import grails.rest.RestfulController

/**
 * @since 15/03/2022
 */
class AsyncJobController extends RestfulController<AsyncJob> implements MdmController {

    static responseFormats = ['json', 'xml']

    static allowedMethods = [save: [], update: [], patch: [], delete: 'DELETE', show: 'GET', index: 'GET']

    AsyncJobService asyncJobService

    AsyncJobController() {
        super(AsyncJob)
    }

    @Override
    def index(Integer max) {
        def res = listAllResources(params)
        // The new grails-views code sets the modelAndView object rather than writing the response
        // Therefore if thats written then we dont want to try and re-write it
        if (response.isCommitted() || modelAndView) return
        respond res, view: 'index'
    }

    @Override
    def show() {
        def resource = queryForResource(params.id)
        resource ? respond(resource, view: 'show') : notFound(params.id)
    }

    @Transactional
    def delete() {
        if (handleReadOnly()) {
            return
        }

        def instance = queryForResource(params.id)
        if (instance == null) {
            transactionStatus.setRollbackOnly()
            notFound()
            return
        }

        asyncJobService.cancelRunningJob(instance)

        respond(queryForResource(instance.id), view: 'show')
    }

    @Override
    protected void deleteResource(AsyncJob resource) {
        //no-op
    }

    @Override
    protected List<AsyncJob> listAllResources(Map params) {
        currentUserSecurityPolicyManager.isApplicationAdministrator() ?
        asyncJobService.listWithFilter(params, params) :
        asyncJobService.findAllByStartedByUser(currentUser.emailAddress, params, params)

    }

    @Override
    protected AsyncJob queryForResource(Serializable id) {
        currentUserSecurityPolicyManager.isApplicationAdministrator() ?
        asyncJobService.get(id) :
        asyncJobService.findByStartedByUserAndId(currentUser.emailAddress, id as UUID)
    }
}
