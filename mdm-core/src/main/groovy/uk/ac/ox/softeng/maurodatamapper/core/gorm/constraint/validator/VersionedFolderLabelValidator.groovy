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
package uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.validator

import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.VersionAware

class VersionedFolderLabelValidator extends FolderLabelValidator implements VersionAwareValidator {

    VersionedFolderLabelValidator(VersionedFolder folder) {
        super(folder)
    }

    @Override
    Object isValid(String value) {
        def folderValidation = super.isValid(value)

        // Failed simple label validation as a folder therefore it needs to be checked if it passes versioned validation
        if (folderValidation instanceof List && folderValidation.first() == 'default.not.unique.message') {

            List<VersionedFolder> versionedFoldersWithTheSameLabel
            // Existing models can change label but it must not already be in use
            if (folder.ident()) {
                versionedFoldersWithTheSameLabel = VersionedFolder.findAllByLabelAndAuthorityAndIdNotEqual(value, folder.authority, folder.id)
            } else versionedFoldersWithTheSameLabel = VersionedFolder.findAllByLabelAndAuthority(value, folder.authority)

            folderValidation = checkLabelValidity(versionedFoldersWithTheSameLabel.toSet() as Set<VersionAware>)
        }
        folderValidation
    }

    @Override
    VersionAware getVersionAware() {
        folder as VersionedFolder
    }
}
