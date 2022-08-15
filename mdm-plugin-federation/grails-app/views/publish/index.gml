import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.federation.PublishedModel
import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter

import java.time.OffsetDateTime

model {
    Authority authority
    Iterable<PublishedModel> publishedModels
}

Authority auth = authority as Authority
Iterable<PublishedModel> pml = publishedModels as Iterable<PublishedModel>

xmlDeclaration()

index {
    authority {
        label auth.label
        url auth.url
    }
    'lastUpdated'(OffsetDateTimeConverter.toString(pml?.max {it.lastUpdated}?.lastUpdated ?: OffsetDateTime.now()))
    publishedModels {
        pml.each {publishedModel ->
            layout '/publishedModel/_publishedModel.gml', publishedModel: publishedModel
        }
    }
}