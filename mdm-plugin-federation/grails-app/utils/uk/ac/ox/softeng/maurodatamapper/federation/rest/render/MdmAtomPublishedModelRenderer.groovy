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

import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyService
import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.authority.AuthorityService
import uk.ac.ox.softeng.maurodatamapper.federation.PublishedModel

import grails.converters.XML
import grails.rest.Link
import grails.rest.render.RenderContext
import grails.rest.render.atom.AtomRenderer
import groovy.util.logging.Slf4j
import org.grails.web.xml.StreamingMarkupWriter
import org.grails.web.xml.XMLStreamWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Extend AtomRenderer, dealing with OffsetDateTimes, adding a summary tag and overriding various methods in order
 * to get closer to an output which passes W3C atom validation at https://validator.w3.org/feed/
 * @since 04/01/2021
 * @see grails.rest.render.atom.AtomRenderer
 */
@Slf4j
class MdmAtomPublishedModelRenderer<T> extends AtomRenderer<T> {

    public static final LocalDate MINTED_DATE = LocalDate.of(2021, 1, 27)
    public static final String AUTHOR_TAG = 'author'
    public static final String AUTHOR_NAME_TAG = 'name'
    public static final String AUTHOR_URI_TAG = 'uri'
    public static final String CATEGORY_TAG = 'category'
    public static final String CATEGORY_TERM_ATTRIBUTE = 'term'
    public static final String SUMMARY_TAG = 'summary'

    @Autowired
    ApiPropertyService apiPropertyService

    @Autowired
    AuthorityService authorityService

    MdmAtomPublishedModelRenderer(Class<T> targetType) {
        super(targetType)
        //  excludes << 'path'
        //  excludes << 'breadcrumbTree'
    }

    /**
     *
     * Override the AtomRenderer.renderInternal so that we can set the updated tag for the feed.
     * Copied from {@link grails.rest.render.atom.AtomRenderer#renderInternal(java.lang.Object, grails.rest.render.RenderContext)}
     * but with the addition for setting the updated tag
     */
    @Override
    void renderInternal(T object, RenderContext context) {
        final streamingWriter = new StreamingMarkupWriter(context.writer, encoding)
        XML xml = new XML(new XMLStreamWriter(streamingWriter))

        Authority authority = authorityService.defaultAuthority

        XMLStreamWriter writer = xml.getWriter()
        writer.startDocument(encoding, '1.0')

        if (object instanceof PublishedModel) {
            writePublishedModelWithLinks(object, xml)
        } else if (object instanceof Collection) {
            final locale = context.locale
            String resourceHref = linkGenerator.link(uri: context.resourcePath, method: HttpMethod.GET, absolute: true)
            final title = getResourceTitle(context.resourcePath, locale)
            writer
                .startNode(FEED_TAG)
                .attribute(XMLNS_ATTRIBUTE, ATOM_NAMESPACE)
                .startNode(TITLE_ATTRIBUTE)
                .characters(title)
                .end()
                .startNode(ID_TAG)
                .characters(generateIdForURI(resourceHref, getMintedDate()))
                .end()

            // Render in the authority as an author for all models
            writer.startNode(AUTHOR_TAG)
                .startNode(AUTHOR_NAME_TAG)
                .characters(authority.label)
                .end()
                .startNode(AUTHOR_URI_TAG)
                .characters(authority.url)
                .end()
                .end()

            /*
            Set the updated of the feed to be the most recent updated of elements in the collection
            This is missing from AtomRenderer and is required by Atom standards
            https://validator.w3.org/feed/docs/atom.html#requiredEntryElements
            */
            if (object.size() > 0) {
                def mostRecentlyUpdated = object.max { it.lastUpdated }
                writer.startNode(UPDATED_TAG)
                    .characters(formatLastUpdated(mostRecentlyUpdated))
                    .end()
            } else {
                writer.startNode(UPDATED_TAG)
                    .characters(formatAtomDate(OffsetDateTime.now()))
                    .end()
            }

            def linkSelf = new Link(RELATIONSHIP_SELF, resourceHref)
            linkSelf.title = title
            linkSelf.contentType = mimeTypes[0].name
            writeLink(linkSelf, null, xml)

            for (o in ((Collection) object)) {
                if (o !instanceof PublishedModel) {
                    throw new IllegalArgumentException(
                        "Cannot render object [$o] using Atom. The AtomRenderer can only be used with domain classes that specify 'dateCreated' and " +
                        "'lastUpdated' properties")
                }
                writePublishedModelWithLinks(o, xml, false)
            }
            writer.end()
            context.writer.flush()
        } else {
            throw new IllegalArgumentException(
                "Cannot render object [$object] using Atom. The AtomRenderer can only be used with domain classes that specify 'dateCreated' and " +
                "'lastUpdated' properties")
        }

    }

