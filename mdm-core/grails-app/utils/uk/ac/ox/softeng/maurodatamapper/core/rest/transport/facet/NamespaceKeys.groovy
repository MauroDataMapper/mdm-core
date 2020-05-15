package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.facet

/**
 * @since 06/10/2017
 */
class NamespaceKeys implements Comparable<NamespaceKeys> {

    String namespace
    Boolean editable
    Collection<String> keys
    Boolean defaultNamespace = false

    @Override
    int compareTo(NamespaceKeys that) {
        this.namespace <=> that.namespace
    }


    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        NamespaceKeys that = (NamespaceKeys) o

        if (namespace != that.namespace) return false

        return true
    }
}
