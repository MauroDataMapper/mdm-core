import uk.ac.ox.softeng.maurodatamapper.federation.PublishedModel
import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter

import java.time.OffsetDateTime

model {
    Iterable<PublishedModel> newerPublishedModels
}

Iterable<PublishedModel> npm = newerPublishedModels as Iterable<PublishedModel>

xmlDeclaration()

newerVersions {
    'lastUpdated'(OffsetDateTimeConverter.toString(npm?.max {it.lastUpdated}?.lastUpdated ?: OffsetDateTime.now()))
    newerPublishedModels {
        npm.each {publishedModel ->
            layout '/publishedModel/_publishedModel.gml', publishedModel: publishedModel
        }
    }
}