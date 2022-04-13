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
package uk.ac.ox.softeng.maurodatamapper.profile

import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.interceptor.FacetInterceptor
import uk.ac.ox.softeng.maurodatamapper.profile.object.Profile
import uk.ac.ox.softeng.maurodatamapper.profile.provider.ProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.util.Utils

class ProfileInterceptor extends FacetInterceptor {

    ProfileService profileService

    @Override
    Class getFacetClass() {
        Profile
    }

    boolean before() {
        // Public or secured at controller as using listAll
        if (actionName in ['profileProviders', 'dynamicProfileProviders', 'listModelsInProfile', 'listValuesInProfile']) return true
        if (!params.containsKey('modelDomainType') && actionName == 'search') return true
        facetResourceChecks()
        checkActionAllowedOnFacet()
    }

    @Override
    void facetResourceChecks() {
        Utils.toUuid(params, 'id')
        params.multiFacetAwareItemDomainType = params.multiFacetAwareItemDomainType ?: params.catalogueItemDomainType ?: params.containerDomainType ?: params.modelDomainType
        params.multiFacetAwareItemId = params.multiFacetAwareItemId ?: params.catalogueItemId ?: params.containerId ?: params.modelId
        checkAdditionalIds()
        mapDomainTypeToClass(getOwningType(), true)
    }

    boolean checkActionAuthorisationOnUnsecuredResource(Class resourceClass, UUID id,
                                                        Class<? extends SecurableResource> owningSecureResourceClass, UUID owningSecureResourceId) {


        boolean canRead = currentUserSecurityPolicyManager.userCanReadResourceId(Metadata, id, owningSecureResourceClass, owningSecureResourceId)

        // Read only actions
        // ProfileController.getMany and ProfileController.validateMany must check that items requested
        // in the body of the request belong to the model that was requested
        if (actionName in ['validate', 'usedProfiles', 'unusedProfiles', 'nonProfileMetadata', 'getMany', 'validateMany', 'search']) {
            return canRead ?: notFound(id ? resourceClass : owningSecureResourceClass, (id ?: owningSecureResourceId).toString())
        }

        // This tests that we *might* be able to save.
        // ProfileController.saveMany must check access for every profile service provider referenced in the request body
        if (actionName == 'saveMany') {
            boolean userCanSaveIgnoreFinalise = currentUserSecurityPolicyManager.userCanWriteResourceId(Metadata, null,
                                                                                                        owningSecureResourceClass, owningSecureResourceId,
                                                                                                        'saveIgnoreFinalise')
            boolean userCanCreateMetadata = currentUserSecurityPolicyManager.userCanCreateResourceId(Metadata, null,
                                                                                                     owningSecureResourceClass, owningSecureResourceId)
            return (userCanSaveIgnoreFinalise || userCanCreateMetadata) ?:
                   forbiddenOrNotFound(canRead, id ? resourceClass : owningSecureResourceClass, id ?: owningSecureResourceId)
        }

        ProfileProviderService profileProviderService = profileService.findProfileProviderService(params.profileNamespace, params.profileName, params.profileVersion)

        if (profileProviderService && profileProviderService.canBeEditedAfterFinalisation()) {
            // Special handling for profiles which allow post finalisation updates
            // In this situation then we check if the user can perform the action ignoring the finalisation state of the owning resource
            if (isUpdate()) {
                return currentUserSecurityPolicyManager.userCanWriteResourceId(Metadata, null,
                                                                               owningSecureResourceClass, owningSecureResourceId,
                                                                               'updateIgnoreFinalise') ?:
                       forbiddenOrNotFound(canRead, id ? resourceClass : owningSecureResourceClass, id ?: owningSecureResourceId)
            }
            if (isSave()) {
                return currentUserSecurityPolicyManager.userCanWriteResourceId(Metadata, null,
                                                                               owningSecureResourceClass, owningSecureResourceId,
                                                                               'saveIgnoreFinalise') ?:
                       forbiddenOrNotFound(canRead, id ? resourceClass : owningSecureResourceClass, id ?: owningSecureResourceId)
            }
        }

        // Otherwise just fall through to the default handling
        super.checkActionAuthorisationOnUnsecuredResource(Metadata, id, owningSecureResourceClass, owningSecureResourceId)
    }
}
