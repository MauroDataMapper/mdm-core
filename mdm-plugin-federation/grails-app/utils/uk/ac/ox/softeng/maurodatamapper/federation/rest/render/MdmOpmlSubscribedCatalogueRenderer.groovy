/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.federation.rest.render

import uk.ac.ox.softeng.maurodatamapper.federation.SubscribedCatalogue

import grails.converters.XML
import grails.rest.render.RenderContext
import grails.rest.render.hal.HalXmlRenderer
import grails.web.mime.MimeType
import groovy.util.logging.Slf4j
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.web.xml.PrettyPrintXMLStreamWriter
import org.grails.web.xml.StreamingMarkupWriter
import org.grails.web.xml.XMLStreamWriter

/**
 * Extend HalXmlRenderer, with specifics for an OPML feed of SubscribedCatalogue
 * @since 02/02/2021
 */
@Slf4j
class MdmOpmlSubscribedCatalogueRenderer<T> extends HalXmlRenderer<T> {


    public static final String HEAD_TAG = 'head'
    public static final String TITLE_TAG = 'title'
    public static final String BODY_TAG = 'body'
    public static final String OUTLINE_TAG = 'outline'
    public static final String OPML_TAG = 'opml'
    public static final String VERSION_ATTRIBUTE = 'version'
    public static final String VERSION = '2.0'
    public static final String TITLE = "Subscribed Catalogues"

    MdmOpmlSubscribedCatalogueRenderer(Class<T> targetType) {
        super(targetType, new MimeType("text/x-opml", "opml"))
    }

    /**
     *
     * Override the HalXmlRenderer.renderInternal so that we can set the updated tag for the feed.
     * Copied from AtomRenderer but with the addition for setting the updated tag
     */
    @Override
    void renderInternal(T object, RenderContext context) {
        final streamingWriter = new StreamingMarkupWriter(context.writer, encoding)
        XMLStreamWriter w = prettyPrint ? new PrettyPrintXMLStreamWriter(streamingWriter) : new XMLStreamWriter(streamingWriter)
        XML xml = new XML(w)

        final entity = mappingContext.getPersistentEntity(object.class.name)
        boolean isDomain = entity != null

        Set writtenObjects = []
        w.startDocument(encoding, "1.0")

        if (isDomain) {
            writeSubscribedCatalogue(entity, object, context, xml, writtenObjects)
        } else if (object instanceof Collection) {
            XMLStreamWriter writer = xml.getWriter()
            writer
            .startNode(OPML_TAG)
            .attribute(VERSION_ATTRIBUTE, VERSION)
            .startNode(HEAD_TAG)
            .startNode(TITLE_TAG)
            .characters(TITLE)
            .end()
            .end()
            .startNode(BODY_TAG)
            .startNode(OUTLINE_TAG)
            .attribute("text", "Subscriptions")
            .attribute("title", "Subscriptions")
            .attribute("type", "Catalogues")

            for (o in ((Collection) object)) {
                final currentEntity = mappingContext.getPersistentEntity(o.class.name)
                if (currentEntity) {
                    writeSubscribedCatalogue(currentEntity, o, context, xml, writtenObjects)
                }
            }

            writer
            .end() //close the <outline>
            .end() //close the <body>
            .end() //close the <opml>
            context.writer.flush()
        }
    }

    /**
     * Write a SubscribedCatalogue to an <outline> tag
     *
     */
    void writeSubscribedCatalogue(PersistentEntity entity, SubscribedCatalogue subscribedCatalogue, RenderContext context, XML xml, Set writtenObjects) {
        XMLStreamWriter writer = xml.getWriter()

        //Write a single outline tag for a single SubscribedCatalogue
        writer.startNode(OUTLINE_TAG)
              .attribute("text", subscribedCatalogue.url)
              .attribute("xmlUrl", subscribedCatalogue.url)
              .attribute("type", "Catalogue")
              .end()

    }
}
