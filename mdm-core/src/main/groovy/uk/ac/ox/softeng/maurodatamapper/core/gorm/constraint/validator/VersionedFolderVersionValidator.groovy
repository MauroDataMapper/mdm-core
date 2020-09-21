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

import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.validator.Validator
import uk.ac.ox.softeng.maurodatamapper.util.Version

class VersionedFolderVersionValidator implements Validator<Version> {

    final VersionedFolder collection

    VersionedFolderVersionValidator(VersionedFolder collection) {
        this.collection = collection
    }

    @Override
    Object isValid(Version collectionVersion) {
        if (collection.ident() && collection.isDirty('collectionVersion') && collection.getOriginalValue('collectionVersion')) {
            return ['collection.collection.version.change.not.allowed']
        }
        if (collectionVersion && !collection.finalised) {
            return ['collection.collection.version.can.only.set.on.finalised.collection']
        }
        if (!collectionVersion && collection.finalised) {
            return ['collection.collection.version.must.be.set.on.finalised.collection']
        }
        true
    }
}
