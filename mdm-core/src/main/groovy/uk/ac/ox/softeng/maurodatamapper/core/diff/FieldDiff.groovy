package uk.ac.ox.softeng.maurodatamapper.core.diff

class FieldDiff<T> extends Diff<T> {

    String fieldName

    FieldDiff() {
    }

    FieldDiff<T> fieldName(String fieldName) {
        this.fieldName = fieldName
        this
    }

    @Override
    Integer getNumberOfDiffs() {
        1
    }

    @Override
    FieldDiff<T> leftHandSide(T lhs) {
        super.leftHandSide(lhs) as FieldDiff<T>
    }

    @Override
    FieldDiff<T> rightHandSide(T rhs) {
        super.rightHandSide(rhs) as FieldDiff<T>
    }

    @Override
    String toString() {
        "${fieldName} :: ${left?.toString()} <> ${right?.toString()}"
    }

    static <K> FieldDiff<K> builder(Class<K> fieldClass) {
        new FieldDiff<K>()
    }
}
