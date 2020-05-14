package uk.ac.ox.softeng.maurodatamapper.security

interface SecurableResourceService<K> {

    K get(Serializable id)

    boolean handles(Class clazz)

    boolean handles(String domainType)

    List<K> getAll(Collection<UUID> containerIds)

    List<K> list()

}
