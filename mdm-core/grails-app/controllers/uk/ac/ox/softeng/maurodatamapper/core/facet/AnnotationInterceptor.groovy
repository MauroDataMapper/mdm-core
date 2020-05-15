package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.interceptor.FacetInterceptor
import uk.ac.ox.softeng.maurodatamapper.util.Utils

class AnnotationInterceptor extends FacetInterceptor {

    @Override
    void checkAdditionalIds() {
        Utils.toUuid(params, 'annotationId')
    }

    boolean before() {
        facetResourceChecks()
        checkActionAllowedOnFacet()
    }
}
