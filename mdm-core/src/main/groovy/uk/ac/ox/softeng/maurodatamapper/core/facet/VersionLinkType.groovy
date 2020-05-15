package uk.ac.ox.softeng.maurodatamapper.core.facet

/**
 * @since 12/02/2018
 */
enum VersionLinkType {
    SUPERSEDED_BY_MODEL('Superseded By Model'),
    NEW_MODEL_VERSION_OF('New Model Version Of'),
    SUPERSEDED_BY_DOCUMENTATION('Superseded By Documentation'),
    NEW_DOCUMENTATION_VERSION_OF('New Documentation Version Of')

    String label

    VersionLinkType(String label) {
        this.label = label
    }

    static VersionLinkType findForLabel(String label) {
        String convert = label?.toUpperCase()?.replaceAll(/ /, '_')
        try {
            return valueOf(convert)
        } catch (Exception ignored) {}
        null
    }

    static VersionLinkType findFromMap(def map) {
        map['linkType'] instanceof VersionLinkType ? map['linkType'] as VersionLinkType : findForLabel(map['linkType'] as String)
    }
}