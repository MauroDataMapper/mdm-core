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
package uk.ac.ox.softeng.maurodatamapper.core.model


import uk.ac.ox.softeng.maurodatamapper.core.facet.AnnotationService
import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFileService
import uk.ac.ox.softeng.maurodatamapper.core.facet.RuleService
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MdmDomainService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MultiFacetAwareService
import uk.ac.ox.softeng.maurodatamapper.path.PathNode
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResourceService
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager

import grails.gorm.DetachedCriteria
import org.hibernate.SessionFactory

/**
 * @since 16/01/2020
 */
abstract class ContainerService<K extends Container> implements SecurableResourceService<K>, MultiFacetAwareService<K>, MdmDomainService<K> {

    SessionFactory sessionFactory
    MetadataService metadataService
    RuleService ruleService
    SemanticLinkService semanticLinkService
    AnnotationService annotationService
    ReferenceFileService referenceFileService

    abstract boolean isContainerVirtual()

    abstract String getContainerPropertyNameInModel()

    abstract List<Container> findAllReadableContainersBySearchTerm(UserSecurityPolicyManager userSecurityPolicyManager, String searchTerm)

    abstract List<Container> findAllContainersInside(PathNode containerPathNode)

    abstract K findDomainByLabel(String label)

    abstract K findByParentIdAndLabel(UUID parentId, String label)

    abstract List<K> findAllByParentId(UUID parentId)

    abstract List<K> findAllByParentId(UUID parentId, Map pagination)

    abstract DetachedCriteria<K> getCriteriaByParent(K domain)

    abstract List<K> findAllWhereDirectParentOfModel(Model model)

    abstract List<K> findAllWhereDirectParentOfContainer(K container)

    Class<K> getMultiFacetAwareClass() {
        getDomainClass()
    }

    @Override
    K findByParentIdAndPathIdentifier(UUID parentId, String pathIdentifier, Map pathParams = [:]) {
        findByParentIdAndLabel(parentId, pathIdentifier)
    }

    K findByPath(String path) {
        List<String> paths
        if (path.contains('/')) paths = path.split('/').findAll() ?: []
        else paths = path.split('\\|').findAll() ?: []
        findByPath(paths)
    }

    K findByPath(List<String> pathLabels) {
        if (!pathLabels) return null
        if (pathLabels.size() == 1) {
            return findDomainByLabel(pathLabels[0])
        }

        String parentLabel = pathLabels.remove(0)
        K parent = findDomainByLabel(parentLabel)
        findByPath(parent, pathLabels)
    }

    K findByPath(K parent, List<String> pathLabels) {
        if (pathLabels.size() == 1) {
            return findByParentIdAndLabel(parent.id, pathLabels[0])
        }

        String nextParentLabel = pathLabels.remove(0)
        K nextParent = findByParentIdAndLabel(parent.id, nextParentLabel)
        findByPath(nextParent, pathLabels)
    }

    List<Container> findAllContainersInside(UUID containerId) {
        findAllContainersInside(get(containerId).path.last())
    }

    void generateDefaultLabel(K domain, String defaultLabel) {
        List<K> siblings = getCriteriaByParent(domain)
            .like('label', "${defaultLabel}%")
            .sort('label')
            .list()

        if (!siblings) {
            domain.label = defaultLabel
            return
        }

        String lastLabel = siblings.last().label
        int lastNum
        lastLabel.find(/${defaultLabel}( \((\d+)\))?/) {
            lastNum = it[1] ? it[2].toInteger() : 0
        }

        domain.label = "${defaultLabel} (${++lastNum})"
    }

    @Override
    boolean isMultiFacetAwareFinalised(K multiFacetAwareItem) {
        false
    }
}