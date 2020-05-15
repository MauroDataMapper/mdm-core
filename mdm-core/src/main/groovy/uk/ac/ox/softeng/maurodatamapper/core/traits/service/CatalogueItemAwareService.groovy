package uk.ac.ox.softeng.maurodatamapper.core.traits.service

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItemService
import uk.ac.ox.softeng.maurodatamapper.security.User

/**
 * @since 31/01/2020
 */
trait CatalogueItemAwareService<K> {

    abstract List<CatalogueItemService> getCatalogueItemServices()

    abstract K findByCatalogueItemIdAndId(UUID catalogueItemId, Serializable id)

    abstract List<K> findAllByCatalogueItemId(UUID catalogueItemId, Map pagination)

    K addCreatedEditToCatalogueItem(User creator, K domain, String catalogueItemDomainType, UUID catalogueItemId) {
        CatalogueItem catalogueItem = findCatalogueItemByDomainTypeAndId(catalogueItemDomainType, catalogueItemId)
        catalogueItem.addToEditsTransactionally creator, "[$domain.editLabel] added to component [${catalogueItem.editLabel}]"
        domain
    }

    K addUpdatedEditToCatalogueItem(User editor, K domain, String catalogueItemDomainType, UUID catalogueItemId) {
        CatalogueItem catalogueItem = findCatalogueItemByDomainTypeAndId(catalogueItemDomainType, catalogueItemId)
        catalogueItem.addToEditsTransactionally editor, domain.editLabel, domain.dirtyPropertyNames
        domain
    }

    K addDeletedEditToCatalogueItem(User deleter, K domain, String catalogueItemDomainType, UUID catalogueItemId) {
        CatalogueItem catalogueItem = findCatalogueItemByDomainTypeAndId(catalogueItemDomainType, catalogueItemId)
        catalogueItem.addToEditsTransactionally deleter, "[$domain.editLabel] removed from component [${catalogueItem.editLabel}]"
        domain
    }


    CatalogueItem findCatalogueItemByDomainTypeAndId(String domainType, UUID catalogueItemId) {
        CatalogueItemService service = catalogueItemServices.find {it.handles(domainType)}
        if (!service) throw new ApiBadRequestException('CIAS02', "Metadata retrieval for catalogue item [${domainType}] with no supporting service")
        service.get(catalogueItemId)
    }
}
