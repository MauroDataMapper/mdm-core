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

    static <K extends Diffable> ArrayDiff<K> arrayDiff(Class<K> arrayClass) {
        new ArrayDiff<K>(arrayClass as Class<Collection<K>>)
    }

    static <K extends Diffable> ArrayDiff<K> arrayDiff(ArrayDiff<K> original) {
        arrayDiff(original.targetClass.arrayType() as Class<K>)
            .fieldName(original.fieldName)
            .leftHandSide(original.left)
            .rightHandSide(original.right)
    }

    static <F> FieldDiff<F> fieldDiff(Class<F> fieldClass) {
        new FieldDiff<F>(fieldClass)
    }

    static <F> FieldDiff<F> fieldDiff(FieldDiff<F> original) {
        fieldDiff(original.targetClass)
            .fieldName(original.fieldName)
            .leftHandSide(original.left)
            .rightHandSide(original.right)
    }

    static <K extends Diffable> ObjectDiff<K> objectDiff(Class<K> objectClass) {
        new ObjectDiff<K>(objectClass)
    }

    static <K extends Diffable> MergeDiff<K> mergeDiff(Class<K> objectClass) {
        new MergeDiff<K>(objectClass)
    }

    static <K extends Diffable> MergeDiff<K> mergeDiff(ObjectDiff<K> objectDiff, K commonAncestor) {
        mergeDiff(objectDiff.targetClass)
            .leftHandSide(objectDiff.left)
            .rightHandSide(objectDiff.right)
            .commonAncestor(commonAncestor)
    }

    static <K extends Diffable> CreationDiff<K> creationDiff(Class<K> objectClass) {
        new CreationDiff<K>(objectClass)
    }

    static <K extends Diffable> DeletionDiff<K> deletionDiff(Class<K> objectClass) {
        new DeletionDiff<K>(objectClass)
    }
}
