/*
 * Copyright 2020 University of Oxford
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
package uk.ac.ox.softeng.maurodatamapper.core.model

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager

abstract class ModelItemService<K extends ModelItem> extends CatalogueItemService<K> {

    @Override
    Class<K> getCatalogueItemClass() {
        return getModelItemClass()
    }

    abstract Class<K> getModelItemClass()

    void deleteAllByModelId(UUID modelId) {
        throw new ApiNotYetImplementedException('MISXX', "deleteAllByModelId for ${getModelItemClass().simpleName}")
    }

    K copy(Model copiedModel, K original, UserSecurityPolicyManager userSecurityPolicyManager) {
        throw new ApiNotYetImplementedException('MISXX', "copy [for ModelItem ${getModelItemClass().simpleName}]")
    }

    K copy(Model copiedModel, K original, UserSecurityPolicyManager userSecurityPolicyManager, UUID parentId) {
        throw new ApiNotYetImplementedException('MISXX', "copy [for ModelItem ${getModelItemClass().simpleName}] (with parent id)")
    }

}