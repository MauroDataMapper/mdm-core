package uk.ac.ox.softeng.maurodatamapper.gorm.constraint.validator


import org.grails.datastore.gorm.GormEntity

/**
 * @since 27/02/2020
 */
abstract class UniqueStringValidator<D extends GormEntity> implements Validator<String> {

    D object

    UniqueStringValidator(D object) {
        this.object = object
    }

    @Override
    Object isValid(String value) {
        if (value == null) return ['default.null.message']
        if (!value) return ['default.blank.message']

        // No id so validation will be performed at collection level
        if (objectIsNotSaved()) return true

        valueIsNotUnique(value) ? ['default.not.unique.message'] : true
    }

    boolean objectIsNotSaved() {
        !object.ident()
    }

    abstract boolean valueIsNotUnique(String value)
}
