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
package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model

import uk.ac.ox.softeng.maurodatamapper.util.Version
import uk.ac.ox.softeng.maurodatamapper.util.VersionChangeType

import grails.databinding.BindUsing
import grails.validation.Validateable

/**
 * @since 02/02/2018
 */
class FinaliseData implements Validateable {

    List<String> supersededBy = []

    @BindUsing({ obj, source ->
        VersionChangeType.findFromDataBindingSource(source)
    })
    VersionChangeType versionChangeType

    @BindUsing({ obj, source -> Version.from(source['version'] as String) })
    Version version

    String versionTag

    String changeNotice


    static constraints = {
        supersededBy nullable: true
        versionChangeType nullable: true, validator: { VersionChangeType value, FinaliseData obj ->
            if (!value && !obj.version) {
                ['model.finalise.must.provide.version.or.versionchangetype']
            }
        }
        version nullable: true
        versionTag nullable: true
        changeNotice nullable: true
    }
}
