package uk.ac.ox.softeng.maurodatamapper.core.diff

class MergeWrapper<T extends Diffable> extends Mergeable {
    T value

    MergeWrapper(T value) {
        this.value = value
    }

    @Override
    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        MergeWrapper<T> diff = (MergeWrapper<T>) o

        if (value != diff.value) return false

        return true
    }

    @Override
    String toString() {
        value.toString()
    }
}
