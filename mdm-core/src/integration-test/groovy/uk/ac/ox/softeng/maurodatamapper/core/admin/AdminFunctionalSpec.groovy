package uk.ac.ox.softeng.maurodatamapper.core.admin

import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.core.type.Argument

import static io.micronaut.http.HttpStatus.OK

/**
 * @see AdminController* Controller: admin
 * |   POST  | /api/admin/editProperties       | Action: editApiProperties    |
 * |   POST  | /api/admin/rebuildLuceneIndexes | Action: rebuildLuceneIndexes |
 * |   GET   | /api/admin/properties           | Action: apiProperties        |
 */
@Integration
@Slf4j
class AdminFunctionalSpec extends BaseFunctionalSpec {

    String getResourcePath() {
        'admin'
    }

    void 'test post to editProperties'() {
        given:
        String expected = '{\n' +
                          '  "email.invite_edit.body": "Dear ${firstName},\\nYou have been invited to edit the model \'${itemLabel}\' in the ' +
                          'Mauro Data Mapper at ' +
                          '${catalogueUrl}\\n\\nYour username / email address is: ${emailAddress}\\nYour password is: ${tempPassword}\\n and you ' +
                          'will be asked to ' +
                          'update this when you first log on.\\nKind regards, the Mauro Data Mapper folks.\\n\\n(This is an automated mail).",' +
                          '\n' +
                          '  "email.admin_register.body": "Dear ${firstName},\\nYou have been given access to the Mauro Data Mapper at ' +
                          '${catalogueUrl} \\n\\nYour ' +
                          'username / email address is: ${emailAddress} \\nYour password is: ${tempPassword} \\nand you will be asked to update ' +
                          'this when you first ' +
                          'log on.\\n\\nKind regards, the Mauro Data Mapper folks. \\n\\n(This is an automated mail).",\n' +
                          '  "email.admin_register.subject": "Mauro Data Mapper Registration",\n' +
                          '  "email.self_register.subject": "Mauro Data Mapper Registration",\n' +
                          '  "email.forgotten_password.subject": "Mauro Data Mapper Forgotten Password",\n' +
                          '  "email.invite_edit.subject": "Mauro Data Mapper Invitation",\n' +
                          '  "email.admin_confirm_registration.body": "Dear ${firstName},\\nYour registration for the Mauro Data Mapper at ' +
                          '${catalogueUrl} has been ' +
                          'confirmed.\\n\\nYour username / email address is: ${emailAddress} \\nYou chose a password on registration, but can reset' +
                          ' it from the login' +
                          ' page.\\n\\nKind regards, the Mauro Data Mapper folks.\\n\\n(This is an automated mail).",\n' +
                          '  "email.invite_view.subject": "Mauro Data Mapper Invitation",\n' +
                          '  "email.from.address": "username@gmail.com",\n' +
                          '  "email.self_register.body": "Dear ${firstName},\\nYou have self-registered for the Mauro Data Mapper at ' +
                          '${catalogueUrl}\\n\\nYour ' +
                          'username / email address is: ${emailAddress}\\nYour registration is marked as pending: you\'ll be sent another email ' +
                          'when your request has' +
                          ' been confirmed by an administrator. \\nKind regards, the Mauro Data Mapper folks.\\n\\n(This is an automated mail)' +
                          '.",\n' +
                          '  "email.password_reset.body": "Dear ${firstName},\\nYour password has been reset for the Mauro Data Mapper at ' +
                          '${catalogueUrl}.\\n\\nYour' +
                          ' new temporary password is: ${tempPassword} \\nand you will be asked to update this when you next log on.\\n\\nKind ' +
                          'regards, the Mauro Data Mapper folks.\\n\\n(This is an automated mail).",\n' +
                          '  "email.from.name": "functional_test",\n' +
                          '  "email.admin_confirm_registration.subject": "Mauro Data Mapper Registration - Confirmation",\n' +
                          '  "email.forgotten_password.body": "Dear ${firstName},\\nA request has been made to reset the password for the ' +
                          'Mauro Data Mapper at ' +
                          '${catalogueUrl}.\\nIf you did not make this request please ignore this email.\\n\\nPlease use the following link to ' +
                          'reset your password ' +
                          '${passwordResetLink}.\\nKind regards, the Mauro Data Mapper folks.\\n\\n(This is an automated mail).",\n' +
                          '  "email.password_reset.subject": "Mauro Data Mapper Password Reset",\n' +
                          '  "email.invite_view.body": "Dear ${firstName},\\nYou have been invited to view the item \'${itemLabel}\' in the ' +
                          'Mauro Data Mapper at ' +
                          '${catalogueUrl}\\n\\nYour username / email address is: ${emailAddress}\\nYour password is: ${tempPassword}\\n and you ' +
                          'will be asked to ' +
                          'update this when you first log on.\\nKind regards, the Mauro Data Mapper folks.\\n\\n(This is an automated mail)."\n' +
                          '}'
        when:
        POST('editProperties', ['email.from.name': 'functional_test'], Argument.of(String))

        then:
        verifyJsonResponse(OK, expected)

        cleanup:
        POST('editProperties', ['email.from.name': 'Mauro Data Mapper'], Argument.of(String))
    }

