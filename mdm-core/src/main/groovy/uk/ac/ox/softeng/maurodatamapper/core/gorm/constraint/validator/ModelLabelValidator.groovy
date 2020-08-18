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
package uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.validator

import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.validator.Validator

/**
 *
 * @since 30/01/2018
 */
class ModelLabelValidator implements Validator<String> {

    final Model model

    ModelLabelValidator(Model model) {
        this.model = model
    }

    Class getModelClass() {
        model.getClass()
    }

    @Override
    Object isValid(String value) {
        if (value == null) return ['default.null.message']
        if (!value) return ['default.blank.message']

        List<Model> modelsWithTheSameLabel
        // Existing models can change label but it must not already be in use
        if (model.ident()) modelsWithTheSameLabel = modelClass.findAllByLabelAndIdNotEqual(value, model.ident())
        else modelsWithTheSameLabel = modelClass.findAllByLabel(value)

        checkLabelValidity(modelsWithTheSameLabel.toSet())

    }

    def checkLabelValidity(Set<Model> modelsWithTheSameLabel) {
        // No models with the same label then we know the label is truely unique
        if (!modelsWithTheSameLabel) return true
        modelsWithTheSameLabel.add(model)

        boolean allHaveDifferentDocVersions = allModelsHaveDifferentDocumentationVersions(modelsWithTheSameLabel)

        // If no model version then this is a branch from a finalised version
        // Likely to have the same doc version as other models so we have to make sure all the branch names are different
        if (!model.modelVersion) {

            boolean allHaveDifferentBranchNames = allModelsWithoutModelVersionHaveDifferentBranchNames(modelsWithTheSameLabel)
            // Same doc versions and same branch names
            if (!allHaveDifferentDocVersions && !allHaveDifferentBranchNames) return ['model.label.not.unique.same.branch.names']
            // Different branch names and same or different doc versions
            // Same branch names and different doc versions
            return true
        }

        boolean allHaveDifferentModelVersions = allModelsHaveDifferentModelVersions(modelsWithTheSameLabel)
        // Not a branch so we check the versions are all acceptable
        if (!allHaveDifferentDocVersions && !allHaveDifferentModelVersions) return ['model.label.not.unique.same.versions']

        // All different doc versions and all different model versions
        // Same doc versions and all different model versions
        true
    }

    boolean allModelsHaveDifferentDocumentationVersions(Set<Model> modelsWithTheSameLabel) {
        modelsWithTheSameLabel
            .groupBy {it.documentationVersion}
            .every {it.value.size() == 1}
    }

    boolean allModelsHaveDifferentModelVersions(Set<Model> modelsWithTheSameLabel) {
        modelsWithTheSameLabel
            .findAll {it.modelVersion}
            .groupBy {it.modelVersion}
            .every {it.value.size() == 1}
    }

    boolean allModelsWithoutModelVersionHaveDifferentBranchNames(Set<Model> modelsWithTheSameLabel) {
        modelsWithTheSameLabel
            .findAll {!it.modelVersion}
            .groupBy {it.branchName}
            .every {it.value.size() == 1}
    }
}
