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
package uk.ac.ox.softeng.maurodatamapper.gorm.constraint.validator

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

/**
 * @since 13/02/2018
 */
@CompileStatic
class UniqueValuesValidator implements Validator<Collection> {

    String uniqueProperty
    String uniquePropertyPath

    UniqueValuesValidator(String uniqueProperty) {
        this(uniqueProperty, uniqueProperty)
    }

    UniqueValuesValidator(String uniqueProperty, String uniquePropertyPath) {
        this.uniqueProperty = uniqueProperty
        this.uniquePropertyPath = uniquePropertyPath
    }

    @Override
    Object isValid(Collection value) {
        if (!value) return true
        Map<String, List> allGrouped = groupCollection(value)
        isValid(allGrouped)
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    Map<String, List> groupCollection(Collection value) {
        value.groupBy {it."${uniqueProperty}"} as Map<String, List>
    }

    Object isValid(Map<String, List> allGrouped) {
        List<String> nonUnique = allGrouped.findAll {it.value.size() != 1}.collect {it.key}
        if (nonUnique) return ['invalid.unique.values.message', nonUnique.sort().join(', '), uniquePropertyPath]
        true
    }
}
