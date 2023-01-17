import uk.ac.ox.softeng.maurodatamapper.federation.PublishedModel
import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter

model {
    PublishedModel publishedModel
}

PublishedModel pm = publishedModel as PublishedModel

publishedModel {
    modelId pm.modelId
    label pm.modelLabel
    version pm.modelVersion
    if (pm.modelVersionTag) modelVersionTag pm.modelVersionTag
    if (pm.modelType) modelType pm.modelType
    lastUpdated OffsetDateTimeConverter.toString(pm.lastUpdated)
    if (pm.dateCreated) dateCreated OffsetDateTimeConverter.toString(pm.dateCreated)
    if (pm.datePublished) datePublished OffsetDateTimeConverter.toString(pm.datePublished)
    if (pm.author) author pm.author
    if (pm.description) description pm.description
    links {
        pm.links.each {link ->
            layout '/link/_link.gml', link: link
        }
    }
}