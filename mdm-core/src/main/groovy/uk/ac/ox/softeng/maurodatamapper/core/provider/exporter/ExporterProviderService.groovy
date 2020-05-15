package uk.ac.ox.softeng.maurodatamapper.core.provider.exporter


import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.core.provider.ProviderType
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.provider.MauroDataMapperService

/**
 * @since 16/11/2017
 */
trait ExporterProviderService extends MauroDataMapperService {

    abstract ByteArrayOutputStream exportDomain(User currentUser, UUID domainId) throws ApiException

    abstract ByteArrayOutputStream exportDomains(User currentUser, List<UUID> domainIds)
        throws ApiException

    abstract Boolean canExportMultipleDomains()

    abstract String getFileExtension()

    abstract String getFileType()

    @Override
    String getProviderType() {
        ProviderType.EXPORTER.name
    }
}