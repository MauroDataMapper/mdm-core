package uk.ac.ox.softeng.maurodatamapper.testing.functional.core.facet.annotation

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.testing.functional.facet.ContainerAnnotationFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.testing.functional.facet.CatalogueItemAnnotationFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

/**
 * <pre>
 *  Controller: annotation
 *  |  POST    | /api/${containerDomainType}/${containerId}/annotations        | Action: save
 *  |  GET     | /api/${containerDomainType}/${containerId}/annotations        | Action: index
 *  |  DELETE  | /api/${containerDomainType}/${containerId}/annotations/${id}  | Action: delete
 *  |  GET     | /api/${containerDomainType}/${containerId}/annotations/${id}  | Action: show
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.AnnotationController
 */
@Integration
@Slf4j
class ClassifierAnnotationFunctionalSpec extends ContainerAnnotationFunctionalSpec {

    @Transactional
    @Override
    String getContainerId() {
        Classifier.findByLabel('Functional Test Classifier').id.toString()
    }

    @Override
    String getContainerDomainType() {
        'classifiers'
    }

}