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
package uk.ac.ox.softeng.maurodatamapper.util

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import grails.config.Config
import grails.core.GrailsApplication
import grails.core.GrailsClass
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.core.artefact.DomainClassArtefactHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.lang.management.ManagementFactory
import java.lang.management.RuntimeMXBean
import java.nio.charset.Charset
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

/**
 * @since 15/03/2018
 */
@Slf4j
@CompileStatic
@SuppressFBWarnings('DCN_NULLPOINTER_EXCEPTION')
class Utils {

    public static final int UUID_CHARACTER_LENGTH = 36

    private Utils() {
    }

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

        logger.warn('Running with JVM args : {}', arguments.size())
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

    /**
     * Utility method to handle the issue where we need a map out of the grails config but they deprecated the navigable map.
     */
    static Map<String, Object> getMapFromConfig(Config config, String keyPrefix) {
        cleanPrefixFromMap(
            config.findAll {it.key.startsWith(keyPrefix) && !(it.value instanceof Map)}, keyPrefix
        )
    }

    static Map<String, Object> cleanPrefixFromMap(Map<String, Object> map, String prefix) {
        map.findAll {it.key.startsWith(prefix)}
            .collectEntries {k, v -> [k.replace(/$prefix./, ''), v]} as Map<String, Object>
    }

    static byte[] copyOf(byte[] contents) {
        Arrays.copyOf(contents, contents.size())
    }

    static String safeUrlEncode(String value) {
        // URLEncoder converts spaces to + which we dont allow
        String encoded = URLEncoder.encode(value, Charset.defaultCharset())
        encoded.replaceAll(/\+/, '%20')
    }

    static String safeUrlDecode(String value) {
        try {
            // To allow our paths to contain the legitimate + character we do NOT allow it to be used as a url encoded "space"
            URLDecoder.decode(value.replaceAll(/\+/, '%2b'), Charset.defaultCharset())
        } catch (IllegalArgumentException | NullPointerException ignored) {
            value
        }
    }

    static List<UUID> gatherIds(Collection<UUID>... ids) {
        ids.collectMany {it}.findAll()
    }

    static void shutdownAndAwaitTermination(ExecutorService executorService, long timeout, TimeUnit timeUnit) {
        executorService.shutdown() // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!executorService.awaitTermination(timeout, timeUnit)) {
                executorService.shutdownNow() // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!executorService.awaitTermination(timeout, timeUnit))
                    log.error("Pool did not terminate")
            }
        } catch (InterruptedException ex) {
            // (Re-)Cancel if current thread also interrupted
            executorService.shutdownNow()
            // Preserve interrupt status
            Thread.currentThread().interrupt()
        }
    }
}
