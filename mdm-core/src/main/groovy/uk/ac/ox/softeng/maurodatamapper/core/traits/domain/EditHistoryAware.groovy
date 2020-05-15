package uk.ac.ox.softeng.maurodatamapper.core.traits.domain

import uk.ac.ox.softeng.maurodatamapper.core.facet.Edit
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.traits.domain.CreatorAware

import groovy.transform.CompileStatic
import groovy.transform.SelfType
import org.grails.datastore.gorm.GormEntity

/**
 * @since 26/09/2017
 */
@SelfType(GormEntity)
@CompileStatic
trait EditHistoryAware extends AddsEditHistory implements CreatorAware {

    void addToEditsTransactionally(User createdBy, String description) {
        createAndSaveEditInsideNewTransaction createdBy, description
    }

    void addToEditsTransactionally(User createdBy, String editLabel, List<String> dirtyPropertyNames) {
        if (shouldAddEdit(dirtyPropertyNames)) {
            createAndSaveEditInsideNewTransaction createdBy, "[$editLabel] changed properties ${editedPropertyNames(dirtyPropertyNames)}"
        }
    }

    void createAndSaveEditInsideNewTransaction(User createdBy, String description) {
        Edit edit = null
        Edit.withNewTransaction {
            edit = new Edit(createdBy: createdBy.emailAddress,
                            description: description,
                            resourceId: id,
                            resourceDomainType: domainType).save(validate: false)
        }
        if (edit) {
            edit.skipValidation(true)
            skipValidation(true)
        }
    }

    @Override
    void addCreatedEdit(User creator) {
        addToEditsTransactionally creator, "{$editLabel} created".toString()
    }

    @Override
    void addUpdatedEdit(User editor) {
        addToEditsTransactionally editor, editLabel, dirtyPropertyNames
    }

    @Override
    void addDeletedEdit(User deleter) {
        // No-op
    }

    List<Edit> getEdits() {
        Edit.findAllByResource(domainType, id)
    }

}
