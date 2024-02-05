/*
 * Copyright 2020-2024 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.core.session

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException

import javax.servlet.ServletContext
import javax.servlet.http.HttpSession
import javax.servlet.http.HttpSessionEvent
import javax.servlet.http.HttpSessionListener

/**
 * List to all sessions being created and destroyed and make sure they're in the active list
 * This way we can see all users, both logged in and not logged in
 */
class SessionService implements HttpSessionListener {
    public static final String CONTEXT_PROPERTY_NAME = 'activeSessionMap'

    ServletContext servletContext

    HttpSession setUserEmailAddress(HttpSession session, String emailAddress) {
        if (isInvalidatedSession(session)) throw new ApiUnauthorizedException('SSXX', 'Session has been invalidated')
        session.setAttribute('emailAddress', emailAddress)
        storeSession(session)
    }

    HttpSession setUserName(HttpSession session, String firstName, String lastName) {
        if (isInvalidatedSession(session)) throw new ApiUnauthorizedException('SSXX', 'Session has been invalidated')
        session.setAttribute('userName', firstName + ' ' + lastName)
        storeSession(session)
    }

    HttpSession setUserOrganisation(HttpSession session, String organisation) {
        if (isInvalidatedSession(session)) throw new ApiUnauthorizedException('SSXX', 'Session has been invalidated')
        session.setAttribute('userOrganisation', organisation)
        storeSession(session)
    }

    HttpSession setLastAccessedUrl(HttpSession session, String lastAccessedUrl) {
        if (isInvalidatedSession(session)) throw new ApiUnauthorizedException('SSXX', 'Session has been invalidated')
        session.setAttribute('lastUrl', lastAccessedUrl)
        storeSession(session)
    }

    boolean isAuthenticatedSession(String sessionId) {
        if (isInvalidatedSession(sessionId)) return false
        retrieveSession(sessionId).getAttribute('emailAddress')
    }

    boolean isInvalidatedSession(HttpSession session) {
        isInvalidatedSession(session.id)
    }

    boolean isInvalidatedSession(String sessionId) {
        !isSessionStored(sessionId)
    }

    String getSessionEmailAddress(HttpSession session) {
        session.getAttribute('emailAddress')
    }

    void destroySession(HttpSession session) {
        session.invalidate()
        removeSession(session.id)
    }

    /**
     * Will store or update an existing stored version
     *
     * @param session
     */
    HttpSession storeSession(HttpSession session) {
        getActiveSessionMap()[session.id] = session
    }

    /**
     * Retrieve the stored session from the activeSession map. Will probably only be used by admin services as all controllers and interceptors
     * have the current session available
     * @param session
     * @return
     */
    HttpSession retrieveSession(HttpSession session) {
        retrieveSession(session.id)
    }

    HttpSession retrieveSession(String id) {
        getActiveSessionMap()[id]
    }

    void removeSession(String id) {
        getActiveSessionMap().remove(id)
    }

    boolean isSessionStored(String id) {
        getActiveSessionMap().containsKey(id)
    }

    /**
     * Create a map to store the active sessions and add this service as a listener.
     * Should be called in bootstrap
     * @param servletContext
     */
    void initialiseToContext() {
        servletContext.setAttribute(CONTEXT_PROPERTY_NAME, new HashMap<String, HttpSession>())
    }

    HashMap<String, HttpSession> getActiveSessionMap() {
        servletContext.getAttribute(CONTEXT_PROPERTY_NAME) as HashMap<String, HttpSession>
    }

    @Override
    void sessionCreated(HttpSessionEvent event) {
        // Make sure all sessions created are stored even if they're for unlogged in users
        storeSession(event.session)
    }

    @Override
    void sessionDestroyed(HttpSessionEvent event) {
        // This will be called by either our destroySession or by the servlet context auto destroying a session
        removeSession(event.session.id)
    }
}
