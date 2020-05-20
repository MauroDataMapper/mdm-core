package uk.ac.ox.softeng.maurodatamapper.core.session


import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.ResourcelessMdmController

class SessionController implements ResourcelessMdmController {

    SessionService sessionService

    static responseFormats = ['json', 'xml']

    def activeSessions() {
        HashMap activeSessions = sessionService.getActiveSessionMap(servletContext)
        respond view: '/session/activeSessions', [httpSessionCollection: activeSessions.values()]
    }

    def isAuthenticatedSession() {
        respond authenticatedSession: sessionService.isAuthenticatedSession(session)
    }
}
