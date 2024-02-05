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
package uk.ac.ox.softeng.maurodatamapper.core.model.facet

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation

import grails.compiler.GrailsCompileStatic
import groovy.transform.SelfType
import org.grails.datastore.gorm.GormEntity

/**
 * @since 30/01/2020
 */
@SelfType([GormEntity])
@GrailsCompileStatic
trait AnnotationAware {
    abstract Set<Annotation> getAnnotations()

    def addToAnnotations(Annotation add) {
        add.setMultiFacetAwareItem(this as MultiFacetAware)
        addTo('annotations', add)
    }

    def addToAnnotations(Map args) {
        addToAnnotations(new Annotation(args))
    }

    def removeFromAnnotations(Annotation annotations) {
        throw new ApiInternalException('FR01', 'Do not use removeFrom to remove facet from domain')
    }
}
