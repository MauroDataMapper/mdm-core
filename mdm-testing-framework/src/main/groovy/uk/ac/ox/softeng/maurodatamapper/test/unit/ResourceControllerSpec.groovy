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
package uk.ac.ox.softeng.maurodatamapper.test.unit

import uk.ac.ox.softeng.maurodatamapper.security.basic.PublicAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.test.json.JsonViewRenderer

import grails.plugin.json.builder.JsonOutput
import grails.plugin.json.view.test.JsonRenderResult
import grails.testing.gorm.DomainUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import grails.views.mvc.GenericGroovyTemplateViewResolver
import groovy.transform.SelfType
import groovy.util.logging.Slf4j
import net.javacrumbs.jsonunit.core.Option
import org.grails.web.servlet.view.CompositeViewResolver

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

/**
 * @since 11/12/2017
 */
@Slf4j
@SelfType([DomainUnitTest, ControllerUnitTest])
abstract class ResourceControllerSpec<D> extends BaseUnitSpec implements JsonWebUnitSpec {

    abstract String getExpectedIndexJson()

    abstract String getExpectedNullSavedJson()

    abstract String getExpectedInvalidSavedJson()

    abstract String getExpectedValidSavedJson()

    abstract String getExpectedShowJson()

    abstract String getExpectedInvalidUpdatedJson()

    abstract String getExpectedValidUpdatedJson()

    abstract D invalidUpdate(D instance)

    abstract D validUpdate(D instance)

    abstract D getInvalidUnsavedInstance()

    abstract D getValidUnsavedInstance()

    static UUID randomId1
    static UUID randomId2

    JsonViewRenderer jsonViewRenderer = new JsonViewRenderer()

    String testIndex

    def setupSpec() {
        log.debug('Setting up resource controller unit spec')
        randomId1 = UUID.randomUUID()
        randomId2 = UUID.randomUUID()
        // The grails unit spec loads th composite view resolver but only with the gsp resolver
        // We need to add the jsonViewResolver
        // Weirdly the base spec does create the smart view resolvers so they are available as referenced beans
        defineBeans {
            jsonViewResolver(GenericGroovyTemplateViewResolver, ref('jsonSmartViewResolver'))
            "${CompositeViewResolver.BEAN_NAME}"(CompositeViewResolver)
        }
    }

    def setup() {
        log.debug('Setting up resource controller unit')
        request.json = ''
        testIndex = specificationContext.currentIteration.name.find(/R\d(\.\d)?/)
    }

    void "R1 - Test the index action returns the correct response"() {
        given:
        response.reset()
        givenParameters()

        when: "The index action is executed"
        controller.index()

        then: "The response is correct"
        verifyJsonResponse OK, getExpectedIndexJson()
    }


    void "R2.1 - Test the save action with a null instance"() {
        given:
        response.reset()
        givenParameters()

        when:
        request.method = 'POST'
        controller.save()

        then:
        verifyJsonResponse UNPROCESSABLE_ENTITY, getExpectedNullSavedJson(), Option.IGNORING_EXTRA_FIELDS
    }

    void "R2.2 - Test the save action with an invalid instance"() {
        given:
        response.reset()
        givenParameters()

        when:
        request.method = 'POST'
        setRequestJson getInvalidUnsavedInstance(), getTemplate()
        controller.save()

        then:
        verifyJsonResponse UNPROCESSABLE_ENTITY, getExpectedInvalidSavedJson(), Option.IGNORING_EXTRA_FIELDS
    }

    void "R2.3 - Test the save action correctly persists"() {
        given:
        response.reset()
        givenParameters()

        when:
        response.reset()
        request.method = 'POST'
        setRequestJson getValidUnsavedInstance(), getTemplate()
        controller.save()

        then:
        verifyJsonResponse CREATED, getExpectedValidSavedJson()
    }

    void "R3.1 - Test the show action with a null id"() {
        given:
        response.reset()
        givenParameters()

        when: "The show action is executed with a null domain"
        controller.show()

        then: "A 404 error is returned"
        verifyR31ShowNullIdResponse()
    }

    void "R3.2 - Test the show action with an invalid id"() {
        given:
        response.reset()
        givenParameters()

        when: "A domain instance is passed to the show action"
        params.id = randomId1
        controller.show()

        then: "A model is populated containing the domain instance"
        verifyR32ShowInvalidIdResponse()
    }

