package uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable

import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CreatorAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.validator.UniqueValuesValidator

/**
 * @since 17/02/2020
 */
class CatalogueItemConstraints extends CreatorAwareConstraints {

    static constraints = {
        CallableConstraints.call(InformationAwareConstraints, delegate)

        aliasesString nullable: true, blank: false
        metadata validator: {val, obj ->
            if (val) new UniqueValuesValidator('namespace:key').isValid(val.groupBy {"${it.namespace}:${it.key}"})
        }
    }
}
