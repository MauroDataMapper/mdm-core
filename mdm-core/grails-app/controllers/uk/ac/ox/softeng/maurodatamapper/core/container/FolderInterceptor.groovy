package uk.ac.ox.softeng.maurodatamapper.core.container


import uk.ac.ox.softeng.maurodatamapper.core.interceptor.SecurableResourceInterceptor
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.util.Utils

class FolderInterceptor extends SecurableResourceInterceptor {

    @Override
    def <S extends SecurableResource> Class<S> getSecuredClass() {
        Folder as Class<S>
    }

    @Override
    void checkIds() {
        Utils.toUuid(params, 'id')
        Utils.toUuid(params, 'folderId')
    }

    @Override
    UUID getId() {
        params.folderId ?: params.id
    }

    boolean before() {
        securableResourceChecks()
        checkActionAuthorisationOnSecuredResource(Folder, getId(), true)
    }
}