    void "R3.3 - Test the show action with a valid id"() {
        given:
        response.reset()
        givenParameters()
        assert domain.id

        when: "A domain instance is passed to the show action"
        params.id = domain.id
        controller.show()

        then: "A model is populated containing the domain instance"
        verifyJsonResponse OK, getExpectedShowJson()
    }

    void "R4.1 - Test the update action with a null instance"() {
        given:
        response.reset()
        givenParameters()

        when:
        request.method = 'PUT'
        params.id = UUID.randomUUID()
        controller.update()

        then:
        verifyResponse NOT_FOUND
    }

    void "R4.2 - Test the update action with an invalid instance"() {
        given:
        response.reset()
        givenParameters()
        assert domain.id

        when:
        request.method = 'PUT'
        def updatedInstance = invalidUpdate(domain)
        params.id = updatedInstance.id
        setRequestJson updatedInstance, getTemplate()
        controller.update()

        then:
        verifyR42UpdateInvalidInstanceResponse()
    }

    void "R4.3 - Test the update action correctly persists"() {
        given:
        response.reset()
        givenParameters()
        assert domain.id

        when:
        request.method = 'PUT'
        def updatedInstance = validUpdate(domain)
        params.id = updatedInstance.id
        setRequestJson updatedInstance, getTemplate()
        controller.update()

        then:
        verifyJsonResponse OK, getExpectedValidUpdatedJson()
    }

    void "R5.1 - Test the delete action with a null instance"() {
        given:
        response.reset()
        givenParameters()

        when:
        request.method = 'DELETE'
        controller.delete()

        then:
        verifyJsonResponse NOT_FOUND, getNotFoundNullJson()
    }

    void "R5.2 - Test the delete action with an invalid instance"() {
        given:
        response.reset()
        givenParameters()

        when:
        request.method = 'DELETE'
        params.id = randomId1
        controller.delete()

        then:
        verifyJsonResponse NOT_FOUND, getNotFoundIdJson()
    }

    void "R5.3 - Test the delete action with an instance"() {
        given:
        response.reset()
        givenParameters()
        assert domain.id

        when:
        request.method = 'DELETE'
        params.id = domain.id
        controller.delete()

        then:
        verifyR53DeleteActionWithAnInstanceResponse()
    }

    void verifyR32ShowInvalidIdResponse() {
        verifyJsonResponse NOT_FOUND, getNotFoundIdJson()
    }

    void verifyR31ShowNullIdResponse() {
        verifyJsonResponse NOT_FOUND, getNotFoundNullJson()
    }

    void verifyR42UpdateInvalidInstanceResponse() {
        verifyJsonResponse UNPROCESSABLE_ENTITY, getExpectedInvalidUpdatedJson(), Option.IGNORING_EXTRA_FIELDS
    }

    void verifyR53DeleteActionWithAnInstanceResponse() {
        verifyResponse NO_CONTENT
    }

    String getNotFoundNullJson(Class clazz = domain.class) {
        """{
  "resource": "${clazz.simpleName}",
  "id": "null"
}"""
    }

    String getNotFoundIdJson(Class clazz = domain.class) {
        """{
  "resource": "${clazz.simpleName}",
  "id": "\${json-unit.matches:id}"
}"""
    }

    JsonRenderResult renderDomain(D domainObj, String template = null) {
        if (!domainObj) return jsonViewRenderer.renderEmpty()
        jsonViewRenderer.render(domainObj, template)
    }

    void setRequestJson(D domainObj, String template = getTemplate()) {
        JsonRenderResult result = renderDomain(domainObj, template)
        if (!result.jsonText) request.json = ''
        else {
            String strippedText = result.jsonText
                .replaceAll(/"lastUpdated":\s*(".+?"|null),?/, '')
                .replaceAll(/"dateCreated":\s*(".+?"|null),?/, '')

            requestBody = strippedText
            log.trace("Domain JSON\n{}", JsonOutput.prettyPrint(strippedText))
            request.json = strippedText
        }
    }

    void requestJson(D domainObj, String template = getTemplate()) {
        setRequestJson(domainObj, template)
    }

    String getTemplate() {
        null
    }

    void givenParameters() {
        params.currentUserSecurityPolicyManager = PublicAccessSecurityPolicyManager.instance
    }
}
