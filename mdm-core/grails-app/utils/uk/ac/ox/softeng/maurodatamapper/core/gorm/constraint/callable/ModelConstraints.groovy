package uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable

import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.validator.ModelLabelValidator

/**
 * @since 17/02/2020
 */
class ModelConstraints extends CatalogueItemConstraints {

    static constraints = {
        folder nullable: false
        deleted nullable: false
        finalised nullable: false
        modelType nullable: false, blank: false
        documentationVersion nullable: false
        author nullable: true, blank: false
        organisation nullable: true, blank: false
        dateFinalised nullable: true

        label validator: {val, obj -> new ModelLabelValidator(obj).isValid(val)}
    }
}
