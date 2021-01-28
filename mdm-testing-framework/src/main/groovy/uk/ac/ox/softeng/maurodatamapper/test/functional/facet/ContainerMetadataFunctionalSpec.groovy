package uk.ac.ox.softeng.maurodatamapper.test.functional.facet

import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata

import grails.gorm.transactions.Transactional
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus

/**
 * Where facet owner is a Container
 *
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataController* Controller: metadata
 *  |   POST   | /api/${containerDomainType}/${containerId}/metadata         | Action: save                                 |
 *  |   GET    | /api/${containerDomainType}/${containerId}/metadata         | Action: index                                |
 *  |  DELETE  | /api/${containerDomainType}/${containerId}/metadata/${id}   | Action: delete                               |
 *  |   PUT    | /api/${containerDomainType}/${containerId}/metadata/${id}   | Action: update                               |
 *  |   GET    | /api/${containerDomainType}/${containerId}/metadata/${id}   | Action: show                                 |
 */
@Slf4j
abstract class ContainerMetadataFunctionalSpec extends ContainerFacetFunctionalSpec<Metadata> {


    @OnceBefore
    @Transactional
    def cleanUpMetadataBefore() {
        Metadata.deleteAll(Metadata.list())
        sessionFactory.currentSession.flush()
    }


    @Override
    String getFacetResourcePath() {
        'metadata'
    }

    @Override
    Map getValidJson() {
        [
                namespace: 'functional.test.namespace',
                key      : 'ftk',
                value    : 'ftv'
        ]
    }

    @Override
    Map getInvalidJson() {
        [
                namespace: null,
                key      : 'ftk',
                value    : 'ftv'
        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
                value: 'ftv.update'
        ]
    }

    @Override
    String getExpectedShowJson() {
        '''{
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "namespace": "functional.test.namespace",
  "id": "${json-unit.matches:id}",
  "value": "ftv",
  "key": "ftk"
}'''
    }

    @Override
    void verifyR4UpdateResponse() {
        super.verifyR4UpdateResponse()
        response.body().value == 'ftv.update'
    }

}