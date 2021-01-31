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
package uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.validator

import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.VersionAware
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.validator.Validator

/**
 *
 * @since 30/01/2018
 */
class ModelLabelValidator implements Validator<String>, VersionAwareValidator {

    final Model model

    ModelLabelValidator(Model model) {
        this.model = model
    }

    Class getModelClass() {
        model.getClass()
    }

    @Override
    VersionAware getVersionAware() {
        model
    }

    @Override
    Object isValid(String value) {
        if (value == null) return ['default.null.message']
        if (!value) return ['default.blank.message']

        List<Model> modelsWithTheSameLabel
        // Existing models can change label but it must not already be in use
        if (model.ident()) modelsWithTheSameLabel = modelClass.findAllByLabelAndIdNotEqual(value, model.ident())
        else modelsWithTheSameLabel = modelClass.findAllByLabel(value)

        checkLabelValidity(modelsWithTheSameLabel.toSet() as Set<VersionAware>)

    }
}
