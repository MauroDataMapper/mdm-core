package uk.ac.ox.softeng.maurodatamapper.core.facet


import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration

import static io.micronaut.http.HttpStatus.OK

/**
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataController* Controller: metadata
 *  | GET    | /api/metadata/namespaces/${id}? | Action: namespaces  |
 */
@Integration
@Transactional
class MetadataFunctionalSpec extends BaseFunctionalSpec {

    @Override
    String getResourcePath() {
        'metadata'
    }

    void 'test getting all metadata namespaces'() {

        when:
        GET('namespaces', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
  {
    "defaultNamespace": true,
    "editable": true,
    "keys": [
      
    ],
    "namespace": "uk.ac.ox.softeng.maurodatamapper.core.provider.email"
  }
]'''
    }

    void 'test getting metadata namespaces for namespace'() {
        when: 'testing unknown namespace'
        GET('namespaces/functional.test.unknown.namespace', STRING_ARG)

        then:
        verifyJsonResponse OK, '[]'

        when: 'testing known namespace'
        GET('namespaces/uk.ac.ox.softeng.maurodatamapper.core.provider.email', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
  {
    "defaultNamespace": true,
    "editable": true,
    "keys": [
      
    ],
    "namespace": "uk.ac.ox.softeng.maurodatamapper.core.provider.email"
  }
]'''
    }
}