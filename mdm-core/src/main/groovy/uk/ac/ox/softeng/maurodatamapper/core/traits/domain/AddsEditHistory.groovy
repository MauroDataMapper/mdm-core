package uk.ac.ox.softeng.maurodatamapper.core.traits.domain

import uk.ac.ox.softeng.maurodatamapper.security.User

import groovy.transform.CompileStatic

/**
 * @since 01/12/2017
 */
@CompileStatic
trait AddsEditHistory {

    public static final List<String> DIRTY_PROPERTY_NAMES_TO_IGNORE = ['version', 'lastUpdated']

    abstract void addCreatedEdit(User creator)

    abstract void addUpdatedEdit(User editor)

    abstract void addDeletedEdit(User deleter)

    abstract String getEditLabel()

    boolean shouldAddEdit(List<String> dirtyPropertyNames) {
        editedPropertyNames(dirtyPropertyNames)
    }

    List<String> editedPropertyNames(List<String> dirtyPropertyNames) {
        dirtyPropertyNames - DIRTY_PROPERTY_NAMES_TO_IGNORE
    }
}