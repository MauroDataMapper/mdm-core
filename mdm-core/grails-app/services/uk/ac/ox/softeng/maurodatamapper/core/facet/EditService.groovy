package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.facet.Edit
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource

@Slf4j
class EditService {

    MessageSource messageSource
    @Autowired(required = false)
    List<ModelService> modelServices

    Edit get(Serializable id) {
        Edit.get(Utils.toUuid(id))
    }

    List<Edit> list(Map pagination) {
        Edit.list(pagination)
    }

    Long count() {
        Edit.count()
    }

    Edit save(Edit edit) {
        edit.save(flush: true)
    }

    List<Edit> findAllByResource(String resourceDomainType, UUID resourceId, Map pagination = [sort: 'dateCreated', order: 'asc']) {
        Edit.findAllByResource(resourceDomainType, resourceId, pagination)
    }

    void createAndSaveEdit(UUID resourceId, String resourceDomainType, String description, User createdBy) {
        Edit edit = new Edit(resourceId: resourceId, resourceDomainType: resourceDomainType,
                             description: description, createdBy: createdBy.emailAddress)
        if (edit.validate()) {
            edit.save(flush: true, validate: false)
        } else {
            throw new ApiInvalidModelException('ES01', 'Created Edit is invalid', edit.errors, messageSource)
        }
    }
}