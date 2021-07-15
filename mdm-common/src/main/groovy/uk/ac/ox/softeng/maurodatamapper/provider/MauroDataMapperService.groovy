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
package uk.ac.ox.softeng.maurodatamapper.provider


import uk.ac.ox.softeng.maurodatamapper.version.Version

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Slf4j
@CompileStatic
abstract class MauroDataMapperService implements MauroDataMapperProvider, Comparable<MauroDataMapperService> {

    abstract String getDisplayName()

    abstract String getProviderType()

    @Override
    String getName() {
        getClass().getSimpleName()
    }

    String getNamespace() {
        getClass().getPackage().getName()
    }

    Boolean allowsExtraMetadataKeys() {
        true
    }

    Set<String> getKnownMetadataKeys() {
        [] as HashSet
    }

    Logger getLog() {
        LoggerFactory.getLogger(getClass())
    }

    @Override
    String toString() {
        "${namespace} : ${name} : ${version}"
    }

    Version sortableVersion() {
        Version.from(version)
    }

    @Override
    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        MauroDataMapperService that = (MauroDataMapperService) o

        if (this.providerType != that.providerType) return false
        if (this.namespace != that.namespace) return false
        if (this.name != that.name) return false
        if (this.version != that.version) return false
        true
    }

    @Override
    int hashCode() {
        int result
        result = providerType.hashCode()
        result = 31 * result + namespace.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + version.hashCode()
        result
    }

    @Override
    int compareTo(MauroDataMapperService that) {
        int result = this.providerType <=> that.providerType
        if (result == 0) result = this.namespace <=> that.namespace
        if (result == 0) result = this.name <=> that.name
        if (result == 0) result = this.sortableVersion() <=> that.sortableVersion()
        result
    }
}
