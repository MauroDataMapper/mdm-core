import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiProperty

model {
    ApiProperty apiProperty
}
ApiProperty ap = apiProperty as ApiProperty

xmlDeclaration()
layout 'apiProperty/_apiProperty.gml', apiProperty: ap