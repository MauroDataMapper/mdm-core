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
package uk.ac.ox.softeng.maurodatamapper.core.container

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.validator.ModelCollectionVersionValidator
import uk.ac.ox.softeng.maurodatamapper.util.Version

import grails.databinding.BindUsing

import java.time.OffsetDateTime

class ModelCollection extends Folder {

    Authority authority
    Boolean finalised
    OffsetDateTime dateFinalised

    @BindUsing({ obj, source -> Version.from(source['collectionVersion'] as String) })
    Version collectionVersion

    Folder parentFolder

    static belongsTo = [Authority, Folder]

    static constraints = {
        //authority validator: { Authority val, ModelCollection obj -> }
        collectionVersion nullable: true, validator: { Version val, ModelCollection obj -> new ModelCollectionVersionValidator(obj).isValid(val) }
        finalised nullable: false
        dateFinalised nullable: true
        parentFolder nullable: true,
                     validator: { Folder val, ModelCollection obj ->
                         if (val?.class == ModelCollection) ['collection.parent.folder.cannot.be.a.collection']
                     }
    }

    ModelCollection() {
        super()
        finalised = false
    }

    @Override
    String getEditLabel() {
        "ModelCollection:${label}"
    }
}
