package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController

class SemanticLinkController extends EditLoggingController<SemanticLink> {

    static responseFormats = ['json', 'xml']

    SemanticLinkService semanticLinkService

    SemanticLinkController() {
        super(SemanticLink)
    }

    @Override
    protected SemanticLink queryForResource(Serializable id) {
        SemanticLink resource = super.queryForResource(id) as SemanticLink
        semanticLinkService.loadCatalogueItemsIntoSemanticLink(resource)
    }

    @Override
    protected List<SemanticLink> listAllReadableResources(Map params) {
        List<SemanticLink> semanticLinks
        switch (params.type) {
            case 'source':
                semanticLinks = semanticLinkService.findAllBySourceCatalogueItemId(params.catalogueItemId, params)
                break
            case 'target':
                semanticLinks = semanticLinkService.findAllByTargetCatalogueItemId(params.catalogueItemId, params)
                break
            default:
                semanticLinks = semanticLinkService.findAllBySourceOrTargetCatalogueItemId(params.catalogueItemId, params)
        }
        semanticLinkService.loadCatalogueItemsIntoSemanticLinks(semanticLinks)
    }

    @Override
    void serviceDeleteResource(SemanticLink resource) {
        semanticLinkService.delete(resource)
    }

    @Override
    protected SemanticLink createResource() {
        SemanticLink resource = super.createResource() as SemanticLink
        resource.catalogueItem = semanticLinkService.findCatalogueItemByDomainTypeAndId(params.catalogueItemDomainType, params.catalogueItemId)
        resource
    }

    @Override
    protected SemanticLink saveResource(SemanticLink resource) {
        semanticLinkService.loadCatalogueItemsIntoSemanticLink(resource)
        resource.save flush: true, validate: false
        semanticLinkService.addCreatedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId)
    }

    @Override
    protected SemanticLink updateResource(SemanticLink resource) {
        semanticLinkService.loadCatalogueItemsIntoSemanticLink(resource)
        resource.save flush: true, validate: false
        semanticLinkService.addUpdatedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId)
    }

    @Override
    protected void deleteResource(SemanticLink resource) {
        serviceDeleteResource(resource)
        semanticLinkService.addDeletedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId)
    }
}
