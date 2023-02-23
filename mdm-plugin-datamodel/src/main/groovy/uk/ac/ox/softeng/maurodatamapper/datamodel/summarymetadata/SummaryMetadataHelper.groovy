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
package uk.ac.ox.softeng.maurodatamapper.datamodel.summarymetadata

import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.summarymetadata.SummaryMetadataReport
import uk.ac.ox.softeng.maurodatamapper.security.User

import groovy.json.JsonBuilder

import java.time.OffsetDateTime

class SummaryMetadataHelper {


    static SummaryMetadata createSummaryMetadataFromMap(User user, String headerName, String description, OffsetDateTime reportDateTime, Map<String, Long> valueDistribution) {
        SummaryMetadata summaryMetadata = new SummaryMetadata(
            label: headerName,
            description: description,
            summaryMetadataType: SummaryMetadataType.MAP,
            createdBy: user.emailAddress
        )
        SummaryMetadataReport smr = new SummaryMetadataReport(
            reportDate: reportDateTime,
            reportValue: new JsonBuilder(valueDistribution).toString(),
            createdBy: user.emailAddress
        )
        summaryMetadata.addToSummaryMetadataReports(smr)
        return summaryMetadata
    }
}
