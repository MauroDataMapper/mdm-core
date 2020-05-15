package uk.ac.ox.softeng.maurodatamapper.test.unit.service

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.BreadcrumbTree
import uk.ac.ox.softeng.maurodatamapper.core.facet.Edit
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity

/**
 * @since 03/04/2020
 */
@Slf4j
class CatalogueItemServiceSpec extends BaseUnitSpec {

    def setup() {
        log.debug('Setting up CatalogueItemServiceSpec unit')
        mockDomains(Classifier, Folder, Annotation, Edit, Metadata, ReferenceFile, SemanticLink, BreadcrumbTree)
        checkAndSave(new Folder(label: 'catalogue', createdBy: admin.emailAddress))
    }

    Folder getTestFolder() {
        Folder.findByLabel('catalogue')
    }

    @Override
    void checkAndSave(GormEntity domainObj) {
        super.checkAndSave(domainObj)
        // Second check and save to make sure breadcrumbs/facets are setup correctly
        super.checkAndSave(domainObj)
        currentSession.flush()
    }

    void verifyBreadcrumbTrees() {
        List<BreadcrumbTree> trees = BreadcrumbTree.list()
        trees.findAll {it.isDirty() && it.validate()}.each {it.save()}
        currentSession.flush()
        assert BreadcrumbTree.countByDomainIdIsNull() == 0
    }
}
