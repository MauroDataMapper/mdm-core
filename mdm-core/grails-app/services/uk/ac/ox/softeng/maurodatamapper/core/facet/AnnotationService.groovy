package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItemService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.CatalogueItemAwareService
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.InvalidDataAccessResourceUsageException

import javax.transaction.Transactional

@Slf4j
@Transactional
class AnnotationService implements CatalogueItemAwareService<Annotation> {

    @Autowired(required = false)
    List<CatalogueItemService> catalogueItemServices

    Annotation get(Serializable id) {
        Annotation.get(id)
    }

    List<Annotation> list(Map args) {
        Annotation.list(args)
    }

    Long count() {
        Annotation.count()
    }

    void delete(UUID id) {
        delete(get(id))
    }

    void delete(Annotation annotation) {
        if (!annotation) return
        CatalogueItemService service = catalogueItemServices.find {it.handles(annotation.catalogueItemDomainType)}
        if (!service) throw new ApiBadRequestException('AS01', 'Annotation removal for catalogue item with no supporting service')
        service.removeAnnotationFromCatalogueItem(annotation.catalogueItemId, annotation)

        annotation.parentAnnotation?.removeFromChildAnnotations(annotation)
        List<Annotation> children = new ArrayList<>(annotation.childAnnotations)
        children.each {
            delete(it)
        }
        annotation.delete()
    }

    Annotation editInformation(Annotation annotation, String label, String description) {
        annotation.label = label
        annotation.description = description
        annotation.validate()
        annotation
    }

    @Override
    Annotation findByCatalogueItemIdAndId(UUID catalogueItemId, Serializable id) {
        Annotation.byCatalogueItemIdAndId(catalogueItemId, id).get()
    }

    @Override
    List<Annotation> findAllByCatalogueItemId(UUID catalogueItemId, Map pagination = [:]) {
        Annotation.byCatalogueItemId(catalogueItemId).list(pagination)
    }

    List<Annotation> findAllWhereRootAnnotationOfCatalogueItemId(Serializable catalogueItemId, Map paginate = [:]) {
        Annotation.whereRootAnnotationOfCatalogueItemId(catalogueItemId).list(paginate)
    }

    List<Annotation> findAllByParentAnnotationId(Serializable parentAnnotationId, Map paginate = [:]) {
        try {
            return Annotation.byParentAnnotationId(parentAnnotationId).list(paginate)
        } catch (InvalidDataAccessResourceUsageException ignored) {
            log.warn('InvalidDataAccessResourceUsageException thrown, attempting query directly on parentAnnotation')
            return new DetachedCriteria<Annotation>(Annotation).eq('parentAnnotation', Utils.toUuid(parentAnnotationId)).list(paginate)
        }
    }

    Number countWhereRootAnnotationOfCatalogueItemId(Serializable catalogueItemId) {
        Annotation.whereRootAnnotationOfCatalogueItemId(catalogueItemId).count()
    }

}