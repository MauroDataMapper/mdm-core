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

import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.VersionAware
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.validator.Validator
import uk.ac.ox.softeng.maurodatamapper.version.Version

class ModelVersionValidator implements Validator<Version> {

    final VersionAware versionAware

    ModelVersionValidator(VersionAware versionAware) {
        this.versionAware = versionAware
    }

    @Override
    Object isValid(Version modelVersion) {
        if (versionAware.ident() && versionAware.isDirty('modelVersion') && versionAware.getOriginalValue('modelVersion')) {
            return ['version.aware.model.version.change.not.allowed']
        }
        if (modelVersion && versionAware.branchName != VersionAwareConstraints.DEFAULT_BRANCH_NAME) {
            return ['version.aware.model.version.cannot.be.set.on.branch']
        }
        if (modelVersion && !versionAware.finalised) {
            return ['version.aware.model.version.can.only.set.on.finalised.model']
        }
        if (!modelVersion && versionAware.finalised) {
            return ['version.aware.model.version.must.be.set.on.finalised.model']
        }
        true
    }
}
