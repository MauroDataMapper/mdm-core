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
package uk.ac.ox.softeng.maurodatamapper.terminology.gorm.constraint.validator

import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.validator.UniqueStringValidator
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipType

/**
 * @since 19/04/2018
 */
class TermRelationshipTypeLabelValidator extends UniqueStringValidator<TermRelationshipType> {

    TermRelationshipTypeLabelValidator(TermRelationshipType termRelationshipType) {
        super(termRelationshipType)
    }

    @Override
    boolean objectParentIsNotSaved() {
        !object.terminology?.ident()
    }

    @Override
    boolean valueIsNotUnique(String value) {
        // We can expect multiple DTs to be added at the same time to an already saved DM,
        // therefore we have to take the hit of getting all DTs and doing an in memory search
        object.terminology.countTermRelationshipTypesByLabel(value) > 1
    }
}
