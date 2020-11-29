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
package uk.ac.ox.softeng.maurodatamapper.profile.object

import groovy.transform.CompileStatic
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FromString

@CompileStatic
abstract class MapBasedProfile extends Profile {

    private Map<String, Object> contents

    MapBasedProfile() {
        contents = new HashMap<>()
    }

    @Override
    Object getField(String fieldName) {
        contents[fieldName]
    }

    String getId() {
        contents.id
    }

    void setId(String id) {
        contents.id = id
    }

    Map<String, Object> each(@ClosureParams(value = FromString, options = ['Map<String,Object>', 'String,Object']) Closure closure) {
        contents.each(closure)
    }

    def propertyMissing(String name) {
        contents[name]
    }

    def propertyMissing(String name, def arg) {
        contents[name] = arg
    }

    @Override
    void setField(String fieldName, def value) {
        contents[fieldName] = value
    }

    Map<String, Object> getMap() {
        contents
    }
}
