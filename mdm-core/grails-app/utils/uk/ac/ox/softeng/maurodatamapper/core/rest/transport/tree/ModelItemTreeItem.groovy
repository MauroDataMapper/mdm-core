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
package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree

import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem

/**
 * @since 07/01/2020
 */
class ModelItemTreeItem extends TreeItem {

    int order

    ModelItemTreeItem(ModelItem modelItem, Boolean childrenExist) {
        super(modelItem, modelItem.id, modelItem.label, modelItem.domainType, childrenExist)
        order = modelItem.getOrder()
    }

    @Override
    int compareTo(TreeItem that) {
        def res = this.domainTypeIndex <=> that.domainTypeIndex
        if (res != 0) return res

        ModelItemTreeItem other = that as ModelItemTreeItem
        res = this.hasChildren() <=> other.hasChildren()
        if (res == 0) res = this.order <=> other.order
        if (res == 0) res = this.label <=> other.label
        res
    }
}
