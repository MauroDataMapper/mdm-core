package uk.ac.ox.softeng.maurodatamapper.core.diff

abstract class Diff<T> {

    T left
    T right

    Diff<T> leftHandSide(T lhs) {
        this.left = lhs
        this
    }

    Diff<T> rightHandSide(T rhs) {
        this.right = rhs
        this
    }

    boolean objectsAreIdentical() {
        !getNumberOfDiffs()
    }

    abstract Integer getNumberOfDiffs()
}
