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
package uk.ac.ox.softeng.maurodatamapper.referencedata.facet

enum ReferenceSummaryMetadataType {

    MAP,
    NUMBER,
    STRING

    /*
    static SummaryMetadataType findForLabel(String label) {
        values().find {it.label.equalsIgnoreCase(label)}
    }

    static SummaryMetadataType findFromMap(Map map) {
        map['summaryMetadataType'] instanceof SummaryMetadataType ? map['summaryMetadataType'] as SummaryMetadataType
                                                                  : findForLabel(map['summaryMetadataType'] as String)
    }

     */
}