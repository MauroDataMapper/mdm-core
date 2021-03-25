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
package uk.ac.ox.softeng.maurodatamapper.core.traits.domain

import grails.compiler.GrailsCompileStatic
import groovy.transform.SelfType
import org.grails.datastore.gorm.GormEntity

/**
 * @since 20/09/2017
 */
@SelfType(GormEntity)
@GrailsCompileStatic
trait InformationAware {

    String description
    String label

    String toString() {
        "${getClass().getName()} (${label})[${ident() ?: '(unsaved)'}]"
    }

    void setLabel(String label) {
        //When importing from Json, a literal '\n' is parsed as a line feed.
        //When importing from Xml, a literal '\n' is escaped to '\\n'
        //So the regex replaces any linefeed characters, \n or \\n with a space.
        this.label = label?.replaceAll(/\R|\n|\\n/, ' ')
    }
}