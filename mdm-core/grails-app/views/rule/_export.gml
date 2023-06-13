import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter

model {
    Rule rule
}
Rule export = rule as Rule

'mdm:rule' {
    if (export.id) 'mdm:id' export.id
    if (export.createdBy) 'mdm:createdBy' export.createdBy
    if (export.lastUpdated) 'mdm:lastUpdated' OffsetDateTimeConverter.toString(export.lastUpdated)
    'mdm:name' export.name
    if (export.description) 'mdm:description' export.description

    if (export.ruleRepresentations) {
        'mdm:ruleRepresentations' {
            export.ruleRepresentations.each {representation ->
                layout '/ruleRepresentation/_export.gml', ruleRepresentation: representation
            }
        }
    }
}