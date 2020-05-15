package uk.ac.ox.softeng.maurodatamapper.core.tree

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor
import uk.ac.ox.softeng.maurodatamapper.util.Utils
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource

import grails.core.GrailsClass
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus

@Slf4j
class TreeItemInterceptor implements MdmInterceptor {

    @Autowired(required = false)
    List<ModelItemService> modelItemServices

    boolean before() {
        mapDomainTypeToClass('container', true)


        if (!params.containerClass) {
            render status: HttpStatus.NOT_FOUND, model: [path: request.requestURI]
            return false
        }

        if (actionName in ['documentationSupersededModels', 'modelSupersededModels', 'deletedModels']) {
            return currentUserSecurityPolicyManager.isApplicationAdministrator() ?: unauthorised()
        }

        if (isShow()) {
            mapDomainTypeToClass('catalogueItem', true)
            // Verify the user can see the catalogue item to allow them to see the tree
            if (Utils.parentClassIsAssignableFromChild(SecurableResource, params.catalogueItemClass)) {
                return checkActionAuthorisationOnUnsecuredResource(params.catalogueItemClass, params.catalogueItemId, null, null)
            }

            Model model = getOwningModel()
            return checkActionAuthorisationOnUnsecuredResource(params.catalogueItemClass, params.catalogueItemId, model.getClass(), model.getId())
        }
        // Otherwise top level action so should be allowed through
        true
    }

    Model getOwningModel() {
        ModelItem modelItem = findModelItemByDomainTypeAndId(params.catalogueItemClass, params.catalogueItemId)
        Model model = modelItem.getModel()
    }

    ModelItem findModelItemByDomainTypeAndId(Class domainType, UUID catalogueItemId) {
        ModelItemService service = modelItemServices.find {it.handles(domainType)}
        if (!service) throw new ApiBadRequestException('FI01', "TreeItem retrieval for model item [${domainType}] with no supporting service")
        service.get(catalogueItemId)
    }

}
