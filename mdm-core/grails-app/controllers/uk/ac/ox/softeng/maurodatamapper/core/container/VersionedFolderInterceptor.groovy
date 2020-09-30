package uk.ac.ox.softeng.maurodatamapper.core.container

import uk.ac.ox.softeng.maurodatamapper.core.interceptor.SecurableResourceInterceptor
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.util.Utils


class VersionedFolderInterceptor extends SecurableResourceInterceptor {

    @Override
    def <S extends SecurableResource> Class<S> getSecuredClass() {
        VersionedFolder as Class<S>
    }

    @Override
    void checkIds() {
        Utils.toUuid(params, 'id')
        Utils.toUuid(params, 'folderId')
        Utils.toUuid(params, 'versionedFolderId')
    }

    @Override
    UUID getId() {
        params.id ?: params.versionedFolderId ?: params.folderId
    }

    boolean before() {
        securableResourceChecks()

        if (actionName == 'search') {
            return currentUserSecurityPolicyManager.userCanReadSecuredResourceId(VersionedFolder, getId()) ?:
                   notFound(VersionedFolder, getId())
        }

        checkActionAuthorisationOnSecuredResource(VersionedFolder, getId(), true)
    }
}
