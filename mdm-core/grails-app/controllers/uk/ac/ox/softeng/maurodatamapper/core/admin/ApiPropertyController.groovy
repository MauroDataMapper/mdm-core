/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.core.admin

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.gorm.transactions.Transactional
import grails.web.servlet.mvc.GrailsParameterMap
import groovy.util.logging.Slf4j

@Slf4j
class ApiPropertyController extends EditLoggingController<ApiProperty> {

    ApiPropertyService apiPropertyService

    static responseFormats = ['json', 'xml', 'csv']

    static includesExcludes = [include: ['key', 'value', 'publiclyVisible', 'category']]

    ApiPropertyController() {
        super(ApiProperty)
    }

    @Override
    def index(Integer max) {
        def res = listAllResources(params)
        // The new grails-views code sets the modelAndView object rather than writing the response
        // Therefore if thats written then we dont want to try and re-write it
        if (response.isCommitted() || modelAndView) return
        respond res, [model: [userSecurityPolicyManager: currentUserSecurityPolicyManager], view: 'index']
    }

    /**
     * Override so that we can specify includesExcludes when creating the resource
     * @return
     */
    @Transactional
    @Override
    def save() {
        if (handleReadOnly()) return

        def instance = createResource(includesExcludes)

        if (response.isCommitted()) return

        if (!validateResource(instance, 'create')) return

        saveResource instance

        saveResponse instance
    }

    /**
     * Override the base method so that we can set instance.lastUpdatedBy
     */
    @Transactional
    @Override
    protected boolean validateResource(ApiProperty instance, String view) {
        instance.lastUpdatedBy = currentUser.emailAddress
        super.validateResource(instance, view)
    }

    @Override
    protected ApiProperty saveResource(ApiProperty resource) {
        ApiProperty apiProperty = super.saveResource(resource)
        apiPropertyService.updateLinkGeneratorWithSiteUrl(apiProperty)
        apiProperty
    }

    @Override
    protected void serviceDeleteResource(ApiProperty resource) {
        apiPropertyService.delete(resource)
    }

    @Override
    protected List<ApiProperty> listAllReadableResources(Map params) {
        if (!apiPropertyService.count()) {
            throw new ApiInternalException('AS01', 'Api Properties have not been loaded. ' +
                                                   'Please contact the System Administrator')
        }
        if ((params as GrailsParameterMap).boolean('openAccess')) return apiPropertyService.findAllByPubliclyVisible(params)
        currentUserSecurityPolicyManager.isApplicationAdministrator() ? apiPropertyService.list(params) : []

    }

    /**
     * Save a collection of ApiProperty resources and respond with the index listing
     * @return
     */
    @Transactional
    def apply() {
        if (handleReadOnly()) return

        Collection instances = createResources()

        // Iterate each instance, validating each one after the previous has been saved.
        ApiProperty errorInstance
        instances.each {instance ->

            if (!errorInstance) {
                instance.validate()

                if (instance.hasErrors()) {
                    errorInstance = instance
                    transactionStatus.setRollbackOnly()
                } else {
                    saveResource instance
                }
            }
        }

        if (errorInstance) {
            respond errorInstance.errors
            return
        } else {
            return index()
        }
    }

    /**
     * Create a collection of resources from the request body
     * @return
     */
    protected Collection<ApiProperty> createResources() {

        // First, bind what was exported to an ApiPropertyCollection (which has properties count and items)
        // There isn't a way here to specify an includesExcludes list
        ApiPropertyCollection apiPropertyCollection = new ApiPropertyCollection()
        bindData apiPropertyCollection, getObjectToBind()

        // Second, iterate the items we just created, and use each instance
        // to create another cleaned instance. This time, we specify includesExcludes.
        // At the same time, set the createdBy property of the cleaned instance.
        Collection<ApiProperty> cleanedInstances = []
        User creator = getCurrentUser()
        apiPropertyCollection.items.each {instance ->
            ApiProperty cleanedInstance = new ApiProperty()

            // Copy the included properties to a cleaned instance
            includesExcludes['include'].each {
                cleanedInstance[it] = instance[it]
            }

            cleanedInstance.createdBy = creator.emailAddress
            cleanedInstances.add(cleanedInstance)
        }

        cleanedInstances
    }
}
