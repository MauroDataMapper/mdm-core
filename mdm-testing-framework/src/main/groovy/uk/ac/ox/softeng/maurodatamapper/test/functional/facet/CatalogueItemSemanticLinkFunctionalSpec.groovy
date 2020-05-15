package uk.ac.ox.softeng.maurodatamapper.test.functional.facet


import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType

import groovy.util.logging.Slf4j

/**
 * Where facet owner is a DataClass
 *
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkController* Controller: semanticLink
 *  |   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks       | Action: save                                 |
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks       | Action: index                                |
 *  |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks/${id} | Action: delete                               |
 *  |   PUT    | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks/${id} | Action: update                               |
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks/${id} | Action: show                                 |
 */
@Slf4j
abstract class CatalogueItemSemanticLinkFunctionalSpec extends CatalogueItemFacetFunctionalSpec<SemanticLink> {

    abstract String getTargetCatalogueItemId()

    abstract String getTargetCatalogueItemDomainType()

    abstract String getCatalogueItemDomainType()

    abstract String getTargetCatalogueItemJsonString()

    abstract String getSourceCatalogueItemJsonString()

    @Override
    String getFacetResourcePath() {
        'semanticLinks'
    }

    @Override
    Map getValidJson() {
        [
            linkType                     : SemanticLinkType.REFINES.label,
            targetCatalogueItemId        : getTargetCatalogueItemId(),
            targetCatalogueItemDomainType: getTargetCatalogueItemDomainType()
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            linkType           : SemanticLinkType.REFINES.label,
            targetCatalogueItem: getCatalogueItemId().toString(),

        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            linkType: SemanticLinkType.DOES_NOT_REFINE.label
        ]
    }

    @Override
    String getExpectedShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "linkType": "Refines",
  "domainType": "SemanticLink",
  "sourceCatalogueItem": ''' + getSourceCatalogueItemJsonString() + ''',
  "targetCatalogueItem": ''' + getTargetCatalogueItemJsonString() + '''
}'''
    }

    @Override
    void verifyR4UpdateResponse() {
        super.verifyR4UpdateResponse()
        response.body().linkType == SemanticLinkType.DOES_NOT_REFINE.label
    }
}