import uk.ac.ox.softeng.maurodatamapper.core.facet.rule.RuleRepresentation
import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter

model {
    RuleRepresentation ruleRepresentation
}
RuleRepresentation export = ruleRepresentation as RuleRepresentation

'mdm:ruleRepresentation' {
    if (export.id) 'mdm:id' export.id
    if (export.language) 'mdm:language' export.language
    if (export.representation) 'mdm:representation' export.representation
    if (export.lastUpdated) 'mdm:lastUpdated' OffsetDateTimeConverter.toString(export.lastUpdated)
}