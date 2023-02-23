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
package uk.ac.ox.softeng.maurodatamapper.test.functional

import uk.ac.ox.softeng.maurodatamapper.test.MdmSpecification
import uk.ac.ox.softeng.maurodatamapper.test.http.RestClientInterface
import uk.ac.ox.softeng.maurodatamapper.test.json.ResponseComparer

import grails.gorm.transactions.Transactional
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MutableHttpRequest
import io.micronaut.http.client.DefaultHttpClientConfiguration
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.http.client.netty.DefaultHttpClient
import io.micronaut.http.netty.cookies.NettyCookie
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.cookie.Cookie
import io.netty.handler.codec.http.cookie.ServerCookieDecoder
import org.grails.datastore.gorm.GormEntity
import org.hibernate.HibernateException
import org.hibernate.SessionFactory
import org.spockframework.util.Assert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import spock.lang.AutoCleanup
import spock.lang.Shared

import java.time.Duration
import javax.persistence.OptimisticLockException

/**
 * Important notes are the new Grails Annotations and use of JUnit Annotations
 * @Before. There is a difference between these and Spock setupSpec and setup, namely
 * dependency injection. The methods annotated will be run AFTER dependency injection and therefore if you
 * want/need autowired properties you need to use these.
 * It also means you can define multiple setup methods
 *
 * Use
 * <pre>
 * @RunOnce
 * @Transactional
 *     def setup() {*      log.debug('Check and setup test data')
 *      // Install all required domain data here
 *}* </pre>
 * and
 * <pre>
 * @Transactional
 *     def cleanupSpec() {*     // Remove all installed required domains here
 *         cleanUpResource()
 *         cleanUpResources()
 *}* </pre>
 */
@Slf4j
abstract class BaseFunctionalSpec extends MdmSpecification implements ResponseComparer, RestClientInterface {

    public static final List<String> REATTEMPT_EXCEPTIONS = ['StaleObjectStateException', 'StaleStateException']

    @Shared
    String baseUrl

    @Shared
    @AutoCleanup
    HttpClient client

    @Autowired
    MessageSource messageSource

    @Autowired
    SessionFactory sessionFactory

    HttpResponse<Map> response

    NettyCookie currentCookie

    abstract String getResourcePath()

    @RunOnce
    def setup() {
        log.debug('Initialise HttpClient')
        baseUrl = "http://localhost:$serverPort/api/"
        this.client = new DefaultHttpClient(new URL(baseUrl).toURI(),
                                            new DefaultHttpClientConfiguration().with {
                                                setReadTimeout(Duration.ofMinutes(30))
                                                setReadIdleTimeout(Duration.ofMinutes(30))
                                                it
                                            })
    }

    def cleanup() {
        log.debug('Cleanup BaseFunctionalSpec')
        cleanUpData()
        response = null
    }

    void cleanUpData(String id = null) {
        // No-op
    }

    Map<String, Object> responseBody() {
        response.body()
    }

    String jsonResponseBody() {
        jsonCapableResponse.body()
    }

    @Transactional
    void safeSessionFlush() {
        try {
            sessionFactory.currentSession.flush()
        } catch (HibernateException | OptimisticLockException exception) {
            log.warn('Unhandled error occured. Ignoring but may cause other tests to fail', exception)
        }
    }

    /********** Rest Requests *******/

    def <O> HttpResponse<O> GET(String resourceEndpoint, Argument<O> bodyType = MAP_ARG, boolean cleanEndpoint = false) {
        GET(resourceEndpoint, bodyType, cleanEndpoint, null)
    }

    def <O> HttpResponse<O> GET(String resourceEndpoint, Argument<O> bodyType, boolean cleanEndpoint, String contentType) {
        HttpResponse<O> localResponse = exchange(HttpRequest.GET(getUrl(resourceEndpoint, cleanEndpoint)), bodyType, contentType)
        if (bodyType.type == String) jsonCapableResponse = localResponse as HttpResponse<String>
        else if (bodyType.type == Map) response = localResponse as HttpResponse<Map>
        localResponse
    }

    def <O> HttpResponse<O> POST(String resourceEndpoint, Map body, Argument<O> bodyType = MAP_ARG, boolean cleanEndpoint = false) {
        HttpResponse<O> localResponse = exchange(HttpRequest.POST(getUrl(resourceEndpoint, cleanEndpoint), body), bodyType)
        if (bodyType.type == String) jsonCapableResponse = localResponse as HttpResponse<String>
        else if (bodyType.type == Map) response = localResponse as HttpResponse<Map>
        localResponse
    }

