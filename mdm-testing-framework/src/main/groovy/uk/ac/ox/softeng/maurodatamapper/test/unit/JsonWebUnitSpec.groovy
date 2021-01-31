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
package uk.ac.ox.softeng.maurodatamapper.test.unit

import uk.ac.ox.softeng.maurodatamapper.test.json.JsonComparer

import grails.artefact.Controller
import grails.testing.web.GrailsWebUnitTest
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import net.javacrumbs.jsonunit.core.Option
import org.grails.testing.GrailsApplicationBuilder
import org.grails.web.pages.GroovyPagesUriSupport
import org.grails.web.util.GrailsApplicationAttributes
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.View
import org.springframework.web.servlet.view.AbstractUrlBasedView

import static io.micronaut.http.HttpStatus.valueOf

/**
 * @since 17/10/2017
 */
@Slf4j
trait JsonWebUnitSpec extends GrailsWebUnitTest implements JsonComparer {

    String requestBody

    abstract Map getModel()

    @Override
    Set<String> getIncludePlugins() {
        GrailsApplicationBuilder.DEFAULT_INCLUDED_PLUGINS + ['i18n', 'urlMappings', 'markupView', 'jsonView'].toSet()
    }

    void verifyResponse(HttpStatus expected) {
        HttpStatus actual = valueOf(status)
        if (actual != expected) {
            log.error('Failed Response :: {}[{}]\n{}', actual.code, actual.reason, prettyPrint(response.text))
        }
        assert actual == expected
    }

    void verifyJsonResponse(HttpStatus expectedStatus, String expectedJson, Option... addtlOptions = new Option[0]) {
        verifyResponse(expectedStatus)
        // Since grails 4.0.5 the controller testing doesn't render the JSON views when using Grails Views
        // The model and view are set but the response JSON is not, therefore we attempt to render here if its not already done so
        if (model && !response.text) renderModelUsingView()
        if (expectedJson) {
            assert response.text != null
            def actual = response.text
            try {
                actual = response.json
            } catch (Exception ignored) {
                log.warn('Cannot render JSON: \n{}', response.text)
            }
            verifyJson(expectedJson, actual, addtlOptions)
        } else {
            if (response.text) {
                log.error('Response : {}', response.text)
                assert !response.text, 'Should be no content in the response'
            }
        }
    }

    /*
    Default controller spec getView doesnt work when Grails Views are used, the resulting view string is a standard gsp page.
    Therefore we override to return the URL from the TemplateView class if the standard viewName doesnt work.
     */

    String getView() {
        final Controller controller = request.getAttribute(GrailsApplicationAttributes.CONTROLLER) as Controller

        ModelAndView modelAndView = controller?.modelAndView
        if (modelAndView && modelAndView.hasView()) {
            final viewName = modelAndView.viewName
            if (viewName) return viewName
            View view = modelAndView.view
            if (view instanceof AbstractUrlBasedView) return view.url
        }

        if (webRequest.controllerName && webRequest.actionName) {
            return new GroovyPagesUriSupport().getViewURI(webRequest.controllerName, webRequest.actionName)
        }
        null
    }

    /*
    Renders the response using the stored ModelAndView.
    This property is set as part of the Grails Views code but the actual render call is not made inside the "respond" call when using Grails Views.
    Therefore any controller unit testing has the model and view set but response.json is empty. Calling this method will solve this issue in the
    most correct way.
     */

    void renderModelUsingView() {
        Controller controller = request.getAttribute(GrailsApplicationAttributes.CONTROLLER)
        ModelAndView modelAndView = controller?.modelAndView
        if (modelAndView) {
            Map model = modelAndView.model
            View view = modelAndView.view
            if (view) {
                view.render(model, request, response)
            }
        }
    }
}