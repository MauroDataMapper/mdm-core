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

import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.CatalogueFileConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.file.CatalogueFile
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import grails.rest.Resource

import java.util.regex.Pattern

/**
 * @since 07/02/2020
 */
@Resource(readOnly = false, formats = ['json', 'xml'])
abstract class ImageFile implements CatalogueFile, MdmDomain {

    private static final Pattern PRECURSOR = ~/^data:image\/[^;]*;base64,?/

    @Override
    String getDomainType() {
        ImageFile.simpleName
    }

    UUID id

    static constraints = {
        CallableConstraints.call(CatalogueFileConstraints, delegate)
    }

    ImageFile() {
    }

    @Override
    String getPathIdentifier() {
        fileName
    }

    void setImage(String image) {
        fileContents = image?.replaceFirst(PRECURSOR, '')?.decodeBase64()
    }

    void setType(String type) {
        fileType = type
    }
}
