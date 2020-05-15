package uk.ac.ox.softeng.maurodatamapper.core.model.facet

import uk.ac.ox.softeng.maurodatamapper.util.Utils

/**
 * @since 06/02/2020
 */
class Breadcrumb {

    UUID id
    String domainType
    String label
    Boolean finalised

    Breadcrumb(UUID id, String domainType, String label, Boolean finalised) {
        this.id = id
        this.domainType = domainType
        this.label = label
        this.finalised = finalised
    }

    Breadcrumb(String info) {
        this(info.split(/\|/))
    }

    Breadcrumb(String[] list) {
        id = Utils.toUuid(list[0])
        domainType = list[1]
        label = list[2]
        finalised = list[3] == 'null' ? null : list[3].toBoolean()
    }

    List<String> toList() {
        finalised != null ? [id.toString(), domainType, label, finalised?.toString()] : [id.toString(), domainType, label, null]
    }

    @Override
    String toString() {
        toList().join('|')
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        Breadcrumb that = (Breadcrumb) o

        if (domainType != that.domainType) return false
        if (finalised != that.finalised) return false
        if (id != that.id) return false
        if (label != that.label) return false

        return true
    }

    int hashCode() {
        int result
        result = id.hashCode()
        result = 31 * result + domainType.hashCode()
        result = 31 * result + label.hashCode()
        result = 31 * result + (finalised != null ? finalised.hashCode() : 0)
        return result
    }
}
