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
import uk.ac.ox.softeng.maurodatamapper.core.rest.render.MdmAtomModelCollectionRenderer
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.ResourcelessMdmController

import grails.artefact.Controller
import grails.rest.render.Renderer
import grails.rest.render.RendererRegistry
import grails.rest.RestfulController

import org.grails.plugins.web.rest.render.ServletRenderContext

import static org.springframework.http.HttpStatus.OK

/**
 * Produce an ATOM feed of all Models. Only respond in ATOM format. To render the response in ATOM,
 * a bean needs to be configured in grails-app/conf/spring/resources.groovy as follows:
 * import uk.ac.ox.softeng.maurodatamapper.core.model.Model
 * import uk.ac.ox.softeng.maurodatamapper.core.rest.render.MdmAtomModelCollectionRenderer
 * beans = {
 *   halModelListRenderer(MdmAtomModelCollectionRenderer, Model) {
 *     includes = []
 *   }
 * }
 * 
 * @since 04/01/2021
 */
class FeedController extends RestfulController<Model> implements ResourcelessMdmController {

    @Autowired(required = false)
    RendererRegistry rendererRegistry

    static responseFormats = ['atom']

    FeedService feedService

    FeedController() {
        super(Model)
    }

    def index() {
        params.format = 'atom'
        
        /**
         * If we do respond(feedService.findModels(currentUserSecurityPolicyManager)), this works fine except
         * when the List returned by findModels is empty. In this case, grails RestResponder sets a 406 response
         * status, rather than returning an Atom feed with no entries. This appears to be because insider RestResponder,
         * a call like renderer = registry.findContainerRenderer(mimeType, valueType, value) returns null.
         *
         * So below we essentially hardcode the renderer lookup for mime type atom, with an ArrayList of Model.
         * The lines of code below copied with minor modifications from within
         * https://github.com/grails/grails-core/blob/master/grails-plugin-rest/src/main/groovy/grails/artefact/controller/RestResponder.groovy
         */
        Renderer renderer = null
        renderer = rendererRegistry.findContainerRenderer(grails.web.mime.MimeType.ATOM_XML,
                                                          ArrayList,
                                                          uk.ac.ox.softeng.maurodatamapper.core.model.Model)
        
        final webRequest = ((Controller)this).getWebRequest()
        final context = new ServletRenderContext(webRequest, [:])
        context.setStatus(OK)
        renderer.render(feedService.findModels(currentUserSecurityPolicyManager), context)
        if(context.wasWrittenTo() && !response.isCommitted()) {
            response.flushBuffer()
        }
    
    }

}
