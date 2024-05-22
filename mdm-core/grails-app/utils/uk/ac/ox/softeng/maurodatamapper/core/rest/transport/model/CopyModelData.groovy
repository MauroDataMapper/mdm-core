package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model

import uk.ac.ox.softeng.maurodatamapper.version.Version
import uk.ac.ox.softeng.maurodatamapper.version.VersionChangeType

import grails.databinding.BindUsing
import grails.validation.Validateable

/**
 * Instances of this class gather the parameters passed to the data model plugin's
 * DataModelController.copyModel() method via the /api/dataModels//copyModel URL.
 */
class CopyModelData implements Validateable {
    UUID folderId
    String label
    Boolean copyPermissions
    String branchName

    @BindUsing({obj, source -> Version.from(source['version'] as String) })
    Version version
}
