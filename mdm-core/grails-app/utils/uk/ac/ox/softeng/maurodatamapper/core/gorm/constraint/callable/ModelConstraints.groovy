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
package uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable

import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.validator.DocumentationVersionValidator
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.validator.ModelLabelValidator
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.validator.ModelVersionValidator
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.util.Version

/**
 * @since 17/02/2020
 */
class ModelConstraints extends CatalogueItemConstraints {

    public static final String DEFAULT_BRANCH_NAME = 'main'

    static constraints = {
        folder nullable: false
        deleted nullable: false
        finalised nullable: false
        modelType nullable: false, blank: false
        documentationVersion nullable: false, validator: {Version val, Model obj -> new DocumentationVersionValidator(obj).isValid(val)}
        author nullable: true, blank: false
        organisation nullable: true, blank: false
        dateFinalised nullable: true

        modelVersion nullable: true, validator: {Version val, Model obj -> new ModelVersionValidator(obj).isValid(val)}
        branchName nullable: false

        label validator: {String val, Model obj -> new ModelLabelValidator(obj).isValid(val)}
    }
}
