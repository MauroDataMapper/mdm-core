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

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder

/**
 *
 * @since 30/01/2018
 */
class FolderLabelValidator extends LabelValidator {

    final Folder folder

    FolderLabelValidator(Folder folder) {
        this.folder = folder
    }

    @Override
    Object isValid(String value) {
        def res = super.isValid(value)
        if (res !instanceof Boolean) return res
        //parentFolder is nullable, and id may be null at this point
        if (folder.parentFolder == null || folder.parentFolder.ident()) {
            if (Folder.countByLabelAndParentFolderAndIdNotEqual(value, folder.parentFolder, folder.id)) return ['default.not.unique.message']
        } else {
            // parentFolder is transient, so check label within siblings
            if (folder.parentFolder.childFolders.find {it.label == value && it != folder}) return ['default.not.unique.message']
        }
        true
    }
}
