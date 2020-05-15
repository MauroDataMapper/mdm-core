package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.interceptor.FacetInterceptor


class ReferenceFileInterceptor extends FacetInterceptor {


    boolean before() {
        facetResourceChecks()
        checkActionAllowedOnFacet()
    }
}
