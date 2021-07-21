/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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

import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.CatalogueFileConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.file.CatalogueFile
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints

import grails.gorm.DetachedCriteria
import grails.rest.Resource

import java.util.regex.Pattern

/**
 * @since 07/02/2020
 */
@Resource(readOnly = false, formats = ['json', 'xml'])
class UserImageFile implements CatalogueFile {

    public static final String NO_PROFILE_IMAGE_FILE_NAME = 'no_profile_image.png'
    private static final Pattern PRECUSOR = ~/^data:image\/[^;]*;base64,?/

    UUID id
    UUID userId

    static constraints = {
        CallableConstraints.call(CatalogueFileConstraints, delegate)
    }

    UserImageFile() {
    }

    @Override
    String getDomainType() {
        UserImageFile.simpleName
    }

    @Override
    String getPathPrefix() {
        'uif'
    }

    def beforeValidate() {
        if (!fileName) fileName = "${userId}-profile"
        fileSize = fileContents?.size()
    }

    void setImage(String image) {
        fileContents = image?.replaceFirst(PRECUSOR, '')?.decodeBase64()
    }

    void setType(String type) {
        fileType = type
    }

    static DetachedCriteria<UserImageFile> byUserId(UUID catalogueUserId) {
        new DetachedCriteria<UserImageFile>(UserImageFile).eq('userId', catalogueUserId)
    }

    static DetachedCriteria<UserImageFile> withFilter(DetachedCriteria<UserImageFile> criteria, Map filters) {
        criteria = withBaseFilter(criteria, filters)
        if (filters.userId) criteria = criteria.ilike('userId', "%${filters.userId}%")
        criteria
    }
}
