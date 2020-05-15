package uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable

/**
 * @since 17/02/2020
 */
class InformationAwareConstraints {

    static constraints = {
        label nullable: false, blank: false
        description nullable: true, blank: false
    }
}
