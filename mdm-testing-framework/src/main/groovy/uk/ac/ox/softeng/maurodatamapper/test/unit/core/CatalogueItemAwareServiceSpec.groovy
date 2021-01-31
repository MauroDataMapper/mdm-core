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

import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.CatalogueItemAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.CatalogueItemAwareService
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import spock.lang.Stepwise

/**
 * @since 03/02/2020
 */
@Stepwise
abstract class CatalogueItemAwareServiceSpec<D extends CatalogueItemAware, T extends CatalogueItemAwareService> extends BaseUnitSpec {

    abstract CatalogueItem getCatalogueItem()

    abstract CatalogueItem getCatalogueItemFromStorage()

    abstract T getService()

    abstract D getAwareItem()

    abstract D getUpdatedAwareItem()

    abstract int getExpectedCountOfAwareItemsInCatalogueItem()

    abstract String getChangedPropertyName()

    void 'CAIS01 - Test findAllByCatalogueItemId'() {
        when:
        List awareItems = service.findAllByCatalogueItemId(UUID.randomUUID(), [:])

        then:
        !awareItems

        when:
        awareItems = service.findAllByCatalogueItemId(getCatalogueItem().id, [:])

        then:
        awareItems.size() == getExpectedCountOfAwareItemsInCatalogueItem()
    }

    void 'CAIS02 - Test addCreatedEditToCatalogueItem'() {
        when:
        def awareItem = service.addCreatedEditToCatalogueItem(admin, getAwareItem(), getCatalogueItem().class.simpleName, getCatalogueItem().id)
        CatalogueItem upd = getCatalogueItemFromStorage()

        then:
        awareItem
        upd

        and:
        upd.edits.size() == 1
        upd.edits[0].description == "[${getAwareItem().editLabel}] added to component [${getCatalogueItem().editLabel}]"
    }

    void 'CAIS03 - Test addUpdatedEditToCatalogueItem'() {
        when:
        def awareItem =
            service.addUpdatedEditToCatalogueItem(admin, getUpdatedAwareItem(), getCatalogueItem().class.simpleName, getCatalogueItem().id,
                                                  [getChangedPropertyName()])
        CatalogueItem upd = getCatalogueItemFromStorage()

        then:
        awareItem
        upd

        and:
        upd.edits.size() == 1
        upd.edits[0].description == "[${getAwareItem().editLabel}] changed properties [${getChangedPropertyName()}]"
    }

    void 'CAIS04 - Test addDeletedEditToCatalogueItem'() {
        when:
        def awareItem = service.addDeletedEditToCatalogueItem(admin, getAwareItem(), getCatalogueItem().class.simpleName, getCatalogueItem().id)
        CatalogueItem upd = getCatalogueItemFromStorage()

        then:
        awareItem
        upd

        and:
        upd.edits.size() == 1
        upd.edits[0].description == "[${getAwareItem().editLabel}] removed from component [${getCatalogueItem().editLabel}]"
    }
}
