package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.importer

import io.micronaut.core.order.Ordered

/**
 * @since 22/01/2018
 */
class ImportParameter implements Comparable<ImportParameter>, Ordered {

    String name
    String type
    String description
    String displayName
    boolean optional = false
    int order

    @Override
    int compareTo(ImportParameter that) {
        int res = this.type == Boolean.simpleName && that.type != Boolean.simpleName ? 1 : 0
        if (res == 0) res = that.type == Boolean.simpleName && this.type != Boolean.simpleName ? -1 : 0
        if (res == 0) res = this.optional <=> that.optional
        if (res == 0) res = this.order <=> that.order
        if (res == 0) res = this.displayName <=> that.displayName
        res
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        ImportParameter that = (ImportParameter) o

        name == that.name
    }

    int hashCode() {
        name != null ? name.hashCode() : 0
    }

    String toString() {
        "$name:$order"
    }
}
