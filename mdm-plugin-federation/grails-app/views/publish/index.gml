import uk.ac.ox.softeng.maurodatamapper.federation.PublishedModel

model {
    List<PublishedModel> publishedModels
}

List<PublishedModel> pml = publishedModels as List<PublishedModel>

publishedModels {
    pml.each {publishedModel ->
        layout '/publishedModel/_publishedModel.gml', publishedModel: publishedModel
    }
}