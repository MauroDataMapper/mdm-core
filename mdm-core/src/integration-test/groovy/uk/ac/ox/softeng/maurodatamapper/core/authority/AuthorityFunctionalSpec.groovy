package uk.ac.ox.softeng.maurodatamapper.core.authority

import grails.core.GrailsApplication
import io.micronaut.core.type.Argument
import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec


@Integration
class AuthorityFunctionalSpec extends BaseFunctionalSpec {
    @Autowired
    ApplicationContext applicationContext

    GrailsApplication grailsApplication

    String getResourcePath() {
        'authorities'
    }

    void 'Test instance rendered default authority correctly'() {
        when: 'When the show action is called to retrieve a resource'
        io.micronaut.http.HttpResponse<List> localResponse = GET("${baseUrl}authorities", Argument.of(List, Map))

        then: 'The response is correct'
        localResponse.body()
        localResponse.body().size() == 1

        Map defaultAuthority = localResponse.body().find {it.label == grailsApplication.config.getProperty(Authority.DEFAULT_NAME_CONFIG_PROPERTY)}
        defaultAuthority

        defaultAuthority.url == grailsApplication.config.getProperty(Authority.DEFAULT_URL_CONFIG_PROPERTY)
    }


    void 'Test get authority'() {
        when: 'When the show action is called to retrieve a resource'
        io.micronaut.http.HttpResponse<List> localResponse = GET("${baseUrl}authorities", Argument.of(List, Map))

        then: 'The response is correct'
        localResponse.body()

        Map defaultAuthority = localResponse.body().find {it.label == grailsApplication.config.getProperty(Authority.DEFAULT_NAME_CONFIG_PROPERTY)}
        defaultAuthority

        String id = defaultAuthority.id

        when:
        io.micronaut.http.HttpResponse<List> getAuthorityByIdResponse = GET("${baseUrl}authorities/${id}", Argument.of(List, Map))

        then:
        getAuthorityByIdResponse.body()
        Map getAuthorityByIdMap = getAuthorityByIdResponse.body().find {it.label == grailsApplication.config.getProperty(Authority.DEFAULT_NAME_CONFIG_PROPERTY)}
        getAuthorityByIdMap

        getAuthorityByIdMap ==  [id: id,
                                label: grailsApplication.config.getProperty(Authority.DEFAULT_NAME_CONFIG_PROPERTY),
                                url: grailsApplication.config.getProperty(Authority.DEFAULT_URL_CONFIG_PROPERTY)]

    }
}
