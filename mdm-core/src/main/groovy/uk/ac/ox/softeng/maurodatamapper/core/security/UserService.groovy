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
package uk.ac.ox.softeng.maurodatamapper.core.security

import uk.ac.ox.softeng.maurodatamapper.core.file.UserImageFile
import uk.ac.ox.softeng.maurodatamapper.security.User

interface UserService {

    User findUser(String emailAddress)

    UserImageFile addImageCreatedEditToUser(User creator, UserImageFile domain, UUID userId)

    UserImageFile addImageUpdatedEditToUser(User editor, UserImageFile domain, UUID userId, List<String> dirtyPropertyNames)

    UserImageFile addImageDeletedEditToUser(User deleter, UserImageFile domain, UUID userId)

}