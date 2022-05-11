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
package uk.ac.ox.softeng.maurodatamapper.core.controller

import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmController
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.EditHistoryAware
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.artefact.controller.RestResponder
import grails.databinding.DataBindingSource
import grails.gorm.PagedResultList
import grails.gorm.transactions.Transactional
import grails.rest.RestfulController
import grails.web.databinding.DataBindingUtils
import grails.web.http.HttpHeaders
import groovy.util.logging.Slf4j

import static org.springframework.http.HttpStatus.CREATED
import static org.springframework.http.HttpStatus.NO_CONTENT
import static org.springframework.http.HttpStatus.OK

/**
 * @since 05/02/2018
 */
@Slf4j
abstract class EditLoggingController<T> extends RestfulController<T> implements MdmController, RestResponder {

    static allowedMethods = [patch: [], edit: [], create: []]

    public static final DEFAULT_SAVE_ARGS = [flush: true, validate: false]

    EditLoggingController(Class<T> resource) {
        super(resource)
    }

    EditLoggingController(Class<T> resource, boolean readOnly) {
        super(resource, readOnly)
    }

    abstract protected void serviceDeleteResource(T resource)

    abstract protected List<T> listAllReadableResources(Map params)

    @Override
    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        def res = listAllResources(params)
        // The new grails-views code sets the modelAndView object rather than writing the response
        // Therefore if thats written then we dont want to try and re-write it
        if (response.isCommitted() || modelAndView) return
        respond res, [model: [userSecurityPolicyManager: currentUserSecurityPolicyManager], view: 'index']
    }

    @Override
    def show() {
        def resource = queryForResource(params.id)
        resource ? respond(resource, [model: [userSecurityPolicyManager: currentUserSecurityPolicyManager], view: 'show']) : notFound(params.id)
    }

    @Transactional
    @Override
    def save() {
        if (handleReadOnly()) return

        def instance = createResource()

        if (response.isCommitted()) return

        if (!validateResource(instance, 'create')) return

        saveResource instance

        saveResponse instance
    }

    @Transactional
    @Override
    def update() {
        if (handleReadOnly()) return

        T instance = queryForResource(params.id)

        if (instance == null) {
            transactionStatus.setRollbackOnly()
            notFound(params.id)
            return
        }

        instance.properties = getObjectToBind()

        if (!validateResource(instance, 'update')) return

        updateResource instance

        updateResponse instance
    }

    @Transactional
    @Override
    def delete() {
        if (handleReadOnly()) {
            return
        }

        def instance = queryForResource(params.id)
        if (instance == null) {
            transactionStatus.setRollbackOnly()
            notFound(params.id)
            return
        }

        if (!validateResourceDeletion(instance, 'delete')) return

        deleteResource instance

        deleteResponse instance
    }

    @Override
    protected List<T> listAllResources(Map params) {
        log.trace('List all resources')
        if (params.all) removePaginationParameters()
        listAllReadableResources(params)
    }

    @Override
    protected Integer countResources() {
        def result = listAllResources(params)
        result instanceof PagedResultList ? result.getTotalCount() : result?.size() ?: 0
    }

    @Override
    protected T createResource(Map includesExcludes = Collections.EMPTY_MAP) {
        T instance = resource.getDeclaredConstructor().newInstance()
        bindData instance, getObjectToBind(), includesExcludes
        User creator = getCurrentUser()
        if (instance.hasProperty('createdBy') && creator) {
            instance.createdBy = creator.emailAddress
        }
        instance
    }

    @Override
    protected T saveResource(T resource) {
        log.trace('save resource')
        resource.save flush: true, validate: false
        if (resource.instanceOf(EditHistoryAware) && !params.boolean('noHistory')) resource.addCreatedEdit(getCurrentUser())
        resource
    }

    @Override
    protected T updateResource(T resource) {
        log.trace('update {}', resource.ident())
        List<String> dirtyPropertyNames = resource.getDirtyPropertyNames()
        resource.save flush: true, validate: false
        if (resource.instanceOf(EditHistoryAware) && !params.boolean('noHistory')) resource.addUpdatedEdit(getCurrentUser(), dirtyPropertyNames)
        resource
    }

    @Override
    protected void deleteResource(T resource) {
        log.trace('delete {}', resource.ident())
        serviceDeleteResource(resource)
        if (resource.instanceOf(EditHistoryAware) && !params.boolean('noHistory')) resource.addDeletedEdit(getCurrentUser())
    }

    protected void updateResponse(T instance) {
        request.withFormat {
            '*' {
                response.addHeader(HttpHeaders.LOCATION,
                                   grailsLinkGenerator.link(resource: this.controllerName, action: 'show', id: instance.id, absolute: true,
                                                            namespace: hasProperty('namespace') ? this.namespace : null))
                respond instance, [status: OK, view: 'update', model: [userSecurityPolicyManager: currentUserSecurityPolicyManager]]
            }

        }
    }

    protected void saveResponse(T instance) {
        request.withFormat {
            '*' {
                response.addHeader(HttpHeaders.LOCATION,
                                   grailsLinkGenerator.link(resource: this.controllerName, action: 'show', id: instance.id, absolute: true,
                                                            namespace: hasProperty('namespace') ? this.namespace : null))
                respond instance, [status: CREATED, view: 'show', model: [userSecurityPolicyManager: currentUserSecurityPolicyManager]]
            }
        }
    }

    protected void deleteResponse(T instance) {
        request.withFormat {
            '*' {
                render status: NO_CONTENT
            }
        }
    }

    protected void removePaginationParameters() {
        params.remove('max')
        params.remove('offset')
    }

    @Transactional
    protected boolean validateResource(T instance, String view) {
        if (instance.hasErrors() || !instance.validate()) {
            transactionStatus.setRollbackOnly()
            respond instance.errors, view: view // STATUS CODE 422
            return false
        }
        true
    }

    @Transactional
    protected boolean validateResourceDeletion(T instance, String view) {
        true
    }

    @Override
    protected getObjectToBind() {
        request.getAttribute('cached_body') ?: request
    }

    Map<String, Object> extractRequestBodyToMap() {
        Object body = getObjectToBind()
        if (body instanceof Map<String, Object>) return body
        DataBindingSource dataBindingSource = DataBindingUtils.createDataBindingSource(grailsApplication, Map, getObjectToBind())
        Map<String, Object> requestBody = dataBindingSource.propertyNames.collectEntries {key -> [key, dataBindingSource.getPropertyValue(key)]}
        if (!request.getAttribute('cached_body')) request.setAttribute('cached_body', requestBody)
        requestBody
    }
}
