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
package uk.ac.ox.softeng.maurodatamapper.core.rest.render

import org.grails.datastore.mapping.model.types.ToOne
import org.grails.web.xml.PrettyPrintXMLStreamWriter
import org.grails.web.xml.StreamingMarkupWriter
import org.springframework.http.HttpMethod
import uk.ac.ox.softeng.maurodatamapper.core.model.Model

import grails.converters.XML
import grails.rest.Link
import grails.rest.render.RenderContext
import grails.rest.render.atom.AtomRenderer
import groovy.util.logging.Slf4j
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.web.xml.XMLStreamWriter

import groovy.util.logging.Slf4j

import java.text.SimpleDateFormat
import java.time.OffsetDateTime

/**
 * Extend AtomRenderer, dealing with OffsetDateTimes, adding a summary tag and overriding various methods in order
 * to get closer to an output which passes W3C atom validation at https://validator.w3.org/feed/
 * @since 04/01/2021
 * @see https://github.com/grails/grails-core/blob/master/grails-plugin-rest/src/main/groovy/grails/rest/render/atom/AtomRenderer.groovy
 * for inspiration. 
 */
@Slf4j
class MdmAtomModelRenderer<T> extends AtomRenderer<T> {

    //Make our own Atom date format because AtomRenderer.ATOM_DATE_FORMAT does not pass validation
    public static SimpleDateFormat MDM_ATOM_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")

    public static final Date MINTED_DATE =  new Date(121, 0, 27)
    public static final String AUTHOR_TAG = 'author'
    public static final String AUTHOR_NAME_TAG = 'name'
    public static final String CATEGORY_TAG = 'category'
    public static final String CATEGORY_TERM_ATTRIBUTE = 'term'
    public static final String SUMMARY_TAG = 'summary'

    MdmAtomModelRenderer(Class<T> targetType) {
        super(targetType)

        //In AbstractLinkingRenderer we have boolean prettyPrint = Environment.isDevelopmentMode()
        //But prettyPrint adds whitespace which causes the rendered XML to fail W3C validation. This makes
        //testing development difficult, so here we turn prettyPrint off.
        prettyPrint = false
    }

    /**
     *
     * Override the AtomRenderer.renderInternal so that we can set the updated tag for the feed.
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
            writeDomainWithEmbeddedAndLinks(entity, object, context, xml, writtenObjects)
        } else if (object instanceof Collection) {
            final locale = context.locale
            String resourceHref = linkGenerator.link(uri: context.resourcePath, method: HttpMethod.GET, absolute:true)
            final title = getResourceTitle(context.resourcePath, locale)
            XMLStreamWriter writer = xml.getWriter()
            writer
                    .startNode(FEED_TAG)
                    .attribute(XMLNS_ATTRIBUTE, ATOM_NAMESPACE)
                    .startNode(TITLE_ATTRIBUTE)
                    .characters(title)
                    .end()
                    .startNode(ID_TAG)
                    .characters(generateIdForURI(resourceHref))
                    .end()

            def linkSelf = new Link(RELATIONSHIP_SELF, resourceHref)
            linkSelf.title = title
            linkSelf.contentType=mimeTypes[0].name
            linkSelf.hreflang = locale
            writeLink(linkSelf, locale, xml)
            def linkAlt = new Link(RELATIONSHIP_ALTERNATE, resourceHref)
            linkAlt.title = title
            linkAlt.hreflang = locale
            writeLink(linkAlt, locale, xml)

            //Set the updated of the feed to be the most recent updated of elements in the collection
            if (object.size() > 0) {
                Model mostRecentlyUpdated = object[0]
                object.each {
                    if (it.lastUpdated > mostRecentlyUpdated.lastUpdated) {
                        mostRecentlyUpdated = it
                    }
                }
                writer.startNode(UPDATED_TAG)
                      .characters(formatLastUpdated(mostRecentlyUpdated))
                      .end()

            }

            for (o in ((Collection) object)) {
                final currentEntity = mappingContext.getPersistentEntity(o.class.name)
                if (currentEntity) {
                    writeDomainWithEmbeddedAndLinks(currentEntity, o, context, xml, writtenObjects, false)
                } else {
                    throw new IllegalArgumentException("Cannot render object [$o] using Atom. The AtomRenderer can only be used with domain classes that specify 'dateCreated' and 'lastUpdated' properties")
                }
            }
            writer.end()
            context.writer.flush()
        } else {
            throw new IllegalArgumentException("Cannot render object [$object] using Atom. The AtomRenderer can only be used with domain classes that specify 'dateCreated' and 'lastUpdated' properties")
        }

    }

    /**
     * Overload the base method, which requires a Date rather than OffsetDateTime, and format a date string
     * which passes W3C validation
     */
    String formatAtomDate(OffsetDateTime timeToFormat) {
        Date dateToFormat = new Date(timeToFormat.toEpochSecond() * 1000)
        return MDM_ATOM_DATE_FORMAT.format(dateToFormat)
    }

