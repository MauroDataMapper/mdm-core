package uk.ac.ox.softeng.maurodatamapper.test.unit.core


import uk.ac.ox.softeng.maurodatamapper.core.facet.BreadcrumbTree
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem

import groovy.util.logging.Slf4j
import org.spockframework.util.InternalSpockError

/**
 * @since 21/09/2017
 */
@Slf4j
abstract class ModelItemSpec<K extends ModelItem> extends CatalogueItemSpec<K> {

    abstract Model getOwningModel()

    abstract String getModelFieldName()

    abstract void wipeModel()

    abstract void setModel(K domain, Model model)

    @Override
    void setValidDomainValues() {
        super.setValidDomainValues()
        setModel(domain, getOwningModel())
    }

    def setup() {
        log.debug('Setting up ModelItemSpec unit')
        mockDomains(BreadcrumbTree)
    }

    int getExpectedBaseConstrainedErrorCount() {
        0
    }

    void 'MI01 : test constrained properties'() {
        given:
        setValidDomainValues()
        wipeModel()
        int expectedErrors = modelFieldName ? 2 : 1
        expectedErrors += expectedBaseConstrainedErrorCount

        when:
        check(domain)

        then:
        thrown(InternalSpockError)
        domain.hasErrors()
        domain.errors.errorCount == expectedErrors

        and:
        if (modelFieldName) {
            domain.errors.getFieldError(modelFieldName).code == 'nullable'
        }

        when:
        setModel(domain, getOwningModel())

        then:
        check(domain)
    }

    void 'MI02 : test depth and path'() {
        when:
        setValidDomainValues()
        checkAndSave(domain)
        item = findById()

        then:
        item.depth == domain.depth
        item.path == domain.path

    }
}
