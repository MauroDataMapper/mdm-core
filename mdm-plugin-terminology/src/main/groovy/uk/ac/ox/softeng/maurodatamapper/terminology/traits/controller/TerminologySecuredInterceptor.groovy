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
package uk.ac.ox.softeng.maurodatamapper.terminology.traits.controller

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.util.Utils

/**
 * @since 20/03/2020
 */
abstract class TerminologySecuredInterceptor implements MdmInterceptor {

    abstract Class getModelItemClass()

    void checkIds() {
        Utils.toUuid(params, 'terminologyId')
        Utils.toUuid(params, 'id')
    }

    void checkTerminologyId() {
        if (!params.terminologyId) throw new ApiBadRequestException('TSI01', 'No Terminology Id provided against secured resource')
    }

    void performChecks() {
        checkIds()
        checkTerminologyId()
    }

    boolean checkStandardActions() {
        checkActionAuthorisationOnUnsecuredResource(getModelItemClass(), params.id, Terminology, params.terminologyId)
    }

    boolean canReadTerminology() {
        currentUserSecurityPolicyManager.userCanReadSecuredResourceId(Terminology, params.terminologyId) ?:
        notFound(Terminology, params.terminologyId.toString())
    }

    boolean canCopyFromTerminologyToOtherTerminology() {
        boolean canRead = currentUserSecurityPolicyManager.userCanReadSecuredResourceId(Terminology, params.terminologyId)
        if (!currentUserSecurityPolicyManager.userCanEditSecuredResourceId(Terminology, params.terminologyId)) {
            return forbiddenOrNotFound(canRead, Terminology, params.terminologyId)
        }
        if (!currentUserSecurityPolicyManager.userCanReadSecuredResourceId(Terminology, params.otherTerminologyId)) {
            return notFound(Terminology, params.otherTerminologyId)
        }
        true
    }
}
