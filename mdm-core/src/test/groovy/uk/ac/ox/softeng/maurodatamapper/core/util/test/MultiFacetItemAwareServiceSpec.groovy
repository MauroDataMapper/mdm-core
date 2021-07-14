/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.core.util.test

import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.MultiFacetItemAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MultiFacetItemAwareService
import uk.ac.ox.softeng.maurodatamapper.test.unit.core.MultiFacetItemAwareServiceSpec as FrameworkMultiFacetItemAwareServiceSpec

/**
 * @since 03/02/2020
 */
abstract class MultiFacetItemAwareServiceSpec<D extends MultiFacetItemAware, T extends MultiFacetItemAwareService>
    extends FrameworkMultiFacetItemAwareServiceSpec<D, T> {

    BasicModel basicModel

    @Override
    MultiFacetAware getMultiFacetAwareItem() {
        basicModel
    }

    @Override
    MultiFacetAware getMultiFacetAwareItemFromStorage() {
        BasicModel.get(basicModel.id)
    }

    abstract int getExpectedCountOfAwareItemsInBasicModel()

    @Override
    int getExpectedCountOfAwareItemsInMultiFacetAwareItem() {
        getExpectedCountOfAwareItemsInBasicModel()
    }
}
