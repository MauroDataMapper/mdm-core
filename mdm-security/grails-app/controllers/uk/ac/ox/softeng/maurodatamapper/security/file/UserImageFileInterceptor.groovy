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
package uk.ac.ox.softeng.maurodatamapper.security.file

import uk.ac.ox.softeng.maurodatamapper.core.file.UserImageFile
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.util.Utils

/**
 * Interceptor to all checking of access to user images through the CatalogueUser
 * Intercept happens before the standard UserImageFileInterceptor
 */
class UserImageFileInterceptor implements MdmInterceptor {

    UserImageFileInterceptor() {
        match(controller: 'userImageFile')
    }

    @Override
    boolean before() {
        // UserImageFile access looks at id or userId so we add this to the param list
        Utils.toUuid(params, 'id')
        Utils.toUuid(params, 'userId')
        Utils.toUuid(params, 'catalogueUserId')
        if (params.catalogueUserId) params.userId = params.catalogueUserId

        // Anyone can read another user's image
        if (isShow()) return true

        // If its the current user acting on itself then they can do anything to their image
        if (currentUser.id == params.catalogueUserId) return true

        // Otherwise check user permissions
        checkActionAuthorisationOnUnsecuredResource(UserImageFile, params.id, CatalogueUser, params.catalogueUserId)
    }
}
