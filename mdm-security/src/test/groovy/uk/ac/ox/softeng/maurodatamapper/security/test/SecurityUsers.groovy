/*
 * Copyright 2020 University of Oxford
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
package uk.ac.ox.softeng.maurodatamapper.security.test

import uk.ac.ox.softeng.maurodatamapper.security.utils.SecurityDefinition

trait SecurityUsers extends SecurityDefinition {

    void implementSecurityUsers(String creatorKey) {
        createModernSecurityUsers(creatorKey)
        checkAndSave(admin)
        checkAndSave(editor)
        checkAndSave(pending)
        checkAndSave(reader)
        checkAndSave(authenticated)
        checkAndSave(author)
        checkAndSave(reviewer)
        checkAndSave(containerAdmin)
    }

    void implementBasicSecurity(String creatorKey) {
        implementSecurityUsers(creatorKey)
        implementBasicGroups(creatorKey)
    }

    void implementBasicGroups(String creatorKey) {
        createBasicGroups(creatorKey)
        checkAndSave(admins)
        checkAndSave(editors)
        checkAndSave(readers)
    }
}
