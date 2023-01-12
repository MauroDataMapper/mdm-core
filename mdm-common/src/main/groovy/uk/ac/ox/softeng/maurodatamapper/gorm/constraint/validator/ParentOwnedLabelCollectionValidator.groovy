/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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

import org.grails.datastore.gorm.GormEntity

/**
 * @since 26/04/2018
 */
class ParentOwnedLabelCollectionValidator extends UniqueValuesValidator {

    GormEntity parent

    ParentOwnedLabelCollectionValidator(GormEntity parent, String uniquePropertyPath) {
        this(parent, 'label', uniquePropertyPath)
    }

    ParentOwnedLabelCollectionValidator(GormEntity parent, String uniqueProperty, String uniquePropertyPath) {
        super(uniqueProperty, uniquePropertyPath)
        this.parent = parent
    }

    @Override
    Object isValid(Collection value) {
        // If id set then validation will be performed at the element level
        parent.ident() ? true : super.isValid(value ?: [])
    }
}
