package uk.ac.ox.softeng.maurodatamapper.test.functional.facet

import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType

import groovy.util.logging.Slf4j

/**
 * Where facet owner is a DataClass
 *
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkController* Controller: versionLink
 *  |   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/versionLinks       | Action: save                                 |
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/versionLinks       | Action: index                                |
 *  |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/versionLinks/${id} | Action: delete                               |
 *  |   PUT    | /api/${catalogueItemDomainType}/${catalogueItemId}/versionLinks/${id} | Action: update                               |
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/versionLinks/${id} | Action: show                                 |
 */
@Slf4j
abstract class ModelVersionLinkFunctionalSpec extends CatalogueItemFacetFunctionalSpec<VersionLink> {

    abstract String getTargetModelId()

    abstract String getTargetModelDomainType()

    abstract String getModelDomainType()

    abstract String getTargetModelJsonString()

    abstract String getSourceModelJsonString()

    String getModelId() {
        getCatalogueItemId().toString()
    }

    @Override
    String getFacetResourcePath() {
        'versionLinks'
    }

    @Override
    Map getValidJson() {
        [
            linkType             : VersionLinkType.NEW_MODEL_VERSION_OF.label,
            targetModelId        : getTargetModelId(),
            targetModelDomainType: getTargetModelDomainType()
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            linkType   : VersionLinkType.NEW_MODEL_VERSION_OF.label,
            targetModel: getModelId(),

        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            linkType: VersionLinkType.NEW_DOCUMENTATION_VERSION_OF.label
        ]
    }

    @Override
    String getExpectedShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "linkType": "''' + VersionLinkType.NEW_MODEL_VERSION_OF.label + '''",
  "domainType": "VersionLink",
  "sourceModel": ''' + getSourceModelJsonString() + ''',
  "targetModel": ''' + getTargetModelJsonString() + '''
}'''
    }

    @Override
    void verifyR4UpdateResponse() {
        super.verifyR4UpdateResponse()
        response.body().linkType == VersionLinkType.NEW_DOCUMENTATION_VERSION_OF.label
    }
}