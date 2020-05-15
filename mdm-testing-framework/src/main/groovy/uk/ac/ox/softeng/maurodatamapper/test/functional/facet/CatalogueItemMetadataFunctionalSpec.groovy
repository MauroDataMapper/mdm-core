package uk.ac.ox.softeng.maurodatamapper.test.functional.facet

import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata

import groovy.util.logging.Slf4j

/**
 * Where facet owner is a DataClass
 *
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataController* Controller: metadata
 *  |   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/metadata         | Action: save                                 |
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/metadata         | Action: index                                |
 *  |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/metadata/${id}   | Action: delete                               |
 *  |   PUT    | /api/${catalogueItemDomainType}/${catalogueItemId}/metadata/${id}   | Action: update                               |
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/metadata/${id}   | Action: show                                 |
 */
@Slf4j
abstract class CatalogueItemMetadataFunctionalSpec extends CatalogueItemFacetFunctionalSpec<Metadata> {

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