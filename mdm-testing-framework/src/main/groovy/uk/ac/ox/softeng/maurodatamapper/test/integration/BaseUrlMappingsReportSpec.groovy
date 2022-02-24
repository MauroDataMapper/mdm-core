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
package uk.ac.ox.softeng.maurodatamapper.test.integration

import uk.ac.ox.softeng.maurodatamapper.test.MdmSpecification

import com.google.common.base.CaseFormat
import grails.gorm.validation.ConstrainedProperty
import grails.util.BuildSettings
import grails.web.mapping.UrlMapping
import grails.web.mapping.UrlMappingsHolder
import groovy.util.logging.Slf4j
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.client.DefaultHttpClientConfiguration
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.http.client.netty.DefaultHttpClient
import org.grails.web.mapping.ResponseCodeMappingData
import org.grails.web.mapping.ResponseCodeUrlMapping
import org.junit.Assert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.MessageSource
import spock.lang.Shared

import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration

import static org.grails.web.mapping.reporting.AnsiConsoleUrlMappingsRenderer.DEFAULT_ACTION

/**
 * Each plugin that defines a controller should implement this test.
 * It will highlight any endpoints which are added or removed and also any which have not been added to the online postman api documentation
 *
 * <pre>
 *     import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseUrlMappingsReportSpec
 *     import grails.testing.mixin.integration.Integration
 *     import groovy.util.logging.Slf4j
 *
 * @Integration
 *     @Slf4j
 *     class UrlMappingsSpec extends BaseUrlMappingsReportSpec {*
 * @Override
 *          String getKnownControllerInPlugin() {*              '????'
 *}*}* </pre>
 *
 * @since 14/02/2022
 */
@Slf4j
abstract class BaseUrlMappingsReportSpec extends MdmSpecification {

    /**
     * The UrlMappings version of a controller name for a controller which is defined in this plugin.
     * This allows the tests to limit to only url mappings defined by the plugin being tested
     * @return
     */
    abstract String getKnownControllerInPlugin()

    Map<String, List<String>> allNotDocumentedLines
    List<DocumentedUrl> foundDocumentedUrls
    List<String> allLines
    Map<String, List<String>> allNotTrackedLines
    List<String> notTrackedLines
    List<DocumentedUrl> documentedUrls

    @Shared
    Path resourcesPath

    @Shared
    Path existingEndpoints

    @Autowired
    MessageSource messageSource

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    UrlMappingsHolder urlMappingsHolder

    def setupSpec() {
        resourcesPath = Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources', 'url-mappings')
        if (!Files.exists(resourcesPath)) {
            Files.createDirectories(resourcesPath)
        }
        existingEndpoints = resourcesPath.resolve('tracked_endpoints.txt')
    }

    def setup() {
        allNotDocumentedLines = [:]
        foundDocumentedUrls = []
        notTrackedLines = []
        allLines = []
        allNotTrackedLines = [:]
    }

    void 'TRACK : test url mappings reporting'() {
        given:
        // Get the endpoints which are currently tracked
        allLines = Files.exists(existingEndpoints) ? Files.readAllLines(existingEndpoints) : []

        when:
        boolean allTracked = verifyAllUrlMappings(CheckingType.TRACKING, urlMappingsHolder.getUrlMappings().toList())

        then:
        Assert.assertTrue('All URLs are correctly tracked. See logs and/or src/integration-test/resources/url-mappings for new/old endpoints', allTracked)
    }

    void 'API : test url mappings are in postman documentation'() {

        given:
        documentedUrls = getApiDocumentedUrls()

        if (!documentedUrls) {
            log.warn('No Api Documentation Available')
            return
        }

        when:
        boolean allDocumented = verifyAllUrlMappings(CheckingType.DOCUMENTED, urlMappingsHolder.getUrlMappings().toList())

        then:
        Assert.assertTrue('All URLs are correctly documented at https://documenter.getpostman.com/view/9840589/UVC8BkkA. See logs and/or src/integration-test/resources/url-mappings for undocumented endpoints', allDocumented)
    }

