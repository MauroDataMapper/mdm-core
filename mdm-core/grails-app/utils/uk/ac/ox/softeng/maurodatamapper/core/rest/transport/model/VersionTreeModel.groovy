/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.VersionAware

class VersionTreeModel implements Comparable<VersionTreeModel> {

    VersionAware versionAware
    Boolean newDocumentationVersion
    Boolean newBranchModelVersion
    Boolean newFork
    VersionTreeModel parentVersionTreeModel
    List<ModelLinkTarget> targets

    VersionTreeModel(VersionAware versionAware, VersionLinkType versionLinkType, VersionTreeModel parentVersionTreeModel) {
        this.targets = []
        this.newBranchModelVersion = false
        this.newDocumentationVersion = false
        this.newFork = false

        this.versionAware = versionAware
        this.parentVersionTreeModel = parentVersionTreeModel

        if (this.parentVersionTreeModel) {
            this.parentVersionTreeModel.addTarget(this.versionAware, versionLinkType)
            newDocumentationVersion = versionLinkType == VersionLinkType.NEW_DOCUMENTATION_VERSION_OF
            newBranchModelVersion = versionLinkType == VersionLinkType.NEW_MODEL_VERSION_OF
            newFork = versionLinkType == VersionLinkType.NEW_FORK_OF
        }
    }

    void addTarget(UUID targetId, VersionLinkType linkType) {
        targets.add(new ModelLinkTarget(targetId, linkType))
    }

    void addTarget(VersionAware target, VersionLinkType linkType) {
        addTarget(target.id, linkType)
    }

    String getLabel() {
        versionAware.label
    }

    String getId() {
        versionAware.id.toString()
    }

    String getBranchName() {
        versionAware.modelVersion ? null : versionAware.branchName
    }

    String getModelVersion() {
        versionAware.modelVersion
    }

    String getModelVersionTag() {
        versionAware.modelVersionTag
    }

    String getDocumentationVersion() {
        versionAware.documentationVersion
    }

    @Override
    int compareTo(VersionTreeModel that) {

        // If not a version then it belongs at the top of the list
        if (!this.newBranchModelVersion && !this.newDocumentationVersion && !this.newFork) return -1
        if (!that.newBranchModelVersion && !that.newDocumentationVersion && !that.newFork) return 1

        // If is a target of other model then belongs before
        if (that.id in targets.collect { it.modelId }) return -1
        if (this.id in that.targets.collect { it.modelId }) return 1

        // Model versions are ordered
        if (this.modelVersion && that.modelVersion) return this.modelVersion <=> that.modelVersion

        // Check parent ordering
        int parentOrder = this.parentVersionTreeModel <=> that.parentVersionTreeModel
        if (parentOrder != 0) return parentOrder

        // If one doesnt have a model version then it comes before the other after parent sorting has happened
        if (this.modelVersion) return 1
        if (that.modelVersion) return -1

        // Fork models come after all branches
        if (this.newFork) return 1
        if (that.newFork) return -1

        // Main branch comes first
        if (this.branchName == 'main') return -1
        if (that.branchName == 'main') return 1

        this.branchName <=> that.branchName
    }

    String getSimpleDisplayName() {
        String prefix = modelVersion ? 'V' : newDocumentationVersion ? 'DM V' : newFork ? 'FM ' : ''
        String version = modelVersion ?: newDocumentationVersion ? documentationVersion : branchName
        String base = parentVersionTreeModel && !modelVersion ? " (V${parentVersionTreeModel.modelVersion})" : ''
        "${prefix}${version}$base"
    }

    @Override
    String toString() {
        simpleDisplayName
    }
}