    /**
     * Override the base method in HalXmlRenderer, removing the contentType
     * attribute from this link. (The base method writes something like
     *
     *  <link rel="self" href="http://localhost:8080/api/terminologies/3472b192-ac49-4495-85cd-f00db153e595" hreflang="c.u"
     *  type="application/atom+xml" />
     *  <link rel="alternate" href="http://localhost:8080/api/terminologies/3472b192-ac49-4495-85cd-f00db153e595" hreflang="c.u" />
     *
     * but a type of application/atom+xml on the self link is not correct)
     *
     * Copied from {@link grails.rest.render.hal.HalXmlRenderer#writeLink(grails.rest.Link, java.util.Locale, java.lang.Object)}
     * with minor changes.
     */
    @Override
    void writeLink(Link link, Locale locale,writerObject) {
        XMLStreamWriter writer = ((XML) writerObject).getWriter()
        writer.startNode(LINK_TAG)

        if (link.rel) {
            writer.attribute(RELATIONSHIP_ATTRIBUTE, link.rel)
        }

        if (link.contentType) {
            writer.attribute(TYPE_ATTRIBUTE, link.contentType)
        }

        writer.attribute(HREF_ATTRIBUTE, link.href)

        if (link.title) {
            writer.attribute(TITLE_ATTRIBUTE, link.title)
        }

        Locale linkLocale = link.hreflang ?: locale
        if (linkLocale) {
            writer.attribute(HREFLANG_ATTRIBUTE, linkLocale.language)
        }

        if (link.templated) {
            writer.attribute(TEMPLATED_ATTRIBUTE, 'true')
        }
        if (link.deprecated) {
            writer.attribute(DEPRECATED_ATTRIBUTE, 'true')
        }
        writer.end()
    }

    /**
     * Overload the base method, which requires a Date rather than OffsetDateTime, and format a date string
     * which passes W3C validation
     */
    protected String formatAtomDate(OffsetDateTime timeToFormat) {
        formatAtomDate(Date.from(timeToFormat.toInstant()))
    }

    /**
     * The date MUST include the offset and the AtomRender format does not.
     *
     * @param dateCreated
     * @return string of formatted date
     */
    @Override
    protected String formatAtomDate(Date dateCreated) {
        getMdmAtomDateFormat().format(dateCreated)
    }

    /**
     * Overload the base method, which requires a Date rather than OffsetDateTime
     *
     */
    @SuppressWarnings('unused')
    String generateIdForURI(String url, OffsetDateTime dateCreated, UUID id) {
        generateIdForURI(url, Date.from(dateCreated.toInstant()), id)
    }

    /**
     * The ID tag must include a 'minted date'
     *
     * @param url
     * @return String ID including the minted date
     */
    @Override
    String generateIdForURI(String url) {
        generateIdForURI(url, getMintedDate())
    }

    /**
     * Override to provide better handling for actual URLs. We can convert them to a URL and then extract the parts we want in the tag
     * rather than using substring cleaning
     * @param url
     * @param dateCreated
     * @param id
     * @return
     */
    @Override
    String generateIdForURI(String url, Date dateCreated, Object id = null) {
        try {
            URL actualUrl = url.toURL()
            return "tag:${actualUrl.host},${ID_DATE_FORMAT.format(dateCreated)}:${id ?: actualUrl.path}"
        } catch (MalformedURLException ignored) {
            super.generateIdForURI(url, dateCreated, id)
        }
    }

    void writePublishedModelWithLinks(PublishedModel publishedModel, XML xml, boolean isFirst = true) {
        XMLStreamWriter writer = xml.getWriter()
        writer.startNode(isFirst ? FEED_TAG : ENTRY_TAG)
        if (isFirst) {
            writer.attribute(XMLNS_ATTRIBUTE, ATOM_NAMESPACE)
        }
        /*
         Required Fields
         */
        writer.startNode(ID_TAG)
            .characters(getPublishedModelUrn(publishedModel))
            .end()

        //Use model label as the title
        writer.startNode(TITLE_ATTRIBUTE)
            .characters(publishedModel.modelLabel + " " + publishedModel.modelVersion.toString())
            .end()

        writer.startNode(UPDATED_TAG)
            .characters(formatLastUpdated(publishedModel))
            .end()

        /*
        Recommended fields
         */
        if (publishedModel.author) {
            writer.startNode(AUTHOR_TAG)
                .startNode(AUTHOR_NAME_TAG)
                .characters(publishedModel.author)
                .end()
                .end()
        }
        if (publishedModel.description) {
            writer.startNode(SUMMARY_TAG)
                .characters(publishedModel.description)
                .end()
        }

        /*
        Optional fields
         */
        writer.startNode(PUBLISHED_TAG)
            .characters(formatAtomDate(publishedModel.datePublished))
            .end()

        writer.startNode(CATEGORY_TAG)
            .attribute(CATEGORY_TERM_ATTRIBUTE, publishedModel.modelType)
            .end()

        publishedModel.links.each {writeLink(it, null, xml)}

        writer.end()
    }

    /**
     * Generate the value of an ID tag that looks like urn:uuid:{publishedModel.modelId}* @param model
     * @return A string like urn:uuid:{publishedModel.modelId}
     */
    String getPublishedModelUrn(PublishedModel publishedModel) {
        "urn:uuid:${publishedModel.modelId}"
    }

    Date getMintedDate() {
        Date.from(MINTED_DATE.atStartOfDay().toInstant(ZoneOffset.UTC))
    }

    //Make our own Atom date format because AtomRenderer.ATOM_DATE_FORMAT does not pass validation
    static SimpleDateFormat getMdmAtomDateFormat() {
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    }
}
