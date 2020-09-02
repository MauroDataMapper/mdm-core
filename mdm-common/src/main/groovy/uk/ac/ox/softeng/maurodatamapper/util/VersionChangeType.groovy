package uk.ac.ox.softeng.maurodatamapper.util;

enum VersionChangeType {
    MAJOR('Major'),
    MINOR('Minor'),
    PATCH('Patch')

    String label

    VersionChangeType(String label) {
        this.label = label
    }

    static VersionChangeType findForLabel(String label) {
        String convert = label?.toUpperCase()?.replaceAll(/ /, '_')
        try {
            return valueOf(convert)
        } catch (Exception ignored) {}
        null
    }

    static VersionChangeType findFromMap(def map) {
        map['versionUpgradeType'] instanceof VersionChangeType ? map['versionUpgradeType'] as VersionChangeType : findForLabel(map['versionUpgradeType'] as String)
    }
}
