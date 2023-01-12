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
package uk.ac.ox.softeng.maurodatamapper.core.facet

/**
 * @since 12/02/2018
 */
enum SemanticLinkType {
    REFINES('Refines'),
    DOES_NOT_REFINE('Does Not Refine'),
    ABSTRACTS('Abstracts'),
    DOES_NOT_ABSTRACT('Does Not Abstract'),
    IS_FROM('Is From', false)

    String label
    boolean isAssignable

    SemanticLinkType(String label) {
        this.label = label
        this.isAssignable = true
    }

    SemanticLinkType(String label, boolean isAssignable) {
        this.label = label
        this.isAssignable = isAssignable
    }

    static SemanticLinkType findForLabel(String label) {
        String convert = label?.toUpperCase()?.replaceAll(/ /, '_')
        try {
            return valueOf(convert)
        } catch (Exception ignored) {}
        null
    }

    static SemanticLinkType findFromMap(def map) {
        map['linkType'] instanceof SemanticLinkType ? map['linkType'] as SemanticLinkType : findForLabel(map['linkType'] as String)
    }
}