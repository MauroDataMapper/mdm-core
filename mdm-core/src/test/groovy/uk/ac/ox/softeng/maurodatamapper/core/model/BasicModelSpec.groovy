package uk.ac.ox.softeng.maurodatamapper.core.model

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.test.unit.core.ModelSpec
import uk.ac.ox.softeng.maurodatamapper.util.Version

import grails.testing.gorm.DomainUnitTest
import groovy.util.logging.Slf4j

import java.time.OffsetDateTime

/**
 * @since 17/02/2020
 */
@Slf4j
class BasicModelSpec extends ModelSpec<BasicModel> implements DomainUnitTest<BasicModel> {

    @Override
    void setValidDomainOtherValues() {
        domain.dateCreated = OffsetDateTime.now()
        domain.lastUpdated = OffsetDateTime.now()
    }

    @Override
    void verifyDomainOtherConstraints(BasicModel domain) {
    }

    @Override
    BasicModel createValidDomain(String label) {
        new BasicModel(label: label, folder: Folder.findByLabel('catalogue'), createdBy: editor.emailAddress,
                       documentationVersion: Version.from('1'))
    }
}
