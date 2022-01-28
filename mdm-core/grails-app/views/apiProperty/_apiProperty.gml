import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiProperty
model {
    ApiProperty apiProperty
}
ApiProperty ap = apiProperty as ApiProperty
apiProperty {
    id ap.id
    key ap.key
    value ap.value
    if (ap.category) category ap.category
    publiclyVisible ap.publiclyVisible
    lastUpdatedBy ap.lastUpdatedBy
    createdBy ap.createdBy
    lastUpdated ap.lastUpdated
}