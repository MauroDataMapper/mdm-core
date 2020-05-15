package uk.ac.ox.softeng.maurodatamapper.test.unit

import uk.ac.ox.softeng.maurodatamapper.test.json.JsonComparer

import grails.testing.web.GrailsWebUnitTest
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import net.javacrumbs.jsonunit.core.Option

import static io.micronaut.http.HttpStatus.valueOf

/**
 * @since 17/10/2017
 */
@Slf4j
trait JsonWebUnitSpec extends GrailsWebUnitTest implements JsonComparer {

    String requestBody

    void verifyResponse(HttpStatus expected) {
        HttpStatus actual = valueOf(response.status)
        if (actual != expected) {
            log.error('Failed Response :: {}[{}]\n{}', actual.code, actual.reason, prettyPrint(response.text))
        }
        assert actual == expected
    }

    void verifyJsonResponse(HttpStatus expectedStatus, String expectedJson, Option... addtlOptions = new Option[0]) {
        verifyResponse(expectedStatus)
        if (expectedJson) {
            assert response.text != null
            def actual = response.text
            try {
                actual = response.json
            } catch (Exception ignored) {
                log.warn('Cannot render JSON: \n{}', response.text)
            }
            //            if (expectedJson instanceof List) {
            //                verifyJson(expectedJson[0].toString(), actual, (expectedJson.findAll {it instanceof Option}.toArray() as Option[]))
            //            } else
            verifyJson(expectedJson, actual, addtlOptions)
        } else {
            if (response.text) {
                log.error('Response : {}', response.text)
                assert !response.text, 'Should be no content in the response'
            }
        }

    }
}