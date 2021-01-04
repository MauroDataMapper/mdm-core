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

import grails.rest.render.atom.AtomRenderer
import grails.rest.render.RenderContext
import org.grails.datastore.mapping.model.PersistentEntity

import groovy.util.logging.Slf4j

import java.time.OffsetDateTime

/**
 * Extend AtomRenderer, dealing with OffsetDateTimes and adding a summary tag
 * @since 04/01/2021
 * @see https://github.com/grails/grails-core/blob/master/grails-plugin-rest/src/main/groovy/grails/rest/render/atom/AtomRenderer.groovy
 * for inspiration. 
 */
@Slf4j
class MdmAtomModelRenderer<T> extends AtomRenderer<T> {

    MdmAtomModelRenderer(Class<T> targetType) {
        super(targetType)
    }

    /**
     * Overload the base method, which requires a Date rather than OffsetDateTime
     *
     */
    String formatAtomDate(OffsetDateTime timeToFormat) {
        formatAtomDate(new Date(timeToFormat.toEpochSecond() * 1000))
    }

    /**
     * Overload the base method, which requires a Date rather than OffsetDateTime
     *
     */
    String generateIdForURI(String url, OffsetDateTime dateCreated, UUID id) {
        generateIdForURI(url, new Date(dateCreated.toEpochSecond() * 1000), id)
    }

    /**
     * Override the base method, adding a 'summary' tag which is populated with the description property
     *
     */
    @Override
    protected void writeDomain(RenderContext context, MetaClass metaClass, PersistentEntity entity, Object model, writer) {

        super.writeDomain(context, metaClass, entity, model, writer)

        if (model.description) {
            writeDomainProperty(model.description, 'summary', writer)
        }
    }

}
