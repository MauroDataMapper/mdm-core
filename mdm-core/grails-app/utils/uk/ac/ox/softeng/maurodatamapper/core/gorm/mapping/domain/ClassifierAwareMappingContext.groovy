package uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.domain

import uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.JoinTableDynamicHibernateMappingContext
import uk.ac.ox.softeng.maurodatamapper.core.model.container.ClassifierAware
import uk.ac.ox.softeng.maurodatamapper.util.Utils

/**
 * @since 31/10/2019
 */
class ClassifierAwareMappingContext extends JoinTableDynamicHibernateMappingContext {
    @Override
    boolean handlesDomainClass(Class domainClass) {
        Utils.parentClassIsAssignableFromChild(ClassifierAware, domainClass)
    }

    @Override
    String getPropertyName() {
        'classifiers'
    }

    @Override
    String getCascade() {
        'none'
    }
}
