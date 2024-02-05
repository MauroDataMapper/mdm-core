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
package uk.ac.ox.softeng.maurodatamapper.security.basic

import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.security.User

/**
 * @since 21/10/2019
 */
@Singleton
class UnloggedUser implements User {

    public static final String UNLOGGED_EMAIL_ADDRESS = 'unlogged_user@mdm-core.com'

    String emailAddress = UNLOGGED_EMAIL_ADDRESS
    String firstName = 'Unlogged'
    String lastName = 'User'
    String tempPassword

    @Override
    UUID getId() {
        UUID.randomUUID()
    }

    UUID ident() {
        id
    }

    @Override
    Path getPath() {
        Path.from('cu', emailAddress)
    }

    @Override
    String getDomainType() {
        UnloggedUser
    }
}