    private boolean verifyAllUrlMappings(CheckingType checkingType, List<UrlMapping> urlMappings) {
        // Get the plugin index for a known controller endpoint from this specific plugin
        // This will allow us to extract on the urlmappings that are added by this plugin
        int pluginIndex = urlMappings.find {it.controllerName == getKnownControllerInPlugin()}.pluginIndex

        // Extract all the new mappings and group by the controller name, only get the ones defined by a controller
        final mappingsByController = urlMappings
            .findAll {it.pluginIndex == pluginIndex && it.controllerName}
            .groupBy {it.controllerName.toString()}

        // Extract the controller names and sort for easier display
        final controllerNames = mappingsByController.keySet().sort()

        // Iterate through all the endpoints and check if theyre tracked
        for (controller in controllerNames) {
            final controllerUrlMappings = mappingsByController[controller]


            for (UrlMapping urlMapping in controllerUrlMappings) {
                String urlPattern = establishUrlPattern(urlMapping, checkingType == CheckingType.DOCUMENTED)

                String actionName = urlMapping.actionName?.toString() ?: DEFAULT_ACTION

                String line = null

                if (actionName && !urlMapping.viewName) {
                    line = "| ${urlMapping.httpMethod.padRight(6)} | ${urlPattern} | ${actionName} |"
                } else if (urlMapping.viewName) {
                    line = "| ${urlMapping.httpMethod.padRight(6)} | ${urlPattern} | ${urlMapping.viewName} |"
                }

                if (line) {
                    checkLine(checkingType, line, urlMapping, urlPattern)
                }
            }

            if (notTrackedLines) {
                switch (checkingType) {
                    case CheckingType.TRACKING:
                        allNotTrackedLines[controller] = notTrackedLines
                        break
                    case CheckingType.DOCUMENTED:
                        allNotDocumentedLines[controller] = notTrackedLines
                        break
                }
                notTrackedLines = []
            }
        }

        outputEndpointResultsAndVerify(checkingType)
    }

    private void checkLine(CheckingType checkingType, String line, UrlMapping urlMapping, String urlPattern) {
        switch (checkingType) {
            case CheckingType.TRACKING:
                String alreadyTracked = allLines.find {it == line}
                if (alreadyTracked) {
                    allLines.remove(alreadyTracked)
                } else {
                    notTrackedLines << line
                }
                break
            case CheckingType.DOCUMENTED:
                DocumentedUrl documentedUrl = documentedUrls.find {
                    (urlMapping.httpMethod == '*' || it.method == urlMapping.httpMethod) &&
                    urlsMatch(it.url, urlPattern)
                }
                if (documentedUrl) {
                    log.debug('[{}] documented as [{}]', urlPattern, documentedUrl.url)
                    foundDocumentedUrls << documentedUrl
                } else {
                    notTrackedLines << line
                }
                break
        }

    }

    private boolean outputEndpointResultsAndVerify(CheckingType checkingType) {
        switch (checkingType) {
            case CheckingType.TRACKING:
                outputTrackedEndpointResults()
                return !allLines && !allNotTrackedLines
            case CheckingType.DOCUMENTED:
                outputDocumentedEndpointResults()
                return !allNotDocumentedLines
        }
    }

    private void outputDocumentedEndpointResults() {
        // Organise results for logging and files
        StringBuilder sb

        if (allNotDocumentedLines) {
            sb = new StringBuilder('>>>>> URL Mappings which are not documented <<<<<<\n\n')
            allNotDocumentedLines.each {k, v ->
                sb.append('Controller: ').append(k).append('\n')
                v.each {
                    sb.append(it).append('\n')
                }
                sb.append('\n')
            }
            Path newEndpoints = resourcesPath.resolve('undocumented_endpoints.txt')
            Files.write(newEndpoints, sb.toString().getBytes(Charset.defaultCharset()))
            log.error('{}', sb)
        }
    }

    private void outputTrackedEndpointResults() {
        // Organise results for logging and files
        StringBuilder sb
        StringBuilder fileSb
        if (allLines) {
            sb = new StringBuilder('>>>>> URL Mappings which no longer exist <<<<<<\n')
            fileSb = new StringBuilder()
            allLines.each {
                fileSb.append(it).append('\n')
                sb.append(it).append('\n')
            }
            Path oldEndpoints = resourcesPath.resolve('old_endpoints.txt')
            Files.write(oldEndpoints, fileSb.toString().getBytes(Charset.defaultCharset()))
            log.error('{}', sb)
        }
        if (allNotTrackedLines) {
            sb = new StringBuilder('>>>>> URL Mappings which are not tracked <<<<<<\n')
            fileSb = new StringBuilder()
            allNotTrackedLines.each {k, v ->
                sb.append('Controller: ').append(k).append('\n')
                v.each {
                    fileSb.append(it).append('\n')
                    sb.append(it).append('\n')
                }
                sb.append('\n')
            }
            Path newEndpoints = resourcesPath.resolve('new_endpoints.txt')
            Files.write(newEndpoints, fileSb.toString().getBytes(Charset.defaultCharset()))
            log.error('{}', sb)
        }
    }

