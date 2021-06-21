package uk.ac.ox.softeng.maurodatamapper.core.diff

import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ArrayDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.FieldDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional.ArrayMergeDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional.CreationMergeDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional.DeletionMergeDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional.FieldMergeDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional.MergeDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.unidirectional.CreationDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.unidirectional.DeletionDiff

import groovy.transform.CompileStatic

@CompileStatic
class DiffBuilder {

    static <K extends Diffable> ArrayDiff<K> arrayDiff(Class<Collection<K>> arrayClass) {
        new ArrayDiff<K>(arrayClass)
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

    static <K extends Diffable> ArrayMergeDiff<K> arrayMergeDiff(Class<Collection<K>> arrayClass) {
        new ArrayMergeDiff<K>(arrayClass)
    }

    static <F> FieldMergeDiff<F> fieldMergeDiff(Class<F> fieldClass) {
        new FieldMergeDiff<F>(fieldClass)
    }

    static <K extends Diffable> CreationMergeDiff<K> creationMergeDiff(Class<K> objectClass) {
        new CreationMergeDiff<K>(objectClass)
    }

    static <K extends Diffable> DeletionMergeDiff<K> deletionMergeDiff(Class<K> objectClass) {
        new DeletionMergeDiff<K>(objectClass)
    }
}
