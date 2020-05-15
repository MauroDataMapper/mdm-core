package uk.ac.ox.softeng.maurodatamapper.core.facet

/**
 * @since 12/02/2018
 */
enum SemanticLinkType {
    REFINES('Refines'),
    DOES_NOT_REFINE('Does Not Refine'),
    ABSTRACTS('Abstracts'),
    DOES_NOT_ABSTRACT('Does Not Abstract')

    String label

    SemanticLinkType(String label) {
        this.label = label
    }

    static SemanticLinkType findForLabel(String label) {
        String convert = label?.toUpperCase()?.replaceAll(/ /, '_')
        try {
            return valueOf(convert)
        } catch (Exception ignored) {}
        null
    }

    static SemanticLinkType findFromMap(def map) {
        map['linkType'] instanceof SemanticLinkType ? map['linkType'] as SemanticLinkType : findForLabel(map['linkType'] as String)
    }
}