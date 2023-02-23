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
package uk.ac.ox.softeng.maurodatamapper.version

import asset.pipeline.AssetFile

import java.nio.file.Path
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * @since 25/01/2017
 */
class Version implements Comparable<Version> {
    int major
    int minor
    int patch
    boolean snapshot

    static final Pattern VERSION_PATTERN = ~/((\d+)(\.(\d+)(\.(\d+))?)?(-SNAPSHOT)?)|SNAPSHOT/

    @Override
    int compareTo(Version that) {

        def result = this.major <=> that.major
        if (result == 0) result = this.minor <=> that.minor
        if (result == 0) result = this.patch <=> that.patch
        if (result == 0) result = this.snapshot <=> that.snapshot
        result
    }

    @Override
    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        Version version = (Version) o

        if (major != version.major) return false
        if (minor != version.minor) return false
        if (patch != version.patch) return false
        snapshot == version.snapshot
    }

    @Override
    int hashCode() {
        int result
        result = major
        result = 31 * result + minor
        result = 31 * result + patch
        result = 31 * result + (snapshot ? 1 : 0)
        result
    }

    @Override
    String toString() {
        if (major == 0 && minor == 0 && patch == 0 && snapshot) {
            return 'SNAPSHOT'
        }
        snapshot ? "${major}.${minor}.${patch}-SNAPSHOT" : "${major}.${minor}.${patch}"
    }

    static Version nextMajorVersion(Version version) {
        new Version(major: version.major + 1, minor: 0, patch: 0)
    }

    static Version nextMinorVersion(Version version) {
        new Version(major: version.major, minor: version.minor + 1, patch: 0)
    }

    static Version nextPatchVersion(Version version) {
        new Version(major: version.major, minor: version.minor, patch: version.patch + 1)
    }

    static Version from(String versionStr) {

        if (!versionStr) throw new IllegalStateException('Must have a version')

        if (versionStr == 'SNAPSHOT') return new Version(major: 0, minor: 0, patch: 0, snapshot: true)

        Matcher m = VERSION_PATTERN.matcher(versionStr)
        if (!m.matches()) {
            throw new IllegalStateException("Version '${versionStr}' does not match the expected pattern")
        }

        new Version(major: m.group(2).toInteger(),
                    minor: m.group(4)?.toInteger() ?: 0,
                    patch: m.group(6)?.toInteger() ?: 0,
                    snapshot: m.group(7) ? true : false
        )
    }

    static Version from(Path versionablePath) {
        String versionStr = versionablePath.toString().split('/').find { it.toString() ==~ VERSION_PATTERN }
        from(versionStr)
    }

    static Version from(AssetFile versionableAssetFile) {
        String versionStr = versionableAssetFile.path.toString().split('/').find { it.toString() ==~ VERSION_PATTERN }
        from(versionStr)
    }

    static boolean isVersionable(String possibleVersion) {
        possibleVersion ==~ VERSION_PATTERN
    }

    static boolean isVersionable(Path possibleVersionedPath) {
        possibleVersionedPath.toString().split('/').any { it.toString() ==~ VERSION_PATTERN }
    }

    static boolean isVersionable(AssetFile possibleVersionedAssetFile) {
        possibleVersionedAssetFile.path.split('/').any { it.toString() ==~ VERSION_PATTERN }
    }
}
