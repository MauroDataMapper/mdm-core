/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.security.rest.transport

import grails.validation.Validateable;

class ChangePassword implements Validateable {

    String newPassword
    String oldPassword
    UUID resetToken

    static constraints = {
        newPassword nullable: false
        oldPassword nullable: true, validator: {val, obj ->
            if (obj.resetToken && val) return ['invalid.change.password.multiple.options']
            if (!obj.resetToken && !val) return ['invalid.change.password.no.options']
            true
        }
        resetToken nullable: true
    }
}