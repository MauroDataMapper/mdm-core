package uk.ac.ox.softeng.maurodatamapper.core.diff

import grails.compiler.GrailsCompileStatic

@GrailsCompileStatic
abstract class Mergeable<T> {

    Boolean isMergeConflict
    T commonAncestorValue
}