    /**
     * Overload the base method, which requires a Date rather than OffsetDateTime
     *
     */
    String generateIdForURI(String url, OffsetDateTime dateCreated, UUID id) {
        generateIdForURI(url, new Date(dateCreated.toEpochSecond() * 1000), id)
    }

    /**
     * Override the base method in HalXmlRenderer, removing the contentType
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

    /**
     * Override the base method so that we have more flexibility to use the properties we want
     *
     */
    @Override
    void writeDomainWithEmbeddedAndLinks(PersistentEntity entity, Object object, RenderContext context, XML xml, Set writtenObjects, boolean isFirst = true) {
        writeModelWithEmbeddedAndLinks(entity, object, context, xml, writtenObjects, isFirst)
    }

    /**
     * Write a model. Copied from AtomRenderer#writeDomainWithEmbeddedAndLinks with minor tweaks.
     *
     */
    void writeModelWithEmbeddedAndLinks(PersistentEntity entity, Model object, RenderContext context, XML xml, Set writtenObjects, boolean isFirst = true) {
        if (!entity.getPropertyByName('lastUpdated')) {
            throw new IllegalArgumentException("Cannot render object [$object] using Atom. The AtomRenderer can only be used with domain classes that specify 'dateCreated' and 'lastUpdated' properties")
        }
        final locale = context.locale
        String resourceHref = linkGenerator.link(resource: object, method: HttpMethod.GET, absolute:true)
        final title = getLinkTitle(entity, locale)
        XMLStreamWriter writer = xml.getWriter()
        writer.startNode(isFirst ? FEED_TAG : ENTRY_TAG)
        if (isFirst) {
            writer.attribute(XMLNS_ATTRIBUTE, ATOM_NAMESPACE)
        }

        //Use model label as the title
        writer.startNode(TITLE_ATTRIBUTE)
              .characters(object.label + " " + object.modelVersion.toString())
              .end()

        final dateCreated = formatDateCreated(object)
        if (dateCreated) {
            writer.startNode(PUBLISHED_TAG)
                    .characters(dateCreated)
                    .end()
        }
        final lastUpdated = formatLastUpdated(object)
        if (lastUpdated) {
            writer.startNode(UPDATED_TAG)
                    .characters(lastUpdated)
                    .end()
        }
        writer.startNode(ID_TAG)
                .characters(getModelUrn(object))
                .end()

        writer.startNode(SUMMARY_TAG)
                .characters(object.description ?: object.label)
                .end()

        writer.startNode(CATEGORY_TAG)
                .attribute(CATEGORY_TERM_ATTRIBUTE, object.class.simpleName)
                .end()

        if (object.author) {
            writer.startNode(AUTHOR_TAG)
                    .startNode(AUTHOR_NAME_TAG)
                    .characters(object.author)
                    .end()
                    .end()
        }

        def linkSelf = new Link(RELATIONSHIP_SELF, resourceHref)
        linkSelf.title = title
        linkSelf.contentType=mimeTypes[0].name
        linkSelf.hreflang = locale
        writeLink(linkSelf, locale, xml)
        def linkAlt = new Link(RELATIONSHIP_ALTERNATE, resourceHref)
        linkAlt.title = title
        linkAlt.hreflang = locale

        writeLink(linkAlt,locale, xml)
        final metaClass = GroovySystem.metaClassRegistry.getMetaClass(entity.javaClass)
        final associationMap = writeAssociationLinks(context,object, locale, xml, entity, metaClass)
        writeDomain(context, metaClass, entity, object, xml)

        if (associationMap) {
            for (entry in associationMap.entrySet()) {
                final property = entry.key
                final isSingleEnded = property instanceof ToOne
                if (isSingleEnded) {
                    Object value = entry.value
                    if (writtenObjects.contains(value)) {
                        continue
                    }

                    if (value != null) {
                        final associatedEntity = property.associatedEntity
                        if (associatedEntity) {
                            writtenObjects << value
                            writeDomainWithEmbeddedAndLinks(associatedEntity, value, context, xml, writtenObjects, false)
                        }
                    }
                } else {
                    final associatedEntity = property.associatedEntity
                    if (associatedEntity) {
                        writer.startNode(property.name)
                        for (obj in entry.value) {
                            writtenObjects << obj
                            writeDomainWithEmbeddedAndLinks(associatedEntity, obj, context, xml, writtenObjects, false)
                        }
                        writer.end()
                    }
                }

            }
        }
        writer.end()
    }

    /**
     * The ID tag must include a 'minted date'
     * 
     * @param url
     * @return String ID including the minted date
     */
    String generateIdForURI(String url) {
        generateIdForURI(url, MINTED_DATE)
    }

    /**
     * Generate the value of an ID tag that looks like urn:uuid:{model.id}
     * @param model
     * @return A string like urn:uuid:{model.id}
     */
    String getModelUrn(Model model) {
        return "urn:uuid:${model.id}"
    }

}
