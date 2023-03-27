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
package uk.ac.ox.softeng.maurodatamapper.core.file

import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiProperty
import uk.ac.ox.softeng.maurodatamapper.core.model.file.CatalogueFileService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

@Slf4j
@Transactional
class ThemeImageFileService implements CatalogueFileService<ThemeImageFile> {

    ThemeImageFile get(Serializable id) {
        ThemeImageFile.get(id)
    }

    List<ThemeImageFile> list(Map args = [:]) {
        ThemeImageFile.list(args)
    }

    Long count() {
        ThemeImageFile.count()
    }

    void delete(Serializable id) {
        delete(get(id))
    }

    void delete(ThemeImageFile themeImageFile) {
        themeImageFile.delete(flush: true)
    }

    ThemeImageFile findByApiPropertyId(UUID apiPropertyId) {
        ApiProperty apiProperty = ApiProperty.findById(Utils.toUuid(apiPropertyId))
        findByApiProperty(apiProperty);
    }

    ThemeImageFile findByApiProperty(ApiProperty apiProperty) {
        if (!apiProperty) return null
        if (!Utils.toUuid(apiProperty.value)) return null
        ThemeImageFile.findById(Utils.toUuid(apiProperty.value))
    }

    @Override
    ThemeImageFile createNewFile(String name, byte[] contents, String type, User user) {
        ThemeImageFile tif = createNewFileBase(name, contents, type, user.emailAddress)
        tif
    }
}