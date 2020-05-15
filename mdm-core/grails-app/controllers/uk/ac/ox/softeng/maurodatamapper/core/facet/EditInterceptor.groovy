package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.interceptor.FacetInterceptor

import groovy.util.logging.Slf4j

@Slf4j
class EditInterceptor extends FacetInterceptor {

    @Override
    String getOwningType() {
        'resource'
    }

    boolean before() {
        facetResourceChecks()
        checkActionAllowedOnFacet()
    }
}
