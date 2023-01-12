/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.test.json

import grails.plugin.json.view.test.JsonRenderResult
import grails.views.json.test.JsonViewUnitTest
import org.grails.testing.GrailsApplicationBuilder

/**
 * @since 20/10/2017
 */
class JsonViewRenderer implements JsonViewUnitTest {

    @Override
    Set<String> getIncludePlugins() {
        GrailsApplicationBuilder.DEFAULT_INCLUDED_PLUGINS + ['i18n', 'urlMappings', 'markupView', 'jsonView'].toSet()
    }


    JsonRenderResult renderEmpty() {
        JsonRenderResult result = new JsonRenderResult()
        result.jsonText = ''
        result
    }

    JsonRenderResult render(domain, String template = null) {
        if (template) render(template, getRenderModel(domain))
        else render(template: getDomainTemplateUri(domain), model: getRenderModel(domain))
    }

    String getDomainTemplateUri(domain) {
        String domainName = domain.class.simpleName
        "/${domainName.uncapitalize()}/${domainName.uncapitalize()}"
    }

    Map getRenderModel(domain) {
        Map<String, Object> map = [pageView: true]
        map.put("${domain.class.simpleName.uncapitalize()}".toString(), domain)
        map
    }
}
