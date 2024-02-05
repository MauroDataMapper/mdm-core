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
package uk.ac.ox.softeng.maurodatamapper.traits.domain


import uk.ac.ox.softeng.maurodatamapper.path.Path

import grails.compiler.GrailsCompileStatic
import groovy.transform.SelfType
import org.grails.datastore.gorm.GormEntity
import org.slf4j.LoggerFactory

import java.time.OffsetDateTime

/**
 * This is the base domain trait which all domain classes must extend/implement in the MDC.
 * @since 25/09/2017
 */
@SelfType(GormEntity)
@GrailsCompileStatic
trait MdmDomain {

    OffsetDateTime dateCreated
    OffsetDateTime lastUpdated
    String createdBy
    Path path

    abstract UUID getId()

    abstract String getDomainType()

    // Allow domains to not be "pathed". Also provides compatability
    abstract String getPathPrefix()

    // Allow domains to not be "pathed". Also provides compatability
    abstract String getPathIdentifier()

    Path getPath() {
        checkPath(false)
        this.@path
    }

    void setPath(Path path) {
        this.@path = path
    }

    Path buildPath() {
        Path.from(pathPrefix, pathIdentifier)
    }

    Path getUncheckedPath() {
        this.@path
    }

    void checkPath(boolean forceCheck = true) {
        if (this.@path) {
            if (forceCheck || !this.@path.checked) {
                //                LoggerFactory.getLogger(this.class).debug('Checking built but unchecked path {}', this.@path)
                Path builtPath = buildPath()
                if (this.@path != builtPath) {
                    setBuiltPath(builtPath)
                }
                if (this.@path) this.@path.checked = true
            }
            //            else {
            //                LoggerFactory.getLogger(this.class).debug('Path exists and already checked {}', this.@path)
            //            }

        } else {
            if (!pathPrefix || !pathIdentifier) {
                LoggerFactory.getLogger(this.class).trace('Cannot build path for {} as no prefix and/or identifier', domainType)
                return
            }
            setBuiltPath(buildPath())
        }
    }

    private void setBuiltPath(Path builtPath) {
        Path oldPath = this.@path
        markDirty('path', builtPath, oldPath)
        this.@path = builtPath
    }
}