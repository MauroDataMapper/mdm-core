package uk.ac.ox.softeng.maurodatamapper.core.facet


import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItemService
import uk.ac.ox.softeng.maurodatamapper.core.model.file.CatalogueFileService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.CatalogueItemAwareService
import uk.ac.ox.softeng.maurodatamapper.security.User

import org.springframework.beans.factory.annotation.Autowired

class ReferenceFileService implements CatalogueFileService<ReferenceFile>, CatalogueItemAwareService<ReferenceFile> {

    @Autowired(required = false)
    List<CatalogueItemService> catalogueItemServices

    ReferenceFile get(Serializable id) {
        ReferenceFile.get(id)
    }

    List<ReferenceFile> list(Map args) {
        ReferenceFile.list(args)
    }

    Long count() {
        ReferenceFile.count()
    }

    void delete(Serializable id) {
        delete(get(id))
    }

    void delete(ReferenceFile file) {
        if (!file) return
        CatalogueItemService service = catalogueItemServices.find {it.handles(file.catalogueItemDomainType)}
        if (!service) throw new ApiBadRequestException('RFS01', 'Reference File removal for catalogue item with no supporting service')
        service.removeReferenceFileFromCatalogueItem(file.catalogueItemId, file)
        file.delete()
    }

    @Override
    ReferenceFile createNewFile(String name, byte[] contents, String type, User user) {
        createNewFileBase(name, contents, type, user.emailAddress)
    }

    @Override
    ReferenceFile resizeImage(ReferenceFile catalogueFile, int size) {
        ReferenceFile referenceFile = resizeImageBase(catalogueFile, size)
        referenceFile.catalogueItemDomainType = catalogueFile.catalogueItemDomainType
        referenceFile.catalogueItemId = catalogueFile.catalogueItemId
        referenceFile
    }

    @Override
    ReferenceFile findByCatalogueItemIdAndId(UUID catalogueItemId, Serializable id) {
        ReferenceFile.byCatalogueItemIdAndId(catalogueItemId, id).get()
    }

    @Override
    List<ReferenceFile> findAllByCatalogueItemId(UUID catalogueItemId, Map pagination) {
        ReferenceFile.byCatalogueItemId(catalogueItemId).list(pagination)
    }
}