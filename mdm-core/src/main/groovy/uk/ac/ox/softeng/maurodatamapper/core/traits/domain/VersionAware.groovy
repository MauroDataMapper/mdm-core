/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.core.traits.domain


import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain
import uk.ac.ox.softeng.maurodatamapper.version.Version

import grails.databinding.BindUsing
import groovy.transform.CompileStatic
import groovy.transform.SelfType

import java.time.OffsetDateTime

@CompileStatic
@SelfType([InformationAware, MdmDomain])
trait VersionAware {

    String branchName
    Boolean finalised
    OffsetDateTime dateFinalised

    @BindUsing({ obj, source -> Version.from(source['modelVersion'] as String) })
    Version modelVersion

    String modelVersionTag

    @BindUsing({ obj, source -> Version.from(source['documentationVersion'] as String) })
    Version documentationVersion

    void initialiseVersioning() {
        setDocumentationVersion Version.from('1')
        finalised = false
        branchName = VersionAwareConstraints.DEFAULT_BRANCH_NAME
    }

    String getModelIdentifier() {
        "${modelVersion ?: branchName}"
    }
}
