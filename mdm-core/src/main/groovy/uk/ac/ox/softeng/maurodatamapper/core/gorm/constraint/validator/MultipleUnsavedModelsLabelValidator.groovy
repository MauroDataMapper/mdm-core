/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
 * @since 24/03/2022
 */
class MultipleUnsavedModelsLabelValidator implements Validator<Collection<Model>>, VersionAwareValidator {

    @Override
    Object isValid(Collection<Model> value) {
        Map<String, Collection<Model>> labelGrouped = value.groupBy {it.label}
        if (labelGrouped.every {it.value.size() == 1}) return true

        boolean valid = true
        // Only need to check those which have more than 1 model with the same label
        labelGrouped.each {label, models ->
            if (models.size() > 1) {
                def result = checkLabelValidity(models.toSet() as Set<VersionAware>)
                if (result instanceof List) {
                    valid = false
                    models.each {model ->
                        model.errors.rejectValue('label', result.first() as String,
                                                 new Object[]{'label', model.class, model.label},
                                                 'Property [{0}] of class [{1}] with value [{2}] must be unique by version')
                    }

                }
            }
        }
        valid
    }

    @Override
    VersionAware getVersionAware() {
        return null
    }
}
