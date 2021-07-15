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
package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.merge

import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.merge.legacy.LegacyFieldPatchData

import grails.validation.Validateable

/**
 * @since 07/02/2018
 */
class ObjectPatchData<T> implements Validateable {

    UUID sourceId
    UUID targetId
    String label
    private List<FieldPatchData> patches

    @Deprecated
    List<LegacyFieldPatchData> diffs

    static constraints = {
        sourceId nullable: false
        targetId nullable: false
        label nullable: true, blank: false
        patches validator: {val, obj ->
            if (!val && !obj.diffs) ['default.invalid.min.message', 1]
        }
    }

    ObjectPatchData() {
        patches = []
        diffs = []
    }

    boolean hasPatches() {
        patches || getDiffsWithContent()
    }

    List<FieldPatchData> getPatches() {
        return patches
    }

    @Deprecated
    List<LegacyFieldPatchData> getDiffsWithContent() {
        diffs.findAll {it.hasPatches()}
    }

    @Deprecated
    void setLeftId(UUID leftId) {
        this.targetId = leftId
    }

    @Deprecated
    void setRightId(UUID rightId) {
        this.sourceId = rightId
    }

    @Deprecated
    UUID getLeftId() {
        this.targetId
    }

    @Deprecated
    UUID getRightId() {
        this.sourceId
    }
}
