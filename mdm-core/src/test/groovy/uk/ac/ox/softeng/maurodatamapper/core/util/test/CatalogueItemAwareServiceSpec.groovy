package uk.ac.ox.softeng.maurodatamapper.core.util.test


import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.CatalogueItemAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.CatalogueItemAwareService
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import spock.lang.Stepwise

/**
 * @since 03/02/2020
 */
@Stepwise
abstract class CatalogueItemAwareServiceSpec<D extends CatalogueItemAware, T extends CatalogueItemAwareService> extends BaseUnitSpec {

    BasicModel basicModel

    abstract T getService()

    abstract D getAwareItem()

    abstract D getUpdatedAwareItem()

    abstract int getExpectedCountOfAwareItemsInBasicModel()

    abstract String getChangedPropertyName()

    void 'CAIS01 - Test findAllByCatalogueItemId'() {
        when:
        List awareItems = service.findAllByCatalogueItemId(UUID.randomUUID(), [:])

        then:
        !awareItems

        when:
        awareItems = service.findAllByCatalogueItemId(basicModel.id, [:])

        then:
        awareItems.size() == getExpectedCountOfAwareItemsInBasicModel()
    }

    void 'CAIS02 - Test addCreatedEditToCatalogueItem'() {
        when:
        def awareItem = service.addCreatedEditToCatalogueItem(admin, getAwareItem(), BasicModel.simpleName, basicModel.id)
        BasicModel upd = BasicModel.get(basicModel.id)

        then:
        awareItem
        upd

        and:
        upd.edits.size() == 1
        upd.edits[0].description == "[${getAwareItem().editLabel}] added to component [BasicModel:dm1]"
    }

    void 'CAIS03 - Test addUpdatedEditToCatalogueItem'() {
        when:
        def awareItem = service.addUpdatedEditToCatalogueItem(admin, getUpdatedAwareItem(), BasicModel.simpleName, basicModel.id)
        BasicModel upd = BasicModel.get(basicModel.id)

        then:
        awareItem
        upd

        and:
        upd.edits.size() == 1
        upd.edits[0].description == "[${getAwareItem().editLabel}] changed properties [${getChangedPropertyName()}]"
    }

    void 'CAIS04 - Test addDeletedEditToCatalogueItem'() {
        when:
        def awareItem = service.addDeletedEditToCatalogueItem(admin, getAwareItem(), BasicModel.simpleName, basicModel.id)
        BasicModel upd = BasicModel.get(basicModel.id)

        then:
        awareItem
        upd

        and:
        upd.edits.size() == 1
        upd.edits[0].description == "[${getAwareItem().editLabel}] removed from component [BasicModel:dm1]"
    }
}
