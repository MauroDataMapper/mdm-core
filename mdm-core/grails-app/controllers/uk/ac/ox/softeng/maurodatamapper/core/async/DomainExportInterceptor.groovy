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
package uk.ac.ox.softeng.maurodatamapper.core.async

import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.core.GrailsClass
import groovy.util.logging.Slf4j

@Slf4j
class DomainExportInterceptor implements MdmInterceptor {

    DomainExportService domainExportService

    boolean before() {
        Utils.toUuid(params, 'id')
        Utils.toUuid(params, 'domainExportId')
        mapDomainTypeToClass('resource', false)

        // Anyone authenticated can index as the controller will listAllReadable
        if (isIndex() && !params.containsKey('resourceId')) {
            return currentUserSecurityPolicyManager.isAuthenticated() ?: forbiddenDueToPermissions()
        }

        if (params.containsKey('resourceId')) {
            return checkActionAuthorisationOnUnsecuredResource(params.domainExportId ?: params.id, params.resourceClass, params.resourceId)
        }

        checkActionAuthorisationOnUnsecuredResource(params.domainExportId ?: params.id)
    }

    boolean checkActionAuthorisationOnUnsecuredResource(UUID id) {

        DomainExport domainExport = domainExportService.get(id)
        if (!domainExport) return notFound(DomainExport, id)

        String[] exportedDomainIds = domainExport.multiDomainExport ? domainExport.exportedDomainIds.split(',') : [domainExport.exportedDomainId]

        for (String exportedDomainId : exportedDomainIds) {
            if (!checkActionAuthorisationOnUnsecuredResource(id, domainExport.exportedDomainType, Utils.toUuid(exportedDomainId))) return false
        }
        true
    }

    boolean checkActionAuthorisationOnUnsecuredResource(UUID id, String exportedDomainType, UUID exportedDomainId) {

        GrailsClass grailsClass = Utils.lookupGrailsDomain(grailsApplication, exportedDomainType)
        Class exportedDomainClass = grailsClass.clazz

        // If the exported domain is a SR then we can use that against the UserSecurityPolicyManager
        if (Utils.parentClassIsAssignableFromChild(SecurableResource, exportedDomainClass)) {
            return checkActionAuthorisationOnUnsecuredResource(id, exportedDomainClass as Class<SecurableResource>, exportedDomainId)
        }

        // Currently you can't export a ModelItem but that may change so lets code it in now,
        // check the MI and the Model access
        if (Utils.parentClassIsAssignableFromChild(ModelItem, exportedDomainClass)) {
            ModelItem modelItem = domainExportService.getExportedDomain(exportedDomainType, exportedDomainId) as ModelItem
            return checkActionAuthorisationOnUnsecuredResource(id, modelItem.model.class, modelItem.model.id)
        }

        // Otherwise its an unknown ExportedDomain type so we just say they can't read it
        log.warn('Could not determine if exported domain {}:{} can be acted on', exportedDomainType, exportedDomainId)
        notFound(DomainExport, id)
    }

    boolean checkActionAuthorisationOnUnsecuredResource(UUID id,
                                                        Class<? extends SecurableResource> owningSecureResourceClass, UUID owningSecureResourceId) {
        boolean canRead = currentUserSecurityPolicyManager.userCanReadResourceId(DomainExport, id, owningSecureResourceClass, owningSecureResourceId)

        if (actionName == 'download') {
            return canRead ?: notFound(DomainExport, id)
        }
        // Otherwise just fall through to the default handling
        checkActionAuthorisationOnUnsecuredResource(DomainExport, id, owningSecureResourceClass, owningSecureResourceId)
    }
}
