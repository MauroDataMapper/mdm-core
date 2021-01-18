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

import grails.converters.XML
import grails.rest.Link
import grails.rest.render.atom.AtomRenderer
import grails.rest.render.RenderContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.web.xml.XMLStreamWriter

import groovy.util.logging.Slf4j

import java.time.OffsetDateTime

/**
 * Extend AtomRenderer, dealing with OffsetDateTimes, adding a summary tag and overriding writeLink in order to deal
 * with links in a slightly different way to the base class.
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

    /**
     * Override the base method in HalXmlRenderer, only writing the 'self' link and removing the contentType
     * attribute from this link. (The base method writes something like
     *
     *  <link rel="self" href="http://localhost:8080/api/terminologies/3472b192-ac49-4495-85cd-f00db153e595" hreflang="c.u" type="application/atom+xml" />
     *  <link rel="alternate" href="http://localhost:8080/api/terminologies/3472b192-ac49-4495-85cd-f00db153e595" hreflang="c.u" />
     *
     * but a type of application/atom+xml on the self link is not correct)
     *
     * Copied from https://github.com/grails/grails-core/blob/master/grails-plugin-rest/src/main/groovy/grails/rest/render/hal/HalXmlRenderer.groovy
     * with minor changes.
     */
    @Override
    void writeLink(Link link, Locale locale, writerObject) {
        if (link.rel == RELATIONSHIP_SELF) {
            XMLStreamWriter writer = ((XML) writerObject).getWriter()
            writer.startNode(LINK_TAG)
                .attribute(RELATIONSHIP_ATTRIBUTE, link.rel)
                .attribute(HREF_ATTRIBUTE, link.href)
                .attribute(HREFLANG_ATTRIBUTE, (link.hreflang ?: locale).language)

            final title = link.title
            if (title) {
                writer.attribute(TITLE_ATTRIBUTE, title)
            }

            if (link.templated) {
                writer.attribute(TEMPLATED_ATTRIBUTE,"true")
            }
            if (link.deprecated) {
                writer.attribute(DEPRECATED_ATTRIBUTE,"true")
            }
            writer.end()
        }
    }

}
