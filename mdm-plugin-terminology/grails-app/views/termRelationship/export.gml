import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationship

TermRelationship export = termRelationship as TermRelationship

'mdm:termRelationship' {
    layout '/catalogueItem/_export.gml', catalogueItem: export
    'mdm:relationshipType' {yield export.relationshipType.label}
    'mdm:sourceTerm' {yield export.sourceTerm.code}
    'mdm:targetTerm' {yield export.targetTerm.code}
}
