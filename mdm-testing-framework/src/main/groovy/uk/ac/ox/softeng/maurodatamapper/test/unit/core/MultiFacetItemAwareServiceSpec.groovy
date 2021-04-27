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
package uk.ac.ox.softeng.maurodatamapper.test.unit.core

import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.MultiFacetItemAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MultiFacetItemAwareService
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import spock.lang.Stepwise

/**
 * @since 03/02/2020
 */
@Stepwise
abstract class MultiFacetItemAwareServiceSpec<D extends MultiFacetItemAware, T extends MultiFacetItemAwareService> extends BaseUnitSpec {

    abstract MultiFacetAware getMultiFacetAwareItem()

    abstract MultiFacetAware getMultiFacetAwareItemFromStorage()

    abstract T getService()

    abstract D getAwareItem()

    abstract D getUpdatedAwareItem()

    abstract int getExpectedCountOfAwareItemsInMultiFacetAwareItem()

    abstract String getChangedPropertyName()

    void 'CAIS01 - Test findAllByMultiFacetAwareItemId'() {
        when:
        List awareItems = service.findAllByMultiFacetAwareItemId(UUID.randomUUID(), [:])

        then:
        !awareItems

        when:
        awareItems = service.findAllByMultiFacetAwareItemId(getMultiFacetAwareItem().id, [:])

        then:
        awareItems.size() == getExpectedCountOfAwareItemsInMultiFacetAwareItem()
    }

    void 'CAIS02 - Test addCreatedEditToMultiFacetAwareItem'() {
        when:
        def awareItem =
            service.addCreatedEditToMultiFacetAwareItem(admin, getAwareItem(), getMultiFacetAwareItem().class.simpleName, getMultiFacetAwareItem().id)
        MultiFacetAware upd = getMultiFacetAwareItemFromStorage()

        then:
        awareItem
        upd

        and:
        upd.edits.size() == 1
        upd.edits[0].description == "[${getAwareItem().editLabel}] added to component [${getMultiFacetAwareItem().editLabel}]"
    }

    void 'CAIS03 - Test addUpdatedEditToMultiFacetAwareItem'() {
        when:
        def awareItem =
            service.addUpdatedEditToMultiFacetAwareItem(admin, getUpdatedAwareItem(), getMultiFacetAwareItem().class.simpleName,
                                                        getMultiFacetAwareItem().id,
                                                        [getChangedPropertyName()])
        MultiFacetAware upd = getMultiFacetAwareItemFromStorage()

        then:
        awareItem
        upd

        and:
        upd.edits.size() == 1
        upd.edits[0].description == "[${getAwareItem().editLabel}] changed properties [${getChangedPropertyName()}]"
    }

    void 'CAIS04 - Test addDeletedEditToMultiFacetAwareItem'() {
        when:
        def awareItem =
            service.addDeletedEditToMultiFacetAwareItem(admin, getAwareItem(), getMultiFacetAwareItem().class.simpleName, getMultiFacetAwareItem().id)
        MultiFacetAware upd = getMultiFacetAwareItemFromStorage()

        then:
        awareItem
        upd

        and:
        upd.edits.size() == 1
        upd.edits[0].description == "[${getAwareItem().editLabel}] removed from component [${getMultiFacetAwareItem().editLabel}]"
    }
}