    void 'test post to rebuildLuceneIndexes'() {
        when:
        POST('rebuildLuceneIndexes', [:], Argument.of(String))

        then:
        verifyJsonResponse(OK, '''{
  "user": "unlogged_user@mdm-core.com",
  "indexed": true,
  "timeTakenMilliseconds": "${json-unit.ignore}",
  "timeTaken": "${json-unit.ignore}"
}''')
    }

    void 'test get properties'() {
        given:
        String expected = '{\n' +
                          '  "email.invite_edit.body": "Dear ${firstName},\\nYou have been invited to edit the model \'${itemLabel}\' in the ' +
                          'Mauro Data Mapper at ' +
                          '${catalogueUrl}\\n\\nYour username / email address is: ${emailAddress}\\nYour password is: ${tempPassword}\\n and you ' +
                          'will be asked to ' +
                          'update this when you first log on.\\nKind regards, the Mauro Data Mapper folks.\\n\\n(This is an automated mail).",' +
                          '\n' +
                          '  "email.admin_register.body": "Dear ${firstName},\\nYou have been given access to the Mauro Data Mapper at ' +
                          '${catalogueUrl} \\n\\nYour ' +
                          'username / email address is: ${emailAddress} \\nYour password is: ${tempPassword} \\nand you will be asked to update ' +
                          'this when you first ' +
                          'log on.\\n\\nKind regards, the Mauro Data Mapper folks. \\n\\n(This is an automated mail).",\n' +
                          '  "email.admin_register.subject": "Mauro Data Mapper Registration",\n' +
                          '  "email.self_register.subject": "Mauro Data Mapper Registration",\n' +
                          '  "email.forgotten_password.subject": "Mauro Data Mapper Forgotten Password",\n' +
                          '  "email.invite_edit.subject": "Mauro Data Mapper Invitation",\n' +
                          '  "email.admin_confirm_registration.body": "Dear ${firstName},\\nYour registration for the Mauro Data Mapper at ' +
                          '${catalogueUrl} has been ' +
                          'confirmed.\\n\\nYour username / email address is: ${emailAddress} \\nYou chose a password on registration, but can reset' +
                          ' it from the login' +
                          ' page.\\n\\nKind regards, the Mauro Data Mapper folks.\\n\\n(This is an automated mail).",\n' +
                          '  "email.invite_view.subject": "Mauro Data Mapper Invitation",\n' +
                          '  "email.from.address": "username@gmail.com",\n' +
                          '  "email.self_register.body": "Dear ${firstName},\\nYou have self-registered for the Mauro Data Mapper at ' +
                          '${catalogueUrl}\\n\\nYour ' +
                          'username / email address is: ${emailAddress}\\nYour registration is marked as pending: you\'ll be sent another email ' +
                          'when your request has' +
                          ' been confirmed by an administrator. \\nKind regards, the Mauro Data Mapper folks.\\n\\n(This is an automated mail)' +
                          '.",\n' +
                          '  "email.password_reset.body": "Dear ${firstName},\\nYour password has been reset for the Mauro Data Mapper at ' +
                          '${catalogueUrl}.\\n\\nYour' +
                          ' new temporary password is: ${tempPassword} \\nand you will be asked to update this when you next log on.\\n\\nKind ' +
                          'regards, the Mauro Data Mapper folks.\\n\\n(This is an automated mail).",\n' +
                          '  "email.from.name": "Mauro Data Mapper",\n' +
                          '  "email.admin_confirm_registration.subject": "Mauro Data Mapper Registration - Confirmation",\n' +
                          '  "email.forgotten_password.body": "Dear ${firstName},\\nA request has been made to reset the password for the ' +
                          'Mauro Data Mapper at ' +
                          '${catalogueUrl}.\\nIf you did not make this request please ignore this email.\\n\\nPlease use the following link to ' +
                          'reset your password ' +
                          '${passwordResetLink}.\\nKind regards, the Mauro Data Mapper folks.\\n\\n(This is an automated mail).",\n' +
                          '  "email.password_reset.subject": "Mauro Data Mapper Password Reset",\n' +
                          '  "email.invite_view.body": "Dear ${firstName},\\nYou have been invited to view the item \'${itemLabel}\' in the ' +
                          'Mauro Data Mapper at ' +
                          '${catalogueUrl}\\n\\nYour username / email address is: ${emailAddress}\\nYour password is: ${tempPassword}\\n and you ' +
                          'will be asked to ' +
                          'update this when you first log on.\\nKind regards, the Mauro Data Mapper folks.\\n\\n(This is an automated mail)."\n' +
                          '}'

        when:
        GET('properties', Argument.of(String))

        then:
        verifyJsonResponse(OK, expected)
    }
}
