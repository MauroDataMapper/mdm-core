package uk.ac.ox.softeng.maurodatamapper.core.diff

interface Diffable<T extends Diffable> {

    ObjectDiff<T> diff(T obj)

    String getDiffIdentifier()
}