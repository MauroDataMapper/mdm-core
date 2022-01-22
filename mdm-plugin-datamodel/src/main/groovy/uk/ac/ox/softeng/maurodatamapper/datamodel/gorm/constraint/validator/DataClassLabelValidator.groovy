/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.validator.UniqueStringValidator

/**
 * @since 19/04/2018
 */
class DataClassLabelValidator extends UniqueStringValidator<DataClass> {


    DataClassLabelValidator(DataClass object) {
        super(object)
    }

    @Override
    boolean objectParentIsNotSaved() {
        !object.model?.ident() || (object.parentDataClass && !object.parentDataClass.ident())
    }

    @Override
    boolean valueIsNotUnique(String value) {
        if (object.parentDataClass) {
            if (object.parentDataClass.dataClasses.count {it.label == value} > 1) return true
        } else {
            if (object.model.childDataClasses.count {it.label == value} > 1) return true
        }
        false
    }
}
