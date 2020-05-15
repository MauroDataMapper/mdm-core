package uk.ac.ox.softeng.maurodatamapper.test.functional.facet

import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus

/**
 * Where facet owner is a DataClass
 *
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.AnnotationController* Controller: annotation
 |   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations          | Action: save                                 |
 |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations          | Action: index                                |
 |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations/${id}    | Action: delete                               |
 |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations/${id}    | Action: show                                 |
 */
@Slf4j
abstract class CatalogueItemAnnotationFunctionalSpec extends CatalogueItemFacetFunctionalSpec<Annotation> {
    @Override
    String getFacetResourcePath() {
        'annotations'
    }

    @Override
    Map getValidJson() {
        [
            label      : 'Some interesting comment',
            description: 'Why are we writing these tests?'
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            description: 'A new annotation'
        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            description: 'Attempting update'
        ]
    }

    @Override
    String getExpectedShowJson() {
        '''{
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "createdBy": "unlogged_user@mc-core.com",
  "description": "Why are we writing these tests?",
  "id": "${json-unit.matches:id}",
  "label": "Some interesting comment"
}'''
    }

    @Override
    void verifyR4InvalidUpdateResponse() {
        verifyResponse(HttpStatus.NOT_FOUND, response)
    }

    @Override
    void verifyR4UpdateResponse() {
        verifyResponse(HttpStatus.NOT_FOUND, response)
    }
}