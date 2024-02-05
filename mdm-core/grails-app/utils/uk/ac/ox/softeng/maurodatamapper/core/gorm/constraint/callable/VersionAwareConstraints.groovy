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
package uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable

import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.validator.DocumentationVersionValidator
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.validator.ModelVersionValidator
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.VersionAware
import uk.ac.ox.softeng.maurodatamapper.version.Version

class VersionAwareConstraints {

    public static final String DEFAULT_BRANCH_NAME = 'main'

    static constraints = {
        finalised nullable: false
        dateFinalised nullable: true

        documentationVersion nullable: false, validator: { Version val, VersionAware obj -> new DocumentationVersionValidator(obj).isValid(val) }
        modelVersion nullable: true, validator: { Version val, VersionAware obj -> new ModelVersionValidator(obj).isValid(val) }
        modelVersionTag nullable: true
        branchName nullable: false
    }
}
