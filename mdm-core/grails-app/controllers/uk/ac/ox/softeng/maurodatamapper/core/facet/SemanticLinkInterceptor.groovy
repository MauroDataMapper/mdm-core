package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.interceptor.FacetInterceptor

class SemanticLinkInterceptor extends FacetInterceptor {


    boolean before() {
        facetResourceChecks()
        checkActionAllowedOnFacet()
    }
}
