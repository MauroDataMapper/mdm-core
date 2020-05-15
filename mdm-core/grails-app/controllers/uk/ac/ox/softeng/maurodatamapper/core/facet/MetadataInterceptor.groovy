package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.interceptor.FacetInterceptor

class MetadataInterceptor extends FacetInterceptor {

    boolean before() {
        if (actionName == 'namespaces') return true

        facetResourceChecks()
        checkActionAllowedOnFacet()
    }
}
