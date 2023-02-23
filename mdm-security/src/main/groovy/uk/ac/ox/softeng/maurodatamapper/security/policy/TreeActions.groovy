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
package uk.ac.ox.softeng.maurodatamapper.security.policy

/**
 * @since 01/06/2021
 */
class TreeActions {

    public static final String CREATE_FOLDER = 'createFolder'
    public static final String CREATE_CONTAINER = 'createContainer'
    public static final String CREATE_MODEL = 'createModel'
    public static final String CREATE_MODEL_ITEM = 'createModelItem'
    public static final String MOVE_TO_VERSIONED_FOLDER = 'moveToVersionedFolder'
    public static final String MOVE_TO_FOLDER = 'moveToFolder'
    public static final String MOVE_TO_CONTAINER = 'moveToContainer'
    public static final String CREATE_VERSIONED_FOLDER = 'createVersionedFolder'
    public static final String SOFT_DELETE = 'softDelete'
    public static final String DELETE = 'delete'


    public static final List<String> EDITOR_FOLDER_TREE_ACTIONS = [CREATE_FOLDER, CREATE_MODEL, MOVE_TO_VERSIONED_FOLDER, CREATE_VERSIONED_FOLDER, SOFT_DELETE, MOVE_TO_FOLDER]
    public static final List<String> CONTAINER_ADMIN_FOLDER_TREE_ACTIONS = EDITOR_FOLDER_TREE_ACTIONS + [DELETE]

    public static final List<String> EDITOR_CONTAINER_TREE_ACTIONS = [CREATE_CONTAINER, CREATE_MODEL, SOFT_DELETE, MOVE_TO_CONTAINER]
    public static final List<String> CONTAINER_ADMIN_CONTAINER_TREE_ACTIONS = EDITOR_FOLDER_TREE_ACTIONS + [DELETE]

    public static final List<String> EDITOR_MODEL_TREE_ACTIONS = [CREATE_MODEL_ITEM, MOVE_TO_VERSIONED_FOLDER, SOFT_DELETE, MOVE_TO_FOLDER, MOVE_TO_CONTAINER]
    public static final List<String> CONTAINER_ADMIN_MODEL_TREE_ACTIONS = EDITOR_MODEL_TREE_ACTIONS + [DELETE]

    public static final List<String> DISALLOWED_MODELITEM_TREE_ACTIONS = [MOVE_TO_VERSIONED_FOLDER, SOFT_DELETE, MOVE_TO_CONTAINER, MOVE_TO_FOLDER]
}
