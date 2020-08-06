/*
 * Copyright 2020 University of Oxford
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
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.InformationAware
import uk.ac.ox.softeng.maurodatamapper.util.Utils
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.PathAware

import grails.compiler.GrailsCompileStatic
import grails.gorm.DetachedCriteria
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity

@Slf4j
@GrailsCompileStatic
class BreadcrumbTree {

    UUID id
    UUID domainId
    String domainType
    String label
    Boolean finalised
    Breadcrumb breadcrumb
    Boolean topBreadcrumbTree
    CatalogueItem domainEntity
    BreadcrumbTree parent

    String treeString

    static belongsTo = [BreadcrumbTree]

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
        parent cascade: 'save-update'
    }

    static mappedBy = [
        domainEntity: 'none',
        children    : 'parent'
    ]

    static transients = ['breadcrumb', 'domainEntity']

    BreadcrumbTree() {
    }

    BreadcrumbTree(Model model) {
        this.domainEntity = model
        this.domainId = model.id
        this.label = model.label ?: 'NOT_VALID'
        this.domainType = model.domainType
        this.finalised = model.finalised
        this.topBreadcrumbTree = true
    }

    BreadcrumbTree(ModelItem modelItem) {
        this.domainEntity = modelItem
        this.domainId = modelItem.id
        this.label = modelItem.label ?: 'NOT_VALID'
        this.domainType = modelItem.domainType
        this.topBreadcrumbTree = false
        if (modelItem.pathParent) {
            BreadcrumbTree parentTree = findOrCreateBreadcrumbTree(modelItem.pathParent as CatalogueItem)
            parentTree.addToChildren(this)
        }
    }

    def beforeValidate() {
        if (id && !isDirty()) trackChanges()
        if (domainEntity) {
            domainId = domainEntity.id
            markDirty('domainId', domainEntity.id, getOriginalValue('domainId'))
            label = domainEntity.label
            markDirty('label', domainEntity.label, getOriginalValue('label'))
        }
        checkTree()
        children?.each {
            it.beforeValidate()
        }
    }

    String getTree() {
        checkTree()
        this.treeString
    }

    void checkTree() {
        if (!treeString) buildTree()
        else if (!matchesTree(treeString)) buildTree()
    }

    void buildTree() {
        String newTreeString = ''
        if (parent) {
            newTreeString += "${parent.getTree()}\n"
        }
        newTreeString += getBreadcrumb().toString()
        setTreeString(newTreeString)
        log.trace('Tree string for {}:{} ==> {}', domainType, domainId, treeString)
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

    boolean matchesPath(String path) {
        List<Breadcrumb> breadcrumbs = getBreadcrumbsFromTree(getTree())

        if (!path) return breadcrumbs.size() == 1

        List<UUID> pathIds = path.split('/').findAll().collect {Utils.toUuid(it)}
        if (pathIds.size() + 1 != breadcrumbs.size()) return false


        for (int i = 0; i < pathIds.size(); i++) {
            if (breadcrumbs[i].id != pathIds[i]) return false
        }
        true
    }

    void update(CatalogueItem catalogueItem) {

        if (catalogueItem.instanceOf(PathAware)) {

            PathAware pathAware = catalogueItem as PathAware

            if (pathAware.pathParent && pathAware.pathParent.instanceOf(CatalogueItem)) {
                BreadcrumbTree parentTree = findOrCreateBreadcrumbTree(pathAware.pathParent as CatalogueItem)
                if (parent != parentTree) {
                    if (parent) parent.removeFromChildren(this)
                    parentTree.addToChildren(this)
                } else if (pathAware.pathParent.instanceOf(ModelItem) && !parent.matchesPath((pathAware.pathParent as ModelItem).path)) {
                    parent.update((pathAware.pathParent as ModelItem))
                }
            }
        }
        buildTree()
    }

    Breadcrumb getBreadcrumb() {
        new Breadcrumb(domainId, domainType, label, finalised)
    }

    static BreadcrumbTree findBreadcrumbTree(CatalogueItem catalogueItem) {
        if (catalogueItem.breadcrumbTree) return catalogueItem.breadcrumbTree
        if (catalogueItem.getId()) return BreadcrumbTree.findByCatalogueItem(catalogueItem)
        null
    }

    static BreadcrumbTree findOrCreateBreadcrumbTree(CatalogueItem catalogueItem) {
        BreadcrumbTree breadcrumbTree = findBreadcrumbTree(catalogueItem)
        if (!breadcrumbTree) {
            if (catalogueItem.instanceOf(Model)) {
                breadcrumbTree = new BreadcrumbTree(catalogueItem as Model)
            }
            if (catalogueItem.instanceOf(ModelItem)) {
                breadcrumbTree = new BreadcrumbTree(catalogueItem as ModelItem)
            }
        }
        breadcrumbTree
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
