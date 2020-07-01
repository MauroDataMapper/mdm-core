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
package uk.ac.ox.softeng.maurodatamapper.core.web.mapping


import static org.grails.web.mapping.DefaultUrlMappingEvaluator.ACTION_CREATE
import static org.grails.web.mapping.DefaultUrlMappingEvaluator.ACTION_EDIT
import static org.grails.web.mapping.DefaultUrlMappingEvaluator.ACTION_INDEX
import static org.grails.web.mapping.DefaultUrlMappingEvaluator.ACTION_PATCH
import static org.grails.web.mapping.DefaultUrlMappingEvaluator.ACTION_SAVE
import static org.grails.web.mapping.DefaultUrlMappingEvaluator.ACTION_SHOW
import static org.grails.web.mapping.DefaultUrlMappingEvaluator.ACTION_UPDATE

class UrlMappingActions {

    public static final List<String> DEFAULT_EXCLUDES = [ACTION_PATCH, ACTION_CREATE, ACTION_EDIT]
    public static final List<String> DEFAULT_EXCLUDES_AND_NO_SAVE = DEFAULT_EXCLUDES + [ACTION_SAVE]
    public static final List<String> DEFAULT_EXCLUDES_AND_NO_UPDATE = DEFAULT_EXCLUDES + [ACTION_UPDATE]
    public static final List<String> INCLUDES_READ_ONLY = [ACTION_INDEX, ACTION_SHOW]
    public static final List<String> INCLUDES_INDEX_ONLY = [ACTION_INDEX]
}
