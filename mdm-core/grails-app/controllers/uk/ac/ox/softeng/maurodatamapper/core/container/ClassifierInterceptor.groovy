/*
 * Copyright 2020-2024 University of Oxford and NHS England
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package uk.ac.ox.softeng.maurodatamapper.core.container

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.interceptor.SecurableResourceInterceptor
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiProperty
import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyEnum
import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyService
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import org.springframework.beans.factory.annotation.Autowired
import groovy.util.logging.Slf4j

@Slf4j
class ClassifierInterceptor extends SecurableResourceInterceptor {

    ApiPropertyService apiPropertyService

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
        params.id ?: params.classifierId
    }

    @Override
    def <S extends SecurableResource> Class<S> getSecuredClass() {
        Classifier as Class<S>
    }

    boolean before() {
        securableResourceChecks()


        if (actionName == 'catalogueItems') {
            return currentUserSecurityPolicyManager.userCanReadSecuredResourceId(Classifier, getId()) ?: notFound(Classifier, getId())
        }

        // Must use "containsKey" incase the field is provided but that value is null
        if (params.containsKey('catalogueItemId')) {
            if (isUpdate()) return methodNotAllowed('Cannot update via a catalogue item')

            if (Utils.parentClassIsAssignableFromChild(SecurableResource, params.catalogueItemClass)) {
                return checkActionAuthorisationOnUnsecuredResource(params.catalogueItemClass, params.catalogueItemId, null, null)
            }

            ModelItem modelItem = findModelItemByDomainTypeAndId(params.catalogueItemClass, params.catalogueItemId)
            Model model = modelItem.getModel()
            return checkActionAuthorisationOnUnsecuredResource(params.catalogueItemClass, params.catalogueItemId, model.getClass(), model.getId())

        }

        if (isSave()) {
            // Check api property to determine if new classifiers can be
            // created by a non-admin user
            if (restrictClassifierCreate()
                && !currentUserSecurityPolicyManager.isApplicationAdministrator()
                && !getId()) {
                log.warn("Additions of new classifiers by this user are not allowed")
                return forbiddenDueToPermissions()
            }
        }

        checkActionAuthorisationOnSecuredResource(Classifier, getId(), true)
    }

    ModelItem findModelItemByDomainTypeAndId(Class domainType, UUID catalogueItemId) {
        ModelItemService service = modelItemServices.find {it.handles(domainType)}
        if (!service) throw new ApiBadRequestException('CI01', "Classifier retrieval for model item [${domainType}] with no supporting service")
        service.get(catalogueItemId)
    }

    private boolean restrictClassifierCreate() {
        ApiProperty property = apiPropertyService.findByApiPropertyEnum(ApiPropertyEnum.SECURITY_RESTRICT_CLASSIFIER_CREATE)
        property?.value?.toBoolean()
    }
}
