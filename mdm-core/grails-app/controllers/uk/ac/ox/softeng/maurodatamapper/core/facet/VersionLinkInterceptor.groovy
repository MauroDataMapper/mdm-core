package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.interceptor.FacetInterceptor
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.core.GrailsClass

class VersionLinkInterceptor extends FacetInterceptor {


    @Override
    String getOwningType() {
        'model'
    }

    boolean before() {
        facetResourceChecks()

        if (!Utils.parentClassIsAssignableFromChild(Model, getOwningClass())) {
            throw new ApiBadRequestException('VLI01', "Domain class [${params.modelDomainType}] does not extend the Model class")
        }

        checkActionAllowedOnFacet()
    }
}