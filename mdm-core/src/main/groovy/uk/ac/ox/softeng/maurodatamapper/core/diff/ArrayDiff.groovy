package uk.ac.ox.softeng.maurodatamapper.core.diff

class ArrayDiff<T extends Diffable> extends FieldDiff<Collection<T>> {

    Collection<T> created
    Collection<T> deleted
    Collection<ObjectDiff<T>> modified

    private ArrayDiff() {
        created = []
        deleted = []
        modified = []
    }

    ArrayDiff<T> created(Collection<T> created) {
        this.created = created
        this
    }

    ArrayDiff<T> deleted(Collection<T> deleted) {
        this.deleted = deleted
        this
    }

    ArrayDiff<T> modified(Collection<ObjectDiff<T>> modified) {
        this.modified = modified
        this
    }

    @Override
    ArrayDiff<T> fieldName(String fieldName) {
        super.fieldName(fieldName) as ArrayDiff<T>
    }

    @Override
    ArrayDiff<T> leftHandSide(Collection<T> lhs) {
        super.leftHandSide(lhs) as ArrayDiff<T>
    }

    @Override
    ArrayDiff<T> rightHandSide(Collection<T> rhs) {
        super.rightHandSide(rhs) as ArrayDiff<T>
    }

    @Override
    Integer getNumberOfDiffs() {
        created.size() + deleted.size() + ((modified.sum {it.getNumberOfDiffs()} ?: 0) as Integer)
    }

    @Override
    String toString() {
        StringBuilder stringBuilder = new StringBuilder(super.toString())

        if (created) {
            stringBuilder.append('\n  Created ::\n').append(created)
        }
        if (deleted) {
            stringBuilder.append('\n  Deleted ::\n').append(deleted)
        }
        if (modified) {
            stringBuilder.append('\n  Modified ::\n').append(modified)
        }
        stringBuilder.toString()
    }

    static <K extends Diffable> ArrayDiff<K> builder(Class<K> arrayClass) {
        new ArrayDiff<K>()
    }
}
