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
package uk.ac.ox.softeng.maurodatamapper.referencedata.traits.controller

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.util.Utils

/**
 * @since 20/03/2020
 */
abstract class ReferenceDataModelSecuredInterceptor implements MdmInterceptor {

    abstract Class getModelItemClass()

    void checkIds() {
        Utils.toUuid(params, 'referenceDataModelId')
        Utils.toUuid(params, 'id')
        Utils.toUuid(params, 'otherReferenceDataModelId')
    }

    void checkReferenceDataModelId() {
        if (!params.referenceDataModelId) throw new ApiBadRequestException('DMSI01', 'No Reference Data Model Id provided against secured resource')
    }

    void performChecks() {
        checkIds()
        checkReferenceDataModelId()
    }

    boolean checkStandardActions() {
        checkActionAuthorisationOnUnsecuredResource(getModelItemClass(), params.id, ReferenceDataModel, params.referenceDataModelId)
    }

    boolean canReadReferenceDataModel() {
        currentUserSecurityPolicyManager.userCanReadSecuredResourceId(ReferenceDataModel, params.referenceDataModelId) ?: notFound(ReferenceDataModel,
                                                                                                                 params.referenceDataModelId.toString()
        )
    }

    boolean canCopyFromReferenceDataModelToOtherReferenceDataModel() {
        boolean canRead = currentUserSecurityPolicyManager.userCanReadSecuredResourceId(ReferenceDataModel, params.referenceDataModelId)
        if (!currentUserSecurityPolicyManager.userCanEditSecuredResourceId(ReferenceDataModel, params.referenceDataModelId)) {
            return forbiddenOrNotFound(canRead, ReferenceDataModel, params.referenceDataModelId)
        }
        if (!currentUserSecurityPolicyManager.userCanReadSecuredResourceId(ReferenceDataModel, params.otherReferenceDataModelId)) {
            return notFound(ReferenceDataModel, params.otherReferenceDataModelId)
        }
        true
    }
}
