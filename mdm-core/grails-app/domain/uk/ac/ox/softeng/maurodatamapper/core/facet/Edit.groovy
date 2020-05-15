package uk.ac.ox.softeng.maurodatamapper.core.facet


import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CreatorAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.traits.domain.CreatorAware

class Edit implements CreatorAware {

    UUID id
    String description
    String resourceDomainType
    UUID resourceId

    static constraints = {
        CallableConstraints.call(CreatorAwareConstraints, delegate)
        resourceDomainType nullable: false, blank: false
        resourceId nullable: false
    }

    Edit() {
    }

    @Override
    String getDomainType() {
        Edit.simpleName
    }


    @SuppressWarnings("UnnecessaryQualifiedReference")
    static List<Edit> findAllByResource(String resourceDomainType, UUID resourceId, Map pagination = [:]) {
        Edit.findAllByResourceDomainTypeAndResourceId(resourceDomainType, resourceId, pagination)
    }
}