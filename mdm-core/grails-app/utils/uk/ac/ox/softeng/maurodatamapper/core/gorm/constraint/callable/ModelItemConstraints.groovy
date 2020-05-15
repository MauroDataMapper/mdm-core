package uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable

/**
 * @since 17/02/2020
 */
class ModelItemConstraints extends CatalogueItemConstraints {

    static constraints = {
        path nullable: false, blank: true
        depth nullable: false
        breadcrumbTree nullable: false
    }
}
