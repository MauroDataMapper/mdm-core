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
package uk.ac.ox.softeng.maurodatamapper.test.json.matcher

import net.javacrumbs.jsonunit.core.ParametrizedMatcher
import org.hamcrest.BaseMatcher
import org.hamcrest.Description

/**
 * @since 15/10/2018
 */
class ErrorMessageMatcher extends BaseMatcher<String> implements ParametrizedMatcher {

    String regex


    @Override
    boolean matches(Object item) {
        if (item instanceof String) {
            item.matches(regex)
        }
        false
    }

    @Override
    void describeTo(Description description) {
        description.appendText('It must be match the expected message')
    }

    @Override
    void describeMismatch(Object item, Description description) {
        description.appendText('It must be match the expected message')
    }

    @Override
    void setParameter(String parameter) {
        this.regex = parameter
    }
}
