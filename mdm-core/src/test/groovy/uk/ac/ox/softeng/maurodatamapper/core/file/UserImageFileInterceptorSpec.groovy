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
package uk.ac.ox.softeng.maurodatamapper.core.file

import uk.ac.ox.softeng.maurodatamapper.core.file.UserImageFileInterceptor

import grails.testing.web.interceptor.InterceptorUnitTest
import spock.lang.Specification

class UserImageFileInterceptorSpec extends Specification implements InterceptorUnitTest<UserImageFileInterceptor> {

    def setup() {
    }

    def cleanup() {

    }

    void 'Test userImageFile interceptor matching'() {
        when: 'A request matches the interceptor'
        withRequest(controller: 'userImageFile')

        then: 'The interceptor does match'
        interceptor.doesMatch()
    }
}
