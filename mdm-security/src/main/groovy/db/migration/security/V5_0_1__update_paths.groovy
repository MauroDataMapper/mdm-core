package db.migration.security

import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole

import groovy.util.logging.Slf4j
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

@Slf4j
class V5_0_1__update_paths extends BaseJavaMigration {

    @Override
    void migrate(Context context) throws Exception {

        GroupRole.withNewTransaction {
            List<GroupRole> roles = GroupRole.findAllByParentIsNull()
            roles.each { r ->
                r.checkPath()
                log.debug('Path Migrating {}', r.path)
                r.children.each { rc -> checkGroupRole(rc) }
                r.validate()
                r.save(flush:true, validate:false)
            }
        }
    }

    void checkGroupRole(GroupRole groupRole) {
        if (groupRole.children) {
            groupRole.children.each { checkGroupRole(it) }
        }
    }
}
