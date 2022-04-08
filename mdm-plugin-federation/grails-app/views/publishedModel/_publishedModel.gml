import uk.ac.ox.softeng.maurodatamapper.federation.PublishedModel

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
    lastUpdated pm.lastUpdated
    dateCreated pm.dateCreated
    datePublished pm.datePublished
    if (pm.author) author pm.author
    if (pm.description) description pm.description
    if (pm.previousModelId) previousModelId pm.previousModelId
    links {
        if (pm.links) {
            links.each {link ->
                layout '/link/_link.gml', link: link
            }
        }
    }
}