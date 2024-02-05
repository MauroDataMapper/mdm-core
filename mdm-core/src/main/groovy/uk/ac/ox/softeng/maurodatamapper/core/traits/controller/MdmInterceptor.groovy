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
package uk.ac.ox.softeng.maurodatamapper.core.traits.controller

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.core.GrailsClass
import grails.databinding.DataBindingSource
import grails.web.api.WebAttributes
import grails.web.databinding.DataBindingUtils
import grails.web.mime.MimeType
import grails.web.servlet.mvc.GrailsParameterMap
import groovy.util.logging.Slf4j

import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletRequest

/**
 * @since 25/11/2019
 */

@Slf4j
trait MdmInterceptor implements UserSecurityPolicyManagerAware, WebAttributes {

    @Override
    void renderMapForResponse(Map map) {
        render(map)
    }

    GrailsClass getGrailsResource(GrailsParameterMap params, String resourceParam) {
        String lookup = params[resourceParam]

        if (!lookup) {
            throw new ApiBadRequestException('MCI01', 'No domain class resource provided')
        }

        GrailsClass grailsClass = Utils.lookupGrailsDomain(grailsApplication, lookup)
        if (!grailsClass) {
            throw new ApiBadRequestException('MCI02', "Unrecognised domain class resource [${params[resourceParam]}]")
        }
        grailsClass
    }

    void mapDomainTypeToClass(String paramKey, boolean required = false) {
        String key = paramKey + 'DomainType'
        String classKey = paramKey + 'Class'
        String idKey = paramKey + 'Id'
        if (required || params.containsKey(key)) {
            Utils.toUuid(params, idKey)
            GrailsClass grailsClass = getGrailsResource(params, key)
            params[key] = grailsClass.shortName
            params[classKey] = grailsClass.clazz
        }
    }

    boolean isIndex() {
        actionName == 'index'
    }

    boolean isSave() {
        actionName == 'save'
    }

    boolean isDelete() {
        actionName == 'delete'
    }

    boolean isShow() {
        actionName == 'show'
    }

    boolean isUpdate() {
        actionName == 'update'
    }

    boolean isKnownAction() {
        isIndex() || isSave() || isDelete() || isShow() || isUpdate()
    }

    /**
     * Check the current action is allowed against the resource and id.
     * The resource may or may not be a {@link SecurableResource}. This method should be used for interceptors which cover domains which belong to
     * a {@link SecurableResource} or domains which belong to a domain which belongs to a {@link SecurableResource}.
     *
     * @param resourceClass
     * @param id
     * @return
     */
    boolean checkActionAuthorisationOnUnsecuredResource(Class resourceClass, UUID id,
                                                        Class<? extends SecurableResource> owningSecureResourceClass, UUID owningSecureResourceId) {

        boolean canRead = currentUserSecurityPolicyManager.userCanReadResourceId(resourceClass, id, owningSecureResourceClass, owningSecureResourceId)

        // The 3 tiers of writing are deleting, updating/editing and saving/creation
        // We will have to handle certain controls inside the controller
        if (isDelete()) {
            return currentUserSecurityPolicyManager.userCanDeleteResourceId(resourceClass, id,
                                                                            owningSecureResourceClass, owningSecureResourceId) ?:
                   forbiddenOrNotFound(canRead, id ? resourceClass : owningSecureResourceClass, id ?: owningSecureResourceId)

        }
        if (isUpdate()) {
            return currentUserSecurityPolicyManager.userCanWriteResourceId(resourceClass, id,
                                                                           owningSecureResourceClass, owningSecureResourceId,
                                                                           isDescriptionEdit() ? 'editDescription' : actionName) ?:
                   forbiddenOrNotFound(canRead, id ? resourceClass : owningSecureResourceClass, id ?: owningSecureResourceId)
        }
        if (isSave()) {
            return currentUserSecurityPolicyManager.userCanCreateResourceId(resourceClass, id,
                                                                            owningSecureResourceClass, owningSecureResourceId) ?:
                   forbiddenOrNotFound(canRead, id ? resourceClass : owningSecureResourceClass, id ?: owningSecureResourceId)
        }

        // If show then if user can read then they can see it otherwise the id is notFound
        if (isShow()) {
            return canRead ?: notFound(id ? resourceClass : owningSecureResourceClass, (id ?: owningSecureResourceId).toString())
        }
        // If index and owning resource id then we are trying to index the content in the owning resource
        // therefore if cannot read then return not found on owning resource otherwise return forbidden
        if (isIndex()) {
            if (owningSecureResourceId) {
                return canRead ?: notFound(owningSecureResourceClass, owningSecureResourceId.toString())
            }
            return canRead ?: forbiddenDueToPermissions()
        }

        unauthorised("Unknown action [${actionName}]")
    }

    boolean forbiddenOrNotFound(boolean canRead, Class resourceClass, UUID resourceId) {
        canRead ? forbiddenDueToPermissions() :
        notFound(resourceClass, resourceId.toString())
    }

    DataBindingSource cacheRequestBody() {
        if (!((request as HttpServletRequest).contentType in [MimeType.MULTIPART_FORM.name, MimeType.FORM.name]) && (request as HttpServletRequest).contentLength > 0) {
            try {
                ServletInputStream is = (request as HttpServletRequest).getInputStream()
                DataBindingSource dbs = DataBindingUtils.createDataBindingSource(grailsApplication, Map, is)
                (request as HttpServletRequest).setAttribute('cached_body', dbs)
                return dbs
            } catch (Exception ignored) {
                log.warn 'Could not cache request body {}', ignored.message
            }
        }
        null
    }

    boolean isDescriptionEdit() {
        DataBindingSource bs = cacheRequestBody()
        bs && bs.size() == 1 && bs.containsProperty('description')
    }
}