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
package uk.ac.ox.softeng.maurodatamapper.util

import grails.core.GrailsApplication
import grails.core.GrailsClass
import groovy.transform.CompileStatic
import org.grails.core.artefact.DomainClassArtefactHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.lang.management.ManagementFactory
import java.lang.management.RuntimeMXBean

/**
 * @since 15/03/2018
 */
@CompileStatic
class Utils {

    static String timeTaken(long start) {
        getTimeString(System.currentTimeMillis() - start)
    }

    static String getTimeString(long duration) {
        StringBuilder sb = new StringBuilder()

        long secs = (duration / 1000) as long
        long ms = duration - (secs * 1000)
        long mins = (secs / 60) as long
        secs = secs % 60

        if (mins > 0) {
            sb.append(mins).append(' min')
            if (mins > 1) sb.append('s')
            sb.append(' ')
        }

        if (secs > 0) {
            sb.append(secs).append(' sec')
            if (secs > 1) sb.append('s')
            sb.append(' ')
        }

        sb.append(ms).append(' ms').toString()
    }

    static void outputRuntimeArgs(Class clazz) {
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean()
        List<String> arguments = runtimeMxBean.getInputArguments()
        Logger logger = LoggerFactory.getLogger(clazz)

        logger.warn("Running with JVM args : {}", arguments.size())
        Map<String, String> map = arguments.collectEntries {arg ->
            arg.split('=').toList()
        }.sort() as Map<String, String>

        map.findAll {k, v ->
            k.startsWith('-Denv') ||
            k.startsWith('-Dgrails') ||
            k.startsWith('-Dinfo') ||
            k.startsWith('-Djava.version') ||
            k.startsWith('-Dspring') ||
            k.startsWith('-Duser.timezone') ||
            k.startsWith('-X')
        }.each {k, v ->
            if (v) logger.warn('{}={}', k, v)
            else logger.warn('{}', k)
        }
    }

    static boolean parentClassIsAssignableFromChild(Class parentClass, Class childClass) {
        childClass.classLoader.loadClass(parentClass.name).isAssignableFrom(childClass)
    }

    static void toUuid(Map<String, Object> map, String key) {
        if (map.containsKey(key)) {
            map[key] = toUuid(map[key] as Serializable)
        }
    }

    static UUID toUuid(Serializable id) {
        id instanceof UUID ? id : toUuid(id.toString())
    }

    static UUID toUuid(String id) {
        try {
            return UUID.fromString(id.toString())
        } catch (Exception ignored) {
            return null
        }
    }

    static List<UUID> mergeLists(List<UUID>... lists) {
        new ArrayList<UUID>(lists.flatten().toSet())
    }

    static GrailsClass lookupGrailsDomain(GrailsApplication grailsApplication, String lookup) {
        grailsApplication.getArtefacts(DomainClassArtefactHandler.TYPE).find {
            lookup in [
                it.getPropertyName(),
                it.getPropertyName() + 's',
                it.getPropertyName() + 'es',
                it.getPropertyName().replaceFirst(/y$/, 'ies')
            ] || lookup == it.getShortName()
        }
    }
}
