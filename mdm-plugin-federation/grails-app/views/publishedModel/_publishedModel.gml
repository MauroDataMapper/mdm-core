import uk.ac.ox.softeng.maurodatamapper.federation.PublishedModel
import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter

model {
    PublishedModel publishedModel
}

PublishedModel pm = publishedModel as PublishedModel

publishedModel {
    modelId pm.modelId
    title pm.title
    label pm.modelLabel
    version pm.modelVersion
    modelType pm.modelType
    lastUpdated OffsetDateTimeConverter.toString(pm.lastUpdated)
    dateCreated OffsetDateTimeConverter.toString(pm.dateCreated)
    datePublished OffsetDateTimeConverter.toString(pm.datePublished)
    if (pm.author) author pm.author
    if (pm.description) description pm.description
    if (pm.previousModelId) previousModelId pm.previousModelId
    links {
        pm.links.each {link ->
            layout '/link/_link.gml', link: link
        }
    }
}