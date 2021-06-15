package uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional

import uk.ac.ox.softeng.maurodatamapper.core.diff.Diff

import groovy.transform.CompileStatic

@CompileStatic
abstract class BiDirectionalDiff<B> extends Diff<B> {

    B right

    protected BiDirectionalDiff(Class<B> targetClass) {
        super(targetClass)
    }

    @Override
    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        BiDirectionalDiff<B> diff = (BiDirectionalDiff<B>) o

        if (left != diff.left) return false
        if (right != diff.right) return false

        return true
    }

    BiDirectionalDiff<B> leftHandSide(B lhs) {
        this.left = lhs
        this
    }

    BiDirectionalDiff<B> rightHandSide(B rhs) {
        this.right = rhs
        this
    }

    void setLeft(B left) {
        this.value = left
    }

    B getLeft() {
        this.value
    }
}