    private String establishUrlPattern(UrlMapping urlMapping, boolean apiDocumentation) {
        if (urlMapping instanceof ResponseCodeUrlMapping) {
            def errorCode = "ERROR: " + ((ResponseCodeMappingData) urlMapping.urlData).responseCode
            return errorCode
        }
        final constraints = urlMapping.constraints
        final tokens = urlMapping.urlData.tokens
        StringBuilder urlPattern = new StringBuilder(UrlMapping.SLASH)
        int constraintIndex = 0
        tokens.eachWithIndex {String token, int i ->
            boolean hasTokens = token.contains(UrlMapping.CAPTURED_WILDCARD) || token.contains(UrlMapping.CAPTURED_DOUBLE_WILDCARD)
            if (hasTokens) {
                String finalToken = token
                while (hasTokens) {
                    if (finalToken.contains(UrlMapping.CAPTURED_WILDCARD)) {
                        ConstrainedProperty constraint = (ConstrainedProperty) constraints[constraintIndex++]
                        def prop = apiDocumentation ? "{{${CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, constraint.propertyName)}}}" :
                                   '\\${' + constraint.propertyName + '}'
                        finalToken = finalToken.replaceFirst(/\(\*\)/, prop)
                    } else if (finalToken.contains(UrlMapping.CAPTURED_DOUBLE_WILDCARD)) {
                        ConstrainedProperty constraint = (ConstrainedProperty) constraints[constraintIndex++]
                        def prop = '\\\${' + constraint.propertyName + '}**'
                        finalToken = finalToken.replaceFirst(/\(\*\*\)/, prop)
                    }
                    hasTokens = finalToken.contains(UrlMapping.CAPTURED_WILDCARD) || finalToken.contains(UrlMapping.CAPTURED_DOUBLE_WILDCARD)
                }
                urlPattern << finalToken
            } else {
                urlPattern << token
            }

            if (i < (tokens.length - 1)) {
                urlPattern << UrlMapping.SLASH
            }
        }
        if (urlMapping.urlData.hasOptionalExtension()) {
            final allConstraints = urlMapping.constraints
            ConstrainedProperty lastConstraint = (ConstrainedProperty) allConstraints[-1]
            urlPattern << "(.\${${lastConstraint.propertyName})?"
        }
        return apiDocumentation ? urlPattern.toString().replace('/api/', '{{base_url}}/') : urlPattern.toString()
    }

    private boolean urlsMatch(String documentedUrl, String mappedUrl) {
        placeHolderUrl(documentedUrl) == placeHolderUrl(mappedUrl)
    }

    private String placeHolderUrl(String url) {
        // Make the url /api/ for ease of reading when debugging
        // Replace all {{xxx}} with {{property_placeholder}} allowing for {{$randomXxx}} and {{xxx}}?
        // Remove all query params
        url.replaceFirst(/(\{\{base_url}}|http:\/\/localhost\/api)/, '/api')
            .replaceFirst(/\?(\w+=\{{0,2}\w+}{0,2}&?)+/, '')
            .replaceAll(/\{{2}\$?\w+}{2}\??/, '{{property_placeholder}}')

    }

    private List<DocumentedUrl> getApiDocumentedUrls() {
        String collectionJsonUrl = 'https://documenter.gw.postman.com'
        HttpClient client = new DefaultHttpClient(new URL(collectionJsonUrl).toURI(),
                                                  new DefaultHttpClientConfiguration().with {
                                                      setReadTimeout(Duration.ofMinutes(30))
                                                      setReadIdleTimeout(Duration.ofMinutes(30))
                                                      it
                                                  })
        Map data
        try {
            data = client.toBlocking().retrieve(HttpRequest.GET('/api/collections/9840589/UVC8BkkA')
                                                    .contentType(MediaType.APPLICATION_JSON), Argument.of(Map))
        } catch (HttpClientResponseException exception) {
            return []
        }

        recurseItem(data.item as List<Map>)
    }

    private List<DocumentedUrl> recurseItem(List<Map> items) {
        if (!items) return []

        items.collectMany {i ->
            if (i.item) recurseItem(i.item as List<Map>)
            else if (i.request) {
                [new DocumentedUrl(i.request.method, i.request.url)]
            } else []
        }
    }

    class DocumentedUrl {
        String method
        String url

        DocumentedUrl(String method, String url) {
            this.method = method
            this.url = url
        }
    }

    enum CheckingType {
        TRACKING,
        DOCUMENTED
    }
}
