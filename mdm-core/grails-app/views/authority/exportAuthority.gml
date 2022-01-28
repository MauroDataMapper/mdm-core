import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter

model {
    Authority authority
    String ns
}

Authority au = authority as Authority

'mdm:authority' {
    if (au.id) 'mdm:id'(au.id)
    'mdm:label' {yield au.label}
    'mdm:url' {yield au.url}
    if (au.lastUpdated) 'mdm:lastUpdated'(OffsetDateTimeConverter.toString(au.lastUpdated))
    if (au.description) 'mdm:description' {yield au.description}
}
