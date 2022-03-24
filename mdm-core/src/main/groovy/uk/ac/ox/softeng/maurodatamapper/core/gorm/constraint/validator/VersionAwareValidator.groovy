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
package uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.validator


import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.VersionAware
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.validator.Validator

trait VersionAwareValidator implements Validator<String> {

    abstract VersionAware getVersionAware()

    def checkLabelValidity(Set<VersionAware> versionAwaresWithTheSameLabel) {
        // No versionAwares with the same label then we know the label is truely unique
        if (!versionAwaresWithTheSameLabel) return true
        if (versionAware) versionAwaresWithTheSameLabel.add(versionAware)

        boolean allHaveDifferentDocVersions = allVersionAwaresHaveDifferentDocumentationVersions(versionAwaresWithTheSameLabel)

        // If no model version then this is a branch from a finalised version
        // Likely to have the same doc version as other versionAwares so we have to make sure all the branch names are different
        if (!versionAware.modelVersion) {

            boolean allHaveDifferentBranchNames = allVersionAwaresWithoutVersionAwareVersionHaveDifferentBranchNames(versionAwaresWithTheSameLabel)
            // Same doc versions and same branch names
            if (!allHaveDifferentDocVersions && !allHaveDifferentBranchNames) return ['version.aware.label.not.unique.same.branch.names']
            // Different branch names and same or different doc versions
            // Same branch names and different doc versions
            return true
        }

        boolean allHaveDifferentVersionAwareVersions = allVersionAwaresHaveDifferentVersionAwareVersions(versionAwaresWithTheSameLabel)
        // Not a branch so we check the versions are all acceptable
        if (!allHaveDifferentDocVersions && !allHaveDifferentVersionAwareVersions) return ['version.aware.label.not.unique.same.versions']

        // All different doc versions and all different model versions
        // Same doc versions and all different model versions
        true
    }

    boolean allVersionAwaresHaveDifferentDocumentationVersions(Set<VersionAware> versionAwaresWithTheSameLabel) {
        versionAwaresWithTheSameLabel
            .groupBy {it.documentationVersion}
            .every {it.value.size() == 1}
    }

    boolean allVersionAwaresHaveDifferentVersionAwareVersions(Set<VersionAware> versionAwaresWithTheSameLabel) {
        versionAwaresWithTheSameLabel
            .findAll {it.modelVersion}
            .groupBy {it.modelVersion}
            .every {it.value.size() == 1}
    }

    boolean allVersionAwaresWithoutVersionAwareVersionHaveDifferentBranchNames(Set<VersionAware> versionAwaresWithTheSameLabel) {
        versionAwaresWithTheSameLabel
            .findAll {!it.modelVersion}
            .groupBy {it.branchName}
            .every {it.value.size() == 1}
    }
}
