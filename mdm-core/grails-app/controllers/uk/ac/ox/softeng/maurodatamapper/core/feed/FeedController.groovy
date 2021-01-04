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
package uk.ac.ox.softeng.maurodatamapper.core.feed

import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmController

import grails.rest.RestfulController

/**
 * Produce an ATOM feed of all Models. Only respond in ATOM format. To render the response in ATOM,
 * a bean needs to be configured in grails-app/conf/spring/resources.groovy as follows:
 * import uk.ac.ox.softeng.maurodatamapper.core.rest.render.MdmAtomModelCollectionRenderer
 * beans = {
 *   halModelListRenderer(MdmAtomModelCollectionRenderer) {
 *       includes = []
 *   }
 * }
 * 
 * @since 04/01/2021
 */
class FeedController extends RestfulController<Model> implements MdmController {

    static responseFormats = ['atom']

    FeedService feedService

    FeedController() {
        super(Model)
    }

    def index() {
        params.format = 'atom'
        respond feedService.findModels()
    }

}
