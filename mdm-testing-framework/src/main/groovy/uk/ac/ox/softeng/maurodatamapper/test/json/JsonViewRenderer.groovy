package uk.ac.ox.softeng.maurodatamapper.test.json

import grails.plugin.json.view.test.JsonRenderResult
import grails.plugin.json.view.test.JsonViewTest

/**
 * @since 20/10/2017
 */
class JsonViewRenderer implements JsonViewTest {

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
