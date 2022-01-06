import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter

model {
    Metadata metadata
    String ns
}

Metadata md = metadata as Metadata

'mdm:metadata' {
    if (md.id) 'mdm:id'(md.id)
    'mdm:namespace' {yield md.namespace}
    'mdm:key' {yield md.key}
    'mdm:value' {yield md.value}
    if (md.lastUpdated) 'mdm:lastUpdated'(OffsetDateTimeConverter.toString(md.lastUpdated))
}
