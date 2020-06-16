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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item


import uk.ac.ox.softeng.maurodatamapper.datamodel.traits.controller.DataModelSecuredInterceptor
import uk.ac.ox.softeng.maurodatamapper.util.Utils


class DataClassInterceptor extends DataModelSecuredInterceptor {

    @Override
    Class getModelItemClass() {
        DataClass
    }

    @Override
    void checkIds() {
        super.checkIds()
        Utils.toUuid(params, 'dataClassId')
    }

    boolean before() {
        performChecks()

        if (actionName in ['content', 'all', 'search']) {
            return canReadDataModel()
        }

        if (actionName in ['copyDataClass']) {
            return canCopyFromDataModelToOtherDataModel()
        }

        checkStandardActions()
    }


}
