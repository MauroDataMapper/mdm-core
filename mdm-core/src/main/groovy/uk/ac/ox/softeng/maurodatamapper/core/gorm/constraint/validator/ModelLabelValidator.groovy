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
package uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.validator

import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.VersionAware

/**
 *
 * @since 30/01/2018
 */
class ModelLabelValidator extends LabelValidator implements VersionAwareValidator {

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
        def res = super.isValid(value)
        if (res !instanceof Boolean) return res

        List<Model> modelsWithTheSameLabel
        // Existing models can change label but it must not already be in use
        // The same label is allowed for a different authority
        if (model.ident()) modelsWithTheSameLabel = modelClass.findAllByLabelAndAuthorityAndIdNotEqual(value, model.authority, model.ident())
        else modelsWithTheSameLabel = modelClass.findAllByLabelAndAuthority(value, model.authority)

        checkLabelValidity(modelsWithTheSameLabel.toSet() as Set<VersionAware>)

    }
}
