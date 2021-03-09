/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model

import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.core.model.Model

class VersionTreeModel {
    String modelId
    String label
    String branchName
    Boolean newDocumentationVersion
    Boolean newBranchModelVersion
    Boolean newFork
    List<ModelLinkTarget> targets

    VersionTreeModel(Model model, VersionLinkType versionLinkType) {
        modelId = model.id.toString()
        label = model.label
        branchName = model.branchName
        newDocumentationVersion = versionLinkType == VersionLinkType.NEW_DOCUMENTATION_VERSION_OF
        newBranchModelVersion = versionLinkType == VersionLinkType.NEW_MODEL_VERSION_OF
        newFork = versionLinkType == VersionLinkType.NEW_FORK_OF
        targets = []
    }

    void addTarget(UUID targetId, VersionLinkType linkType){
        targets.add(new ModelLinkTarget(targetId, linkType))
    }
}
