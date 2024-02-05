/*
 * Copyright 2020-2024 University of Oxford and NHS England
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package uk.ac.ox.softeng.maurodatamapper.security.role

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolderService
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional

@Transactional
class VirtualSecurableResourceGroupRoleService {

    VersionedFolderService versionedFolderService

    VirtualSecurableResourceGroupRole buildFromSecurableResourceGroupRole(SecurableResourceGroupRole securableResourceGroupRole) {
        this.buildForSecurableResource(securableResourceGroupRole.securableResource)
            .definedByGroup(securableResourceGroupRole.userGroup)
            .definedByAccessLevel(securableResourceGroupRole.groupRole)
    }

    VirtualSecurableResourceGroupRole buildForSecurableResource(SecurableResource securableResource) {
        VirtualSecurableResourceGroupRole virtualRole = new VirtualSecurableResourceGroupRole()
            .forSecurableResource(securableResource)

        if (securableResource.domainType == VersionedFolder.simpleName) {
            virtualRole.withAlternateDomainType(Folder.simpleName)
        }

        if (Utils.parentClassIsAssignableFromChild(Folder, securableResource.class)) {
            virtualRole
                .withDependencyOnAccessToDomainId((securableResource as Folder).parentFolder?.id)
                .asVersionControlled(versionedFolderService.hasVersionedFolderParent(securableResource as Folder))
                .withVersionedContents(versionedFolderService.doesDepthTreeContainVersionedFolder(securableResource as Folder) ||
                                       versionedFolderService.doesDepthTreeContainFinalisedModel(securableResource as Folder))
        } else if (Utils.parentClassIsAssignableFromChild(Classifier, securableResource.class)) {
            virtualRole.withDependencyOnAccessToDomainId((securableResource as Classifier).parentClassifier?.id)
        }

        if (Utils.parentClassIsAssignableFromChild(Model, securableResource.class)) {
            Model model = securableResource as Model
            boolean modelIsInsideVersionedFolder = versionedFolderService.isVersionedFolderFamily(model.folder)
            virtualRole.withDependencyOnAccessToDomainId(model.folder?.id)
                .asFinalised(model.finalised)
            // If the container is versioned then models inside it cannot be finalised
                .asFinalisable(!modelIsInsideVersionedFolder &&
                               model.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME)
                .asVersionable(!modelIsInsideVersionedFolder)
                .asVersionControlled(modelIsInsideVersionedFolder)
                .withVersionedContents(model.finalised)
        }

        if (Utils.parentClassIsAssignableFromChild(VersionedFolder, securableResource.class)) {
            VersionedFolder versionedFolder = securableResource as VersionedFolder
            virtualRole.asFinalised(versionedFolder.finalised)
                .asFinalisable(versionedFolder.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME)
                .asVersionable(true)

                .asVersionControlled(false)
                .withVersionedContents(true)
        }
        virtualRole
    }

}