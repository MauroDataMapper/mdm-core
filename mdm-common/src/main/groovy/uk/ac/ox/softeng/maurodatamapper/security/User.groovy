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
package uk.ac.ox.softeng.maurodatamapper.security

/**
 * @since 08/10/2019
 */
trait User extends SecurableResource {

    abstract UUID getId()

    abstract String getEmailAddress()

    abstract void setEmailAddress(String emailAddress)

    abstract String getFirstName()

    abstract void setFirstName(String firstName)

    abstract String getLastName()

    abstract void setLastName(String lastName)

    abstract String getTempPassword()

    abstract void setTempPassword(String tempPassword)

    String toString() {
        getEmailAddress()
    }

    @Override
    Boolean getReadableByEveryone() {
        false
    }

    @Override
    Boolean getReadableByAuthenticatedUsers() {
        false
    }

    String getFullName() {
        "${firstName} ${lastName}"
    }
}