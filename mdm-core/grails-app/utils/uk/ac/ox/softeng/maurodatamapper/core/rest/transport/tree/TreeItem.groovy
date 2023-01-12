/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree

import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * @since 27/10/2017
 */
@Slf4j
@CompileStatic
class TreeItem implements Comparable<TreeItem> {

    UUID id
    String label
    String domainType
    Path path

    UUID parentId
    UUID rootId

    protected Set<TreeItem> childSet
    List<UUID> pathIds

    boolean renderChildren
    Boolean childrenExist

    int domainTypeIndex
    List<String> availableActions

    protected TreeItem(MdmDomain domain, String label, Boolean childrenExist, List<String> availableTreeActions) {
        childSet = [] as HashSet
        renderChildren = false
        pathIds = []

        this.id = domain.id
        this.label = label
        this.domainType = domain.domainType
        this.childrenExist = childrenExist
        this.availableActions = availableTreeActions
        this.path = domain.path
        setDomainTypeIndex(domain.getClass())
    }

    @Override
    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        TreeItem treeItem = (TreeItem) o

        if (domainType != treeItem.domainType) return false
        if (id != treeItem.id) return false
        if (label?.toLowerCase() != treeItem.label?.toLowerCase()) return false

        true
    }

    @Override
    int hashCode() {
        int result
        result = (id != null ? id.hashCode() : 0)
        result = 31 * result + (label != null ? label.hashCode() : 0)
        result = 31 * result + (domainType != null ? domainType.hashCode() : 0)
        result
    }

    @Override
    String toString() {
        "${label} [$id]"
    }

    @Override
    @SuppressFBWarnings(value = ['NP_NULL_PARAM_DEREF', 'NP_NULL_PARAM_DEREF', 'NP_NULL_ON_SOME_PATH'], justification = 'Groovy elvis operator')
    int compareTo(TreeItem that) {
        def res = this.domainTypeIndex <=> that.domainTypeIndex
        if (res == 0) res = this.label?.toLowerCase() <=> that.label?.toLowerCase()
        res
    }

    /**
     * Allows the use of array mapping access through a tree.
     * e.g. item[0][1][2]
     * @param index
     * @return
     * @throws ArrayIndexOutOfBoundsException if no children or index out of bounds
     */
    TreeItem getAt(int index) throws ArrayIndexOutOfBoundsException {
        if (childSet) return children[index]
        throw new ArrayIndexOutOfBoundsException(index)
    }

    TreeItem find(String label) {
        childSet.find {it.label == label}
    }

    TreeItem find(Closure predicate) {
        childSet.find predicate
    }

    Set<TreeItem> findAll(Closure predicate) {
        childSet.findAll predicate
    }

    boolean hasParent() {
        parentId != rootId
    }

    TreeItem addAllToChildren(Collection<? extends TreeItem> childrenToAdd) {
        childSet.addAll(childrenToAdd)
        childrenExist = true
        this
    }

    boolean every(Closure predicate) {
        childSet.every predicate
    }

    boolean any(Closure predicate) {
        childSet.any predicate
    }

    int size() {
        childSet.size()
    }

    List<TreeItem> getChildren() {
        childSet.sort()
    }

    TreeItem withRenderChildren() {
        renderChildren = true
        this
    }

    /**
     * This method relies on items being added in depth order.
     * It will not add the children if the parent cannot be found in the direct children of this treeitem
     *
     * @param childrenToAdd
     * @return
     */
    TreeItem recursivelyAddToChildren(Collection<? extends TreeItem> childrenToAdd, int indent = 2, boolean addMissingOnly = false) {
        Map<String, List<TreeItem>> levelGrouped = (childrenToAdd.groupBy {it.getParentId() == id ? 'alpha' : 'beta'} as Map<String, List<TreeItem>>)

        if (levelGrouped.alpha) {
            List<TreeItem> toAdd = addMissingOnly || childSet.empty ?
                                   levelGrouped.alpha.findAll {ti -> !(ti.id in childSet*.id)} :
                                   levelGrouped.alpha
            log.trace('{}Adding {} children to parent [{}] ', ' ' * indent, toAdd.size(), this.label)
            addAllToChildren(toAdd)
        }

        if (levelGrouped.beta) {
            log.trace('{}Beta children present', ' ' * indent)
            Map<UUID, List<TreeItem>> childGroupedItems = levelGrouped.beta.groupBy {it.getNextParentId(id)}
            childGroupedItems.each {childId, grandChildren ->
                TreeItem child = childSet.find {it.id == childId}
                child?.recursivelyAddToChildren(grandChildren, indent + 2, addMissingOnly)
            }
        }
        this
    }

    boolean hasChildren() {
        if (renderChildren) !childSet.isEmpty()
        else childrenExist != null ? childrenExist : !childSet.isEmpty()
    }

    UUID getNextParentId(UUID currentParent) {
        int index = allNonRootIds.indexOf(currentParent) + 1
        index < allNonRootIds.size() ? allNonRootIds[index] : null
    }

    List<UUID> getAllNonRootIds() {
        List<UUID> ids = new ArrayList<>(pathIds)
        if (ids) ids.remove(0)
        ids
    }

    private int setDomainTypeIndex(Class itemClass) {
        if (Container.isAssignableFrom(itemClass)) domainTypeIndex = 0
        else if (Model.isAssignableFrom(itemClass)) domainTypeIndex = 1
        else domainTypeIndex = 2
    }
}
