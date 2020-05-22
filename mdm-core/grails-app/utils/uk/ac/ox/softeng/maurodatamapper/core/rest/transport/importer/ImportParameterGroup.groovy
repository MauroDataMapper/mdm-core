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
package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.importer

import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import io.micronaut.core.order.Ordered

/**
 * @since 11/07/2018
 */
class ImportParameterGroup implements Comparable<ImportParameterGroup>, Ordered {

    String name
    int order = LOWEST_PRECEDENCE
    private final Set<ImportParameter> importParameters = [] as HashSet

    List<ImportParameter> getSortedImportParameters() {
        importParameters.sort()
    }

    ImportParameterGroup addToImportParameters(ImportParameter importParameter) {
        this.importParameters.add(importParameter)
        this
    }

    ImportParameterGroup addToImportParameters(Map importParameters) {
        this.importParameters.add(new ImportParameter(importParameters))
        this
    }

    @Override
    int compareTo(ImportParameterGroup that) {
        int res = this.order <=> that.order
        res == 0 ? this.name <=> that.name : res
    }

    int size() {
        importParameters.size()
    }

    ImportParameter find(@DelegatesTo(Set) @ClosureParams(value = SimpleType,
        options = 'uk.ac.ox.softeng.maurodatamapper.core.rest.transport.importer.ImportParameter') Closure closure) {
        importParameters.find closure
    }

    Boolean any(@DelegatesTo(Set) @ClosureParams(value = SimpleType,
        options = 'uk.ac.ox.softeng.maurodatamapper.core.rest.transport.importer.ImportParameter') Closure closure) {
        importParameters.any closure
    }

    ImportParameter get(int i) {
        sortedImportParameters[i]
    }

    ImportParameter getAt(int i) {
        get(i)
    }

    ImportParameter first() {
        get(0)
    }

    ImportParameter last() {
        get(-1)
    }
}
