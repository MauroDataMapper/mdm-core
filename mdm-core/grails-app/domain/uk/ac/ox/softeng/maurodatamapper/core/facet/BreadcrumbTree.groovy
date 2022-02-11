/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.Breadcrumb
import uk.ac.ox.softeng.maurodatamapper.hibernate.PathUserType
import uk.ac.ox.softeng.maurodatamapper.path.Path

import grails.compiler.GrailsCompileStatic
import grails.gorm.DetachedCriteria
import groovy.util.logging.Slf4j

@Slf4j
@GrailsCompileStatic
class BreadcrumbTree {

    UUID id
    UUID domainId
    String domainType
    String label
    Boolean finalised
    Boolean topBreadcrumbTree
    CatalogueItem domainEntity
    Path path

    String treeString

    static belongsTo = [parent: BreadcrumbTree]

    static hasMany = [
        children: BreadcrumbTree
    ]

    static constraints = {
        finalised nullable: true
        domainType blank: false
        label blank: false, nullable: true, validator: {String val, BreadcrumbTree obj ->
            if (val) return true
            if (!val && obj.domainEntity && !obj.domainEntity.label) return true
            ['default.null.message']
        }
        treeString blank: true
        parent nullable: true, validator: {BreadcrumbTree val, BreadcrumbTree obj ->
            obj.topBreadcrumbTree || val ? true : ['default.null.message']
        }
        domainId nullable: true, unique: true, validator: {UUID val, BreadcrumbTree obj ->
            if (val) return true
            if (!val && obj.domainEntity && !obj.domainEntity.ident()) return true
            ['default.null.message']
        }
    }

    static mapping = {
        treeString type: 'text'
        label type: 'text'
        parent cascadeValidate: 'none' //, cascade: 'none'
        children cascade: 'all-delete-orphan'
        path type: PathUserType
    }

    static mappedBy = [
        domainEntity: 'none',
    ]

    static transients = ['breadcrumb', 'domainEntity']

    BreadcrumbTree() {
    }

    BreadcrumbTree(Model model) {
        initialise(model)
        this.finalised = model.finalised
        this.topBreadcrumbTree = true
    }

    BreadcrumbTree(ModelItem modelItem) {
        initialise(modelItem)
        if (modelItem.parent) {
            BreadcrumbTree parentTree = findOrCreateBreadcrumbTree(modelItem.parent)
            parentTree.addToChildren(this)
        }
    }

    private initialise(CatalogueItem catalogueItem) {
        this.domainEntity = catalogueItem
        this.domainId = catalogueItem.id
        this.domainType = catalogueItem.domainType
        this.topBreadcrumbTree = false
        if (catalogueItem.label) {
            this.label = catalogueItem.label
            this.path = catalogueItem.path
        } else {
            this.label = 'NOT_VALID'
        }
    }

    def beforeValidate() {
        beforeValidateCheck(true)
    }

    def beforeValidateCheck(boolean cascade = true) {
        if (this.shouldSkipValidation()) return
        if (id && !isDirty()) trackChanges()
        if (domainEntity) {
            UUID originalId = domainId
            domainId = domainEntity.id
            markDirty('domainId', domainEntity.id, originalId)
            String originalLabel = label
            label = domainEntity.label
            markDirty('label', domainEntity.label, originalLabel)
            Path originalPath = path
            path = domainEntity.path
            markDirty('path', domainEntity.path, originalPath)
        }
        checkTree()
        if (cascade) {
            // After checking the tree, if its changed (or we havent been saved before) then we will need to update all the children
            if (isDirty('treeString') && children) {
                children.each {
                    it.buildTree()
                    it.beforeValidate()
                }
            }
        }
    }

    def beforeInsert() {
        if (domainEntity && !domainId) domainId = domainEntity.getId()
        checkTree()
        true
    }

    String getTree() {
        checkTree()
        this.treeString
    }

    void checkTree() {
        if (!treeString) buildTree()
        else if (!matchesTree(treeString)) buildTree()
    }

    void updateTree() {
        buildTree()
        children?.each {it.updateTree()}
    }

    void buildTree() {
        String oldTreeString = treeString
        StringBuilder newTreeString = new StringBuilder()
        if (parent) {
            newTreeString.append(parent.getTree()).append('\n')
        }
        newTreeString.append(getBreadcrumb().toString())
        setTreeString(newTreeString.toString())
        log.trace('Updated Tree string for {}:{} from {} ==> {}', domainType, domainId, oldTreeString, treeString)
    }

    void removeFromParent() {
        parent?.removeFromChildren(this)
    }

    List<Breadcrumb> getBreadcrumbs() {
        getBreadcrumbsFromTree(getTree()).init()
    }

    @Override
    String toString() {
        "BreadcrumbTree:${domainType}:${domainId}:${label}"
    }

    boolean matchesTree(String treeToMatch) {
        List<Breadcrumb> breadcrumbs = getBreadcrumbsFromTree(treeToMatch)
        breadcrumbs.last() == getBreadcrumb()
    }

    boolean matchesPath(Path path) {
        this.path?.matches(path)
    }

    void update(CatalogueItem catalogueItem) {
        this.domainEntity = catalogueItem
        if (catalogueItem.instanceOf(ModelItem)) {

            ModelItem modelItem = catalogueItem as ModelItem

            if (modelItem.parent) {
                BreadcrumbTree parentTree = findOrCreateBreadcrumbTree(modelItem.parent)
                if (parent != parentTree) {
                    if (parent) parent.removeFromChildren(this)
                    parentTree.addToChildren(this)
                } else if (modelItem.parent.instanceOf(ModelItem) && !parent.matchesPath(modelItem.parent.path)) {
                    parent.update((modelItem.parent as ModelItem))
                }
            }
        }
        buildTree()
        this.path = catalogueItem.path
        catalogueItem.markDirty('breadcrumbTree')
    }

    Breadcrumb getBreadcrumb() {
        new Breadcrumb(domainId, domainType, label, finalised)
    }

    void disableValidation() {
        skipValidation(true)
        children?.each {it.disableValidation()}
    }

    static BreadcrumbTree findOrCreateBreadcrumbTree(CatalogueItem catalogueItem) {
        if (catalogueItem.breadcrumbTree) return catalogueItem.breadcrumbTree

        if (catalogueItem.instanceOf(Model)) {
            catalogueItem.breadcrumbTree = new BreadcrumbTree(catalogueItem as Model)
        }
        if (catalogueItem.instanceOf(ModelItem)) {
            catalogueItem.breadcrumbTree = new BreadcrumbTree(catalogueItem as ModelItem)
        }
        catalogueItem.breadcrumbTree
    }

    static List<Breadcrumb> getBreadcrumbsFromTree(String treeString) {
        treeString.split('\n').collect {new Breadcrumb(it)}
    }

    static BreadcrumbTree findByCatalogueItem(CatalogueItem catalogueItem) {
        findByCatalogueItemDomainAndId(catalogueItem.domainType, catalogueItem.id)
    }

    static BreadcrumbTree findByCatalogueItemDomainAndId(String domainType, UUID domainId) {
        new DetachedCriteria<BreadcrumbTree>(BreadcrumbTree).eq('domainType', domainType).eq('domainId', domainId).get()
    }
}
