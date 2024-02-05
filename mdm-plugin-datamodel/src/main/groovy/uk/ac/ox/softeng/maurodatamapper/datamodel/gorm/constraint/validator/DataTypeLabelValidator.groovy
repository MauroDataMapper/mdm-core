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
package uk.ac.ox.softeng.maurodatamapper.datamodel.gorm.constraint.validator

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.validator.UniqueStringValidator

/**
 * @since 19/04/2018
 */
class DataTypeLabelValidator extends UniqueStringValidator<DataType> {

    DataTypeLabelValidator(DataType object) {
        super(object)
    }

    @Override
    boolean objectParentIsNotSaved() {
        !object.dataModel?.ident()
    }

    @Override
    boolean valueIsNotUnique(String value) {
        // We can expect multiple DTs to be added at the same time to an already saved DM,
        // therefore we have to take the hit of getting all DTs and doing an in memory search
        object.dataModel.countDataTypesByLabel(value) > 1
    }
}
