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
package uk.ac.ox.softeng.maurodatamapper.core.rest.render

import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import grails.rest.render.ContainerRenderer

/**
 * @since 04/01/2021
 * @see https://github.com/grails/grails-core/blob/master/grails-plugin-rest/src/main/groovy/grails/rest/render/atom/AtomCollectionRenderer.groovy
 * for inspiration.
 */
class MdmAtomModelCollectionRenderer extends MdmAtomModelRenderer implements ContainerRenderer {

    final Class componentType

    MdmAtomModelCollectionRenderer() {
        super(Collection)
        this.componentType = Model
    }
}