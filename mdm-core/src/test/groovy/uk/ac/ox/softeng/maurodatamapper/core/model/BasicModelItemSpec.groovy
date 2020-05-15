package uk.ac.ox.softeng.maurodatamapper.core.model

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModelItem
import uk.ac.ox.softeng.maurodatamapper.test.unit.core.ModelItemSpec

import grails.testing.gorm.DomainUnitTest
import groovy.util.logging.Slf4j

import java.time.OffsetDateTime

/**
 * @since 17/02/2020
 */
@Slf4j
class BasicModelItemSpec extends ModelItemSpec<BasicModelItem> implements DomainUnitTest<BasicModelItem> {

    def setup() {
        log.debug('Setting up BasicModelItem unit')
        mockDomains(BasicModel)
        checkAndSave(new BasicModel(label: 'test model', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue')))
    }

    @Override
    void wipeModel() {
        domain.model = null
        domain.breadcrumbTree = null
    }

    @Override
    void setModel(BasicModelItem domain, Model model) {
        domain.model = model as BasicModel
    }

    @Override
    void setValidDomainOtherValues() {
        domain.dateCreated = OffsetDateTime.now()
        domain.lastUpdated = OffsetDateTime.now()
    }

    @Override
    void verifyDomainOtherConstraints(BasicModelItem domain) {
    }

    @Override
    BasicModelItem createValidDomain(String label) {
        new BasicModelItem(label: label, model: BasicModel.findByLabel('test model'), createdBy: editor.emailAddress)
    }

    @Override
    Model getOwningModel() {
        BasicModel.findByLabel('test model')
    }

    @Override
    String getModelFieldName() {
        'model'
    }
}
