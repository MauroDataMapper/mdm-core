package uk.ac.ox.softeng.maurodatamapper.core.container

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.interceptor.SecurableResourceInterceptor
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import org.springframework.beans.factory.annotation.Autowired

class ClassifierInterceptor extends SecurableResourceInterceptor {

    @Autowired(required = false)
    List<ModelItemService> modelItemServices

    @Override
    void checkIds() {
        Utils.toUuid(params, 'id')
        Utils.toUuid(params, 'classifierId')
        mapDomainTypeToClass('catalogueItem')
    }

    @Override
    UUID getId() {
        params.classifierId ?: params.id
    }

    @Override
    def <S extends SecurableResource> Class<S> getSecuredClass() {
        Classifier as Class<S>
    }

    boolean before() {
        securableResourceChecks()


        if (actionName == 'catalogueItems') {
            return currentUserSecurityPolicyManager.userCanReadSecuredResourceId(Classifier, getId()) ?: unauthorised()
        }

        // Must use "containsKey" incase the field is provided but that value is null
        if (params.containsKey('catalogueItemId')) {
            if (isUpdate()) return unauthorised()

            if (Utils.parentClassIsAssignableFromChild(SecurableResource, params.catalogueItemClass)) {
                return checkActionAuthorisationOnUnsecuredResource(params.catalogueItemClass, params.catalogueItemId, null, null)
            }

            ModelItem modelItem = findModelItemByDomainTypeAndId(params.catalogueItemClass, params.catalogueItemId)
            Model model = modelItem.getModel()
            return checkActionAuthorisationOnUnsecuredResource(params.catalogueItemClass, params.catalogueItemId, model.getClass(), model.getId())

        }
        checkActionAuthorisationOnSecuredResource(Classifier, getId(), true)
    }

    ModelItem findModelItemByDomainTypeAndId(Class domainType, UUID catalogueItemId) {
        ModelItemService service = modelItemServices.find {it.handles(domainType)}
        if (!service) throw new ApiBadRequestException('CI01', "Classifier retrieval for model item [${domainType}] with no supporting service")
        service.get(catalogueItemId)
    }
}
