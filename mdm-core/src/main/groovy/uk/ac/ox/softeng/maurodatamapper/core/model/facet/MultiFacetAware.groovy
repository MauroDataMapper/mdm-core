package uk.ac.ox.softeng.maurodatamapper.core.model.facet

import grails.compiler.GrailsCompileStatic
import groovy.transform.SelfType
import org.grails.datastore.gorm.GormEntity

@SelfType(GormEntity)
@GrailsCompileStatic
trait MultiFacetAware implements MetadataAware,
    AnnotationAware,
    SemanticLinkAware,
    ReferenceFileAware {
}