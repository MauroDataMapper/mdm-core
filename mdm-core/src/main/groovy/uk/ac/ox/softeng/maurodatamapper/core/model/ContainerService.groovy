package uk.ac.ox.softeng.maurodatamapper.core.model

import uk.ac.ox.softeng.maurodatamapper.security.SecurableResourceService
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager

/**
 * @since 16/01/2020
 */
interface ContainerService<K> extends SecurableResourceService<K> {

    boolean isContainerVirtual()

    String getContainerPropertyNameInModel()

    abstract List<K> findAllReadableContainersBySearchTerm(UserSecurityPolicyManager userSecurityPolicyManager, String searchTerm)

    List<K> findAllContainersInside(UUID containerId)

}