package uk.ac.ox.softeng.maurodatamapper.federation

enum SubscribedCatalogueType {
    MDM_JSON('mdm+json'),
    ATOM('atom')

    String type

    SubscribedCatalogueType(String type) {
        this.type = type
    }
}