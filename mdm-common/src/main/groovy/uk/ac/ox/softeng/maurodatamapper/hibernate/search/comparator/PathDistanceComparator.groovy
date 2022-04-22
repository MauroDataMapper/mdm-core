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
package uk.ac.ox.softeng.maurodatamapper.hibernate.search.comparator

import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import org.apache.lucene.util.SloppyMath
import org.hibernate.search.engine.spatial.GeoPoint

/**
 * @since 22/04/2022
 */
class PathDistanceComparator implements Comparator<MdmDomain> {

    private static final double TO_METERS = 6371008.7714D
    // equatorial radius

    GeoPoint center
    boolean ascending

    PathDistanceComparator(GeoPoint center, boolean ascending) {
        this.center = center
        this.ascending = ascending
    }

    @Override
    int compare(MdmDomain left, MdmDomain right) {

        double leftHaversinDistance = SloppyMath.haversinSortKey(center.latitude(), center.longitude(), left.path.latitude, left.path.longitude)
        double rightHaversinDistance = SloppyMath.haversinSortKey(center.latitude(), center.longitude(), right.path.latitude, right.path.longitude)

        double leftDistance = haversinNanoMeters(leftHaversinDistance)
        double rightDistance = haversinNanoMeters(rightHaversinDistance)
        ascending ? leftDistance <=> rightDistance : rightDistance <=> leftDistance
    }

    public static double haversinNanoMeters(double sortKey) {
        return (TO_METERS * 1E9) * 2 * SloppyMath.asin(Math.min(1, Math.sqrt(sortKey * 0.5)))
    }
}
