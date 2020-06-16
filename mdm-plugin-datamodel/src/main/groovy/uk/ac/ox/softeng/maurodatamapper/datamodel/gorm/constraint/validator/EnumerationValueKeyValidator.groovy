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
package uk.ac.ox.softeng.maurodatamapper.datamodel.gorm.constraint.validator

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration.EnumerationValue
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.validator.UniqueStringValidator

/**
 * @since 27/02/2020
 */
class EnumerationValueKeyValidator extends UniqueStringValidator<EnumerationValue> {

    EnumerationValueKeyValidator(EnumerationValue object) {
        super(object)
    }

    @Override
    boolean objectParentIsNotSaved() {
        !object.enumerationType?.ident()
    }

    @Override
    boolean valueIsNotUnique(String value) {
        object.enumerationType.countEnumerationValuesByKey(value) > 1
    }
}
