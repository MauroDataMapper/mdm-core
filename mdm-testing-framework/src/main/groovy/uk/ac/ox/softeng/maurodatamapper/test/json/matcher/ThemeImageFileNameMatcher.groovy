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

import org.hamcrest.BaseMatcher
import org.hamcrest.Description


/**
 * @since 15/03/2023
 */
class ThemeImageFileNameMatcher extends IdMatcher {
    @Override
    boolean matches(Object item) {
        if (item instanceof String) {
            def suffix = item.tokenize('-').last()
            def uuidString = item.replace("-${suffix}","")
            return super.matches(uuidString) && (suffix == "theme")
        }
        false
    }

    @Override
    void describeTo(Description description) {
        description.appendText('Filename must be made up of a parseable UUID and the suffix "-theme"')
    }

    @Override
    void describeMismatch(Object item, Description description) {
        description.appendText('Filename must be made up of a parseable UUID and the suffix "-theme"')
    }
}
