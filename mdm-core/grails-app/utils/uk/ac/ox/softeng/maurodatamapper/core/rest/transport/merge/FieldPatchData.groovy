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
package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.merge

import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable
import uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional.CreationMergeDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional.DeletionMergeDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional.FieldMergeDiff
import uk.ac.ox.softeng.maurodatamapper.path.Path

import grails.validation.Validateable

/**
 * @since 07/02/2018
 */
class FieldPatchData<T> implements Validateable, Comparable<FieldPatchData> {

    String fieldName
    Path path
    T sourceValue
    T targetValue
    T commonAncestorValue
    boolean isMergeConflict
    String type

    static constraints = {
        fieldName nullable: false, blank: false
        path nullable: false, blank: false
        sourceValue nullable: true
        targetValue nullable: true
        commonAncestorValue nullable: true
        type nullable: false, blank: false, inList: ['creation', 'deletion', 'modification']

    }

    boolean isMetadataChange() {
        false
    }

    boolean isCreation() {
        type == 'creation'
    }

    boolean isDeletion() {
        type == 'deletion'
    }

    boolean isModification() {
        type == 'modification'
    }

    String toString() {
        String base = "Merge ${type} patch on ${path}"
        type == 'modification' ? "${base} :: Changing ${targetValue} to ${sourceValue}" : base
    }

    void setPath(String path) {
        this.path = Path.from(path)
    }

    void setPath(Path path) {
        this.path = path
    }

    Path getRelativePathToRoot() {
        this.path.childPath
    }

    static <P> FieldPatchData<P> from(FieldMergeDiff<P> fieldMergeDiff) {
        new FieldPatchData().tap {
            fieldName = fieldMergeDiff.fieldName
            sourceValue = fieldMergeDiff.source
            targetValue = fieldMergeDiff.target
            commonAncestorValue = fieldMergeDiff.commonAncestor
            path = fieldMergeDiff.fullyQualifiedPath
            isMergeConflict = fieldMergeDiff.isMergeConflict()
            type = 'modification'
        }
    }

    static <P extends Diffable> FieldPatchData<P> from(CreationMergeDiff<P> creationMergeDiff) {
        new FieldPatchData().tap {
            //            fieldName = creationMergeDiff.fieldName
            sourceValue = creationMergeDiff.source
            targetValue = creationMergeDiff.target
            commonAncestorValue = creationMergeDiff.commonAncestor
            path = creationMergeDiff.fullyQualifiedPath
            isMergeConflict = creationMergeDiff.isMergeConflict()
            type = 'creation'
        }
    }

    static <P extends Diffable> FieldPatchData<P> from(DeletionMergeDiff<P> deletionMergeDiff) {
        new FieldPatchData().tap {
            //            fieldName = deletionMergeDiff.fieldName
            sourceValue = deletionMergeDiff.source
            targetValue = deletionMergeDiff.target
            commonAncestorValue = deletionMergeDiff.commonAncestor
            path = deletionMergeDiff.fullyQualifiedPath
            isMergeConflict = deletionMergeDiff.isMergeConflict()
            type = 'deletion'
        }
    }

    @Override
    int compareTo(FieldPatchData that) {
        switch (this.type) {
            case 'modification':
                if (that.type == 'modification') return 0
                else return 1
            case 'creation':
                if (that.type == 'modification') return -1
                if (that.type == 'deletion') return -1
                return 0
            case 'deletion':
                if (that.type == 'modification') return -1
                if (that.type == 'creation') return 1
                return 0
        }
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        FieldPatchData that = (FieldPatchData) o

        if (isMergeConflict != that.isMergeConflict) return false
        if (fieldName != that.fieldName) return false
        if (path != that.path) return false
        if (type != that.type) return false

        return true
    }

    int hashCode() {
        int result
        result = fieldName.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + (isMergeConflict ? 1 : 0)
        result = 31 * result + type.hashCode()
        return result
    }
}
