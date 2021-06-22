package uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional


import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.BiDirectionalDiff

import groovy.transform.CompileStatic

@CompileStatic
abstract class TriDirectionalDiff<T> extends BiDirectionalDiff<T> {

    private Boolean mergeConflict
    private T commonAncestor

    protected TriDirectionalDiff(Class<T> targetClass) {
        super(targetClass)
        mergeConflict = false
    }

    @Override
    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        TriDirectionalDiff<T> diff = (TriDirectionalDiff<T>) o

        if (left != diff.left) return false
        if (right != diff.right) return false

        return true
    }

    @SuppressWarnings('GrDeprecatedAPIUsage')
    TriDirectionalDiff<T> withSource(T source) {
        this.left = source
        this
    }

    @SuppressWarnings('GrDeprecatedAPIUsage')
    TriDirectionalDiff<T> withTarget(T target) {
        this.right = target
        this
    }

    TriDirectionalDiff<T> withCommonAncestor(T ca) {
        this.commonAncestor = ca
        this
    }

    TriDirectionalDiff<T> asMergeConflict() {
        this.mergeConflict = true
        this
    }

    Boolean isMergeConflict() {
        mergeConflict
    }

    T getCommonAncestor() {
        commonAncestor
    }

    T getTarget() {
        super.getRight()
    }

    T getSource() {
        super.getLeft()
    }

    @Deprecated
    @Override
    T getRight() {
        super.getRight()
    }

    @Deprecated
    @Override
    void setRight(T right) {
        super.setRight(right)
    }

    @Deprecated
    @Override
    BiDirectionalDiff<T> leftHandSide(T lhs) {
        super.leftHandSide(lhs)
    }

    @Deprecated
    @Override
    BiDirectionalDiff<T> rightHandSide(T rhs) {
        super.rightHandSide(rhs)
    }

    @Deprecated
    @Override
    void setLeft(T left) {
        super.setLeft(left)
    }

    @Deprecated
    @Override
    T getLeft() {
        super.getLeft()
    }

    @Override
    String toString() {
        "${source} --> ${target} [${commonAncestor}]"
    }
}
