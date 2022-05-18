package uk.ac.ox.softeng.maurodatamapper.federation

enum SubscribedCatalogueType {
    MAURO_JSON('Mauro JSON'),
    ATOM('Atom')

    String label

    SubscribedCatalogueType(String label) {
        this.label = label
    }

    static SubscribedCatalogueType findForLabel(String label) {
        String convert = label?.toUpperCase()?.replaceAll(/ /, '_')
        try {
            return valueOf(convert)
        } catch (Exception ignored) {}
        null
    }

    static SubscribedCatalogueType findFromMap(def map) {
        map['subscribedCatalogueType'] instanceof SubscribedCatalogueType ? map['subscribedCatalogueType'] as SubscribedCatalogueType :
                                                                            findForLabel(map['subscribedCatalogueType'] as String)
    }
}