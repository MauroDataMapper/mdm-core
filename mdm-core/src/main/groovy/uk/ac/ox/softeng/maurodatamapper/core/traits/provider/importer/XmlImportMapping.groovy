/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.core.traits.provider.importer

import groovy.xml.slurpersupport.NodeChild
import groovy.xml.slurpersupport.NodeChildren

/**
 * @since 08/10/2020
 */
trait XmlImportMapping {

    Map<String, Object> convertToMap(NodeChild nodes) {
        Map<String, Object> map = [:]
        if (nodes.children().isEmpty()) {
            map[nodes.name()] = nodes.text()
        } else {
            map = ((NodeChildren) nodes.children()).findAll {it.name() != 'id'}.collectEntries {NodeChild child ->
                String name = child.name()
                def content = child.text()

                if (child.childNodes()) {
                    Collection<String> childrenNames = child.children().list().collect {it.name().toLowerCase()}.toSet()

                    if (childrenNames.size() == 1 && child.name().toLowerCase().contains(childrenNames[0])) content = convertToList(child)
                    else content = convertToMap(child)

                }

                [name, content]
            }
        }
        map
    }

    List convertToList(NodeChild nodeChild) {
        nodeChild.children().collect {convertToMap(it)}
    }
}