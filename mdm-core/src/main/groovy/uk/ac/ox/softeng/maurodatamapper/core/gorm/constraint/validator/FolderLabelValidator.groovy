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

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.validator.Validator

/**
 *
 * @since 30/01/2018
 */
class FolderLabelValidator implements Validator<String> {

    private static String NOT_UNIQUE_MSG = 'default.not.unique.message'

    final Folder folder

    FolderLabelValidator(Folder folder) {
        this.folder = folder
    }

    @Override
    Object isValid(String value) {
        if (value == null) return ['default.null.message']
        if (!value) return ['default.blank.message']

        if (folder.parentFolder) {
            if (folder.id) {
                if (Folder.countByLabelAndParentFolderAndIdNotEqual(value,
                                                                    folder.parentFolder,
                                                                    folder.id)) return [NOT_UNIQUE_MSG]
            } else if (Folder.countByLabelAndParentFolder(value, folder.parentFolder)) return [NOT_UNIQUE_MSG]
        } else {
            if (folder.id) {
                if (Folder.countByLabelAndParentFolderIsNullAndIdNotEqual(value, folder.id)) return [NOT_UNIQUE_MSG]
            } else if (Folder.countByLabelAndParentFolderIsNull(value)) return [NOT_UNIQUE_MSG]
        }

        true
    }
}
