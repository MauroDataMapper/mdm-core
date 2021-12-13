/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
import groovy.util.logging.Slf4j
import org.grails.core.artefact.DomainClassArtefactHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.lang.management.ManagementFactory
import java.lang.management.RuntimeMXBean
import java.time.Duration

/**
 * @since 15/03/2018
 */
@Slf4j
@CompileStatic
class Utils {

    static String timeTaken(long start) {
        getTimeString(System.currentTimeMillis() - start)
    }

    static String getTimeString(long duration) {
        durationToString(Duration.ofMillis(duration))
    }

    static String durationToString(Duration duration) {
        StringBuilder sb = new StringBuilder()

        int hrs = duration.toHoursPart()
        int mins = duration.toMinutesPart()
        int secs = duration.toSecondsPart()
        int ms = duration.toMillisPart()

        if (hrs > 0) {
            sb.append(duration.toHoursPart()).append(' hr')
            if (hrs > 1) sb.append('s')
            sb.append(' ')
        }

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

        sb.toString()
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

    static boolean parentClassIsAssignableFromChild(Class<? extends Object> parentClass, Class<? extends Object> childClass) {
        // Try the standard way of checking and if that fails just check the classLoader which may be different due to reloading
        parentClass.isAssignableFrom(childClass) ?: childClass.classLoader.loadClass(parentClass.name).isAssignableFrom(childClass)
    }

    static void toUuid(Map<String, Object> map, String key) {
        if (map.containsKey(key)) {
            map[key] = toUuid(map[key] as Serializable)
        }
    }

    static UUID toUuid(Serializable id) {
        if (!id) return null
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
        if (!cachedGrailsDomains) loadGrailsDomainsCache(grailsApplication)
        GrailsClass gc = cachedGrailsDomains[lookup]
        if (gc) return gc
        // Just incase theres a secondary load of artefacts we can check the system again
        // Should only happen under test
        log.debug('Reloading grails domain cache')
        loadGrailsDomainsCache(grailsApplication)
        cachedGrailsDomains[lookup]
    }

    private static Map<String, GrailsClass> cachedGrailsDomains = [:]

    private static void loadGrailsDomainsCache(GrailsApplication grailsApplication) {
        grailsApplication.getArtefacts(DomainClassArtefactHandler.TYPE).each {gc ->
            cachedGrailsDomains[gc.getShortName()] = gc
            cachedGrailsDomains[gc.propertyName] = gc
            cachedGrailsDomains[(gc.propertyName + 's')] = gc
            cachedGrailsDomains[(gc.propertyName + 'es')] = gc
            cachedGrailsDomains[(gc.propertyName.replaceFirst(/y$/, 'ies'))] = gc
        }
    }
}
