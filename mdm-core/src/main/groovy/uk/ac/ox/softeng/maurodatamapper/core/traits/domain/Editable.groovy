package uk.ac.ox.softeng.maurodatamapper.core.traits.domain

/**
 *
 * Implement the below sets by using grails hasMany
 *
 *
 * @deprecated ,replace with {@link uk.ac.ox.softeng.maurodatamapper.security.SecurableResource} and {@link EditHistoryAware}
 * @since 25/09/2017
 */
@Deprecated
trait Editable /*extends EditHistoryAware*/ {
    /*
    Boolean readableByEveryone = false
    Boolean readableByAuthenticated = false
    Boolean deleted = false

    abstract Set<UserGroup> getReadableByGroups()

    abstract void setReadableByGroups(Set<UserGroup> readableByGroups)

    abstract Set<UserGroup> getWriteableByGroups()

    abstract void setWriteableByGroups(Set<UserGroup> writeableByGroups)

    abstract Set<CatalogueUser> getReadableByUsers()

    abstract void setReadableByUsers(Set<CatalogueUser> readableByUsers)

    abstract Set<CatalogueUser> getWriteableByUsers()

    abstract void setWriteableByUsers(Set<CatalogueUser> writeableByUsers)

    abstract void createAndSaveEditInsideNewTransaction(CatalogueUser createdBy, String description)

    abstract List getDirtyPropertyNames()

    abstract CatalogueUser getOwner()

    abstract Boolean isOwnedBy(CatalogueUser catalogueUser)

    Boolean getDeleted() {
        deleted == null ? false : deleted
    }

    Set<CatalogueUser> getAllReadableByUsers() {
        SqlLogger.debug('Getting all editable readable users')
        Set<CatalogueUser> users = getAllWriteableByUsers()
        users += readableByUsers ?: []
        users
    }

    Set<CatalogueUser> getAllWriteableByUsers() {
        SqlLogger.debug('Getting all editable writeable users')
        Set<CatalogueUser> users = [] as HashSet
        users += writeableByUsers ?: []
        users += CatalogueUser.byAdministratorOrId(owner.id).list()
        users
    }



    @SuppressWarnings("GroovyAssignabilityCheck")
    static <T extends Editable> DetachedCriteria<T> withWriteableByUser(DetachedCriteria<T> criteria, CatalogueUser user,
                                                                        @DelegatesTo(AbstractDetachedCriteria) Closure additionalSecurity = null) {
        def groupIdsUserBelongsTo = UserGroup.byHasMember(user).id().list()
        if (!user?.isAdministrator()) criteria = withNotDeleted(criteria)
        criteria.or {
            eq 'createdBy', user
            inList 'id', {
                writeableByUsers {eq 'id', user.id}
                projections {property 'id'}
            }
            if (groupIdsUserBelongsTo) {
                inList 'id', {
                    writeableByGroups {inList 'id', groupIdsUserBelongsTo}
                    projections {property 'id'}
                }
            }
            if (additionalSecurity) {
                additionalSecurity.delegate = criteria
                additionalSecurity.call()
            }
        }
    }

    @Deprecated
    static <T extends Editable> DetachedCriteria<T> withReadableByUser(DetachedCriteria<T> criteria, CatalogueUser user) {
        withReadableByUser(criteria, user, false, null)
    }

    @Deprecated
    static <T extends Editable> DetachedCriteria<T> withReadableByUser(DetachedCriteria<T> criteria, CatalogueUser user,
                                                                       @DelegatesTo(AbstractDetachedCriteria) Closure additionalSecurity) {
        withReadableByUser(criteria, user, false, additionalSecurity)
    }

    @Deprecated
    static <T extends Editable> DetachedCriteria<T> withReadableByUser(DetachedCriteria<T> criteria, CatalogueUser user, Boolean includeDeleted) {
        withReadableByUser(criteria, user, includeDeleted, null)
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    @Deprecated
    static <T extends Editable> DetachedCriteria<T> withReadableByUser(DetachedCriteria<T> criteria, CatalogueUser user, Boolean includeDeleted,
                                                                       @DelegatesTo(AbstractDetachedCriteria) Closure additionalSecurity) {
        if (!user) return withReadableByEveryone(criteria, additionalSecurity)

        if (!includeDeleted) criteria = withNotDeleted(criteria)

        if (user.isAdministrator()) return criteria

        def groupIdsUserBelongsTo = UserGroup.byHasMember(user).id().list()

        criteria.or {

            // Writeable
            eq 'createdBy', user
            inList 'id', {
                writeableByUsers {eq 'id', user.id}
                projections {property 'id'}
            }
            if (groupIdsUserBelongsTo) {
                inList 'id', {
                    writeableByGroups {inList 'id', groupIdsUserBelongsTo}
                    projections {property 'id'}
                }
            }

            // Readable
            eq 'readableByEveryone', true
            eq 'readableByAuthenticated', true

            inList 'id', {
                readableByUsers {eq 'id', user.id}
                projections {property 'id'}
            }
            if (groupIdsUserBelongsTo) {
                inList 'id', {
                    readableByGroups {inList 'id', groupIdsUserBelongsTo}
                    projections {property 'id'}
                }
            }
            if (additionalSecurity) {
                additionalSecurity.delegate = criteria
                additionalSecurity.call()
            }
        }
    }

    static <T extends Editable> DetachedCriteria<T> withReadable(DetachedCriteria<T> criteria, Set<UUID> readableIds, Boolean includeDeleted) {
        if (!readableIds) return withReadableByEveryone(criteria)
        if (!includeDeleted) criteria = withNotDeleted(criteria)
        criteria.inList('id', readableIds.toList())
    }

    static <T extends Editable> DetachedCriteria<T> withReadableByEveryone(DetachedCriteria<T> criteria,
                                                                           @DelegatesTo(AbstractDetachedCriteria) Closure additionalSecurity = null) {
        withNotDeleted(criteria).or {
            eq('readableByEveryone', true)
            if (additionalSecurity) {
                additionalSecurity.delegate = criteria
                additionalSecurity.call()
            }
        }
    }

    static <T extends Editable> DetachedCriteria<T> withNotDeleted(DetachedCriteria<T> criteria,
                                                                   @DelegatesTo(AbstractDetachedCriteria) Closure additionalSecurity = null) {
        criteria.and {
            eq('deleted', false)
            if (additionalSecurity) {
                additionalSecurity.delegate = criteria
                additionalSecurity.call()
            }
        }

    }
*/
}