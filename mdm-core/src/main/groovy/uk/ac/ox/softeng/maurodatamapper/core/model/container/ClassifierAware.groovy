/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.core.model.container

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier

import groovy.transform.CompileStatic
import groovy.transform.SelfType
import org.grails.datastore.gorm.GormEntity

/**
 * @since 20/
 * 04/2018
 */
@SelfType(GormEntity)
@CompileStatic
trait ClassifierAware<K> {

    abstract Set<Classifier> getClassifiers()

    K addToClassifiers(Classifier classifier) {
        addTo('classifiers', classifier) as K
    }

    K addToClassifiers(Map args) {
        addTo('classifiers', args) as K
    }

    K removeFromClassifiers(Classifier classifier) {
        throw new ApiInternalException('CL01', 'Do not use removeFrom to remove classifier from domain')
    }

    static abstract K findByIdJoinClassifiers(UUID id)
}
