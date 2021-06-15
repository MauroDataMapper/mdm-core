package uk.ac.ox.softeng.maurodatamapper.core.diff

import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ArrayDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.FieldDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.MergeDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.unidirectional.CreationDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.unidirectional.DeletionDiff

import groovy.transform.CompileStatic

@CompileStatic
class DiffBuilder {

    static <K> ArrayDiff<K> arrayDiff(Class<K> arrayClass) {
        new ArrayDiff<K>(arrayClass as Class<Collection<K>>)
    }

    static <F> FieldDiff<F> fieldDiff(Class<F> fieldClass) {
        new FieldDiff<F>(fieldClass)
    }

    static <K extends Diffable> ObjectDiff<K> objectDiff(Class<K> objectClass) {
        new ObjectDiff<K>(objectClass)
    }

    static <K extends Diffable> MergeDiff<K> mergeDiff(Class<K> objectClass) {
        new MergeDiff<K>(objectClass)
    }

    static <K extends Diffable> CreationDiff<K> creationDiff(Class<K> objectClass) {
        new CreationDiff<K>(objectClass)
    }

    static <K extends Diffable> DeletionDiff<K> deletionDiff(Class<K> objectClass) {
        new DeletionDiff<K>(objectClass)
    }
}
