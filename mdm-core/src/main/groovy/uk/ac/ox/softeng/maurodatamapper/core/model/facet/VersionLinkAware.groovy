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
package uk.ac.ox.softeng.maurodatamapper.core.model.facet

import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLink
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.VersionAware

import grails.compiler.GrailsCompileStatic
import groovy.transform.SelfType
import org.grails.datastore.gorm.GormEntity

/**
 * @since 30/01/2020
 */
@SelfType([MultiFacetAware, VersionAware, GormEntity])
@GrailsCompileStatic
trait VersionLinkAware {

    abstract Set<VersionLink> getVersionLinks()

    def addToVersionLinks(VersionLink add) {
        add.setModel(this as Model)
        addTo('versionLinks', add)
    }

    def addToVersionLinks(Map args) {
        addToVersionLinks(new VersionLink(args))
    }

    def removeFromVersionLinks(VersionLink versionLinks) {
        removeFrom('versionLinks', versionLinks)
    }
}
