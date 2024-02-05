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
package uk.ac.ox.softeng.maurodatamapper.core.diff

import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import groovy.transform.CompileStatic
import groovy.transform.SelfType

@SelfType(MdmDomain)
@CompileStatic
trait Diffable<T extends Diffable> {

    abstract ObjectDiff<T> diff(T that, String context)

    abstract ObjectDiff<T> diff(T that, String context, DiffCache lhsDiffCache, DiffCache rhsDiffCache)

    String getDiffIdentifier() {
        getDiffIdentifier(null)
    }

    String getDiffIdentifier(String context) {
        getPathIdentifier()
    }

    abstract String getPathPrefix()

    abstract Path getPath()
}