    /**
     * Use this method if you want to POST a string body. For example, when testing a CSV endpoint, set String body
     * to the CSV text, and set contentType to text/csv
     * @param resourceEndpoint
     * @param body
     * @param bodyType
     * @param cleanEndpoint
     * @param contentType
     * @return
     */
    def <O> HttpResponse<O> POST(String resourceEndpoint, String body, Argument<O> bodyType = STRING_ARG, boolean cleanEndpoint = false, String contentType = null) {
        HttpResponse<O> localResponse = exchange(HttpRequest.POST(getUrl(resourceEndpoint, cleanEndpoint), body), bodyType, contentType)
        if (bodyType.type == String) jsonCapableResponse = localResponse as HttpResponse<String>
        else if (bodyType.type == Map) response = localResponse as HttpResponse<Map>
        localResponse
    }

    def <O> HttpResponse<O> PUT(String resourceEndpoint, Map body, Argument<O> bodyType = MAP_ARG, boolean cleanEndpoint = false) {
        HttpResponse<O> localResponse = exchange(HttpRequest.PUT(getUrl(resourceEndpoint, cleanEndpoint), body), bodyType)
        if (bodyType.type == String) jsonCapableResponse = localResponse as HttpResponse<String>
        else if (bodyType.type == Map) {
            response = localResponse as HttpResponse<Map>
            if (response?.body()?.exception?.type in REATTEMPT_EXCEPTIONS) {
                PUT(resourceEndpoint, body, bodyType, cleanEndpoint)
            }
        }
        localResponse
    }

    def <O> HttpResponse<O> DELETE(String resourceEndpoint, Argument<O> bodyType = MAP_ARG, boolean cleanEndpoint = false) {
        HttpResponse<O> localResponse = exchange(HttpRequest.DELETE(getUrl(resourceEndpoint, cleanEndpoint)), bodyType)
        if (bodyType.type == String) jsonCapableResponse = localResponse as HttpResponse<String>
        else if (bodyType.type == Map) {
            response = localResponse as HttpResponse<Map>
            if (response?.body()?.exception?.type in REATTEMPT_EXCEPTIONS) {
                return DELETE(resourceEndpoint, bodyType, cleanEndpoint) as HttpResponse<O>
            }
        }
        localResponse
    }

    def <O> HttpResponse<O> DELETE(String resourceEndpoint, Map body, Argument<O> bodyType = MAP_ARG) {
        HttpResponse<O> localResponse = exchange(HttpRequest.DELETE(getUrl(resourceEndpoint), body), bodyType)
        if (bodyType.type == String) jsonCapableResponse = localResponse as HttpResponse<String>
        else if (bodyType.type == Map) {
            response = localResponse as HttpResponse<Map>
            if (response?.body()?.exception?.type in REATTEMPT_EXCEPTIONS) {
                return DELETE(resourceEndpoint, body, bodyType) as HttpResponse<O>
            }
        }
        localResponse
    }

    /****** Setup *******/


    void wipeSession() {
        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()
    }

    private String getUrl(String resourceEndpoint, boolean cleanEndpoint = false) {
        if (resourceEndpoint.startsWith('http')) return resourceEndpoint
        if (cleanEndpoint) return resourceEndpoint
        resourcePath ? "$resourcePath/${resourceEndpoint}" : resourceEndpoint
    }

    /**
     * Allows for overriding of basic requests with additional information such as headers.
     * This is called after the cookie is set, and passed directly to the client exchange.
     *
     * @param request
     * @return
     */
    MutableHttpRequest augmentRequest(MutableHttpRequest request) {
        request
    }

    def <O> HttpResponse<O> exchange(MutableHttpRequest request, Argument<O> bodyType = MAP_ARG, String contentType = null) {
        response = null
        jsonCapableResponse = null
        try {
            // IIf there's a cookie saved then add it to the request
            if (currentCookie) request.cookie(currentCookie)
            if (contentType) {
                request.header('Content-Type', contentType)
            }
            HttpResponse<O> httpResponse = client.toBlocking().exchange(augmentRequest(request), bodyType)

            // Preserve the JSESSIONID cookie returned from the server
            if (httpResponse.header(HttpHeaderNames.SET_COOKIE)) {
                Set<Cookie> cookies = ServerCookieDecoder.LAX.decode(httpResponse.header(HttpHeaderNames.SET_COOKIE))
                if (cookies.find {it.name() == 'JSESSIONID'}) currentCookie = new NettyCookie(cookies.find {it.name() == 'JSESSIONID'})
            }

            httpResponse

        } catch (HttpClientResponseException responseException) {
            return responseException.response as HttpResponse<O>
        }
    }

    @Transactional
    def <T extends GormEntity> void cleanUpResources(Class<T>... resourceClasses) {
        try {
            for (Class<T> resourceClass : resourceClasses) {
                cleanUpResource(resourceClass)
            }
        } catch (Exception ex) {
            Assert.fail("Could not perform cleanup: ${ex.message}")
        }
    }

    @Transactional
    def <T extends GormEntity> void cleanUpResource(Class<T> resourceClass) {
        log.info('Cleaning {}', resourceClass)
        resourceClass.deleteAll(resourceClass.list(), flush: true)

        if (resourceClass.count() != 0) {
            Assert.fail("Resource Class ${resourceClass.simpleName} has not been emptied")
        }
    }
}
