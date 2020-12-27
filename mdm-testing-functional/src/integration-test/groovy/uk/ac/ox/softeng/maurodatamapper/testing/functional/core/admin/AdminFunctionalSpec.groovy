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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.core.admin

import uk.ac.ox.softeng.maurodatamapper.testing.functional.FunctionalSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import spock.lang.Unroll

import static io.micronaut.http.HttpStatus.OK

/**
 * <pre>
 * Controller: admin
 *  |  GET   | /api/admin/status                | Action: status
 *  |  POST  | /api/admin/editProperties        | Action: editApiProperties
 *  |  POST  | /api/admin/rebuildLuceneIndexes  | Action: rebuildLuceneIndexes
 *  |  GET   | /api/admin/properties            | Action: apiProperties
 *  </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.admin.AdminController
 */
@Integration
@Slf4j
class AdminFunctionalSpec extends FunctionalSpec {

    String getResourcePath() {
        'admin'
    }

    @Unroll
    void '#method:#endpoint endpoint are admin access only'() {
        when: 'Unlogged in call to check'
        if (args != null) this."$method"(endpoint, args)
        else this."$method"(endpoint)

        then: 'The response is Unauth'
        verifyForbidden response

        when: 'logged in as normal user'
        loginAuthenticated()
        if (args != null) this."$method"(endpoint, args)
        else this."$method"(endpoint)

        then: 'The response is Unauth'
        verifyForbidden response

        where:
        method | endpoint               | args
        'GET'  | 'status'               | null
        'POST' | 'editProperties'       | [:]
        'POST' | 'rebuildLuceneIndexes' | [:]
        'GET'  | 'properties'           | null
    }

    @Unroll
    void '#method:#endpoint endpoint when logged in as admin user'() {
        when: 'logged in as admin'
        loginAdmin()
        if (args != null) this."$method"(endpoint, args, STRING_ARG)
        else this."$method"(endpoint, STRING_ARG)

        then: 'The response is Unauth'
        verifyJsonResponse responseCode, expectedJson

        cleanup:
        POST('editProperties', ["site.url": ''])
        verifyResponse(OK, response)

        where:
        method | endpoint               | args                                       || responseCode | expectedJson
        'GET'  | 'status'               | null                                       || OK           | '''
{
  "Mauro Data Mapper Version": "${json-unit.matches:version}",
  "Grails Version": "4.0.6",
  "Java Version": "12.0.2",
  "Java Vendor": "${json-unit.any-string}",
  "OS Name": "${json-unit.any-string}",
  "OS Version": "${json-unit.matches:version}",
  "OS Architecture": "${json-unit.any-string}",
  "Driver Manager Drivers Available": [
    {
      "class": "org.h2.Driver",
      "version": "1.4"
    },
    {
      "class": "org.postgresql.Driver",
      "version": "42.2"
    }
  ]
}
'''

        'POST' | 'rebuildLuceneIndexes' | [:]                                        || OK           | '''{
  "user": "admin@maurodatamapper.com",
  "indexed": true,
  "timeTakenMilliseconds": "${json-unit.ignore}",
  "timeTaken": "${json-unit.ignore}"
}'''
        'GET'  | 'properties'           | null                                       || OK           | '{\n' +
        '  "email.invite_edit.body": "Dear ${firstName},\\nYou have been invited to edit the model \'${itemLabel}\' in the Mauro Data Mapper at ' +
        '${catalogueUrl}\\n\\nYour username / email address is: ${emailAddress}\\nYour password is: ${tempPassword}\\n and you will be asked to ' +
        'update this when you first log on.\\nKind regards, the Mauro Data Mapper folks.\\n\\n(This is an automated mail).",\n' +
        '  "email.admin_register.body": "Dear ${firstName},\\nYou have been given access to the Mauro Data Mapper at ${catalogueUrl} \\n\\nYour ' +
        'username / email address is: ${emailAddress} \\nYour password is: ${tempPassword} \\nand you will be asked to update this when you first ' +
        'log on.\\n\\nKind regards, the Mauro Data Mapper folks. \\n\\n(This is an automated mail).",\n' +
        '  "email.admin_register.subject": "Mauro Data Mapper Registration",\n' +
        '  "email.self_register.subject": "Mauro Data Mapper Registration",\n' +
        '  "email.forgotten_password.subject": "Mauro Data Mapper Forgotten Password",\n' +
        '  "email.invite_edit.subject": "Mauro Data Mapper Invitation",\n' +
        '  "email.admin_confirm_registration.body": "Dear ${firstName},\\nYour registration for the Mauro Data Mapper at ${catalogueUrl} has been ' +
        'confirmed.\\n\\nYour username / email address is: ${emailAddress} \\nYou chose a password on registration, but can reset it from the login' +
        ' page.\\n\\nKind regards, the Mauro Data Mapper folks.\\n\\n(This is an automated mail).",\n' +
        '  "email.invite_view.subject": "Mauro Data Mapper Invitation",\n' +
        '  "email.from.address": "username@gmail.com",\n' +
        '  "email.self_register.body": "Dear ${firstName},\\nYou have self-registered for the Mauro Data Mapper at ${catalogueUrl}\\n\\nYour ' +
        'username / email address is: ${emailAddress}\\nYour registration is marked as pending: you\'ll be sent another email when your request has' +
        ' been confirmed by an administrator. \\nKind regards, the Mauro Data Mapper folks.\\n\\n(This is an automated mail).",\n' +
        '  "email.password_reset.body": "Dear ${firstName},\\nYour password has been reset for the Mauro Data Mapper at ${catalogueUrl}.\\n\\nYour' +
        ' new temporary password is: ${tempPassword} \\nand you will be asked to update this when you next log on.\\n\\nKind regards, the Mauro ' +
        'Data Mapper folks.\\n\\n(This is an automated mail).",\n' +
        '  "email.from.name": "Mauro Data Mapper",\n' +
        '  "email.admin_confirm_registration.subject": "Mauro Data Mapper Registration - Confirmation",\n' +
        '  "email.forgotten_password.body": "Dear ${firstName},\\nA request has been made to reset the password for the Mauro Data Mapper at ' +
        '${catalogueUrl}.\\nIf you did not make this request please ignore this email.\\n\\nPlease use the following link to reset your password ' +
        '${passwordResetLink}.\\nKind regards, the Mauro Data Mapper folks.\\n\\n(This is an automated mail).",\n' +
        '  "email.password_reset.subject": "Mauro Data Mapper Password Reset",\n' +
        '  "email.invite_view.body": "Dear ${firstName},\\nYou have been invited to view the item \'${itemLabel}\' in the Mauro Data Mapper at ' +
        '${catalogueUrl}\\n\\nYour username / email address is: ${emailAddress}\\nYour password is: ${tempPassword}\\n and you will be asked to ' +
        'update this when you first log on.\\nKind regards, the Mauro Data Mapper folks.\\n\\n(This is an automated mail)."\n' +
        '}'
        'POST' | 'editProperties'       | ["site.url": "http://functional-test.com"] || OK           | '{\n' +
        '  "email.invite_edit.body": "Dear ${firstName},\\nYou have been invited to edit the model \'${itemLabel}\' in the Mauro Data Mapper at ' +
        '${catalogueUrl}\\n\\nYour username / email address is: ${emailAddress}\\nYour password is: ${tempPassword}\\n and you will be asked to ' +
        'update this when you first log on.\\nKind regards, the Mauro Data Mapper folks.\\n\\n(This is an automated mail).",\n' +
        '  "email.admin_register.body": "Dear ${firstName},\\nYou have been given access to the Mauro Data Mapper at ${catalogueUrl} \\n\\nYour ' +
        'username / email address is: ${emailAddress} \\nYour password is: ${tempPassword} \\nand you will be asked to update this when you first ' +
        'log on.\\n\\nKind regards, the Mauro Data Mapper folks. \\n\\n(This is an automated mail).",\n' +
        '  "email.admin_register.subject": "Mauro Data Mapper Registration",\n' +
        '  "email.self_register.subject": "Mauro Data Mapper Registration",\n' +
        '  "email.forgotten_password.subject": "Mauro Data Mapper Forgotten Password",\n' +
        '  "email.invite_edit.subject": "Mauro Data Mapper Invitation",\n' +
        '  "email.admin_confirm_registration.body": "Dear ${firstName},\\nYour registration for the Mauro Data Mapper at ${catalogueUrl} has been ' +
        'confirmed.\\n\\nYour username / email address is: ${emailAddress} \\nYou chose a password on registration, but can reset it from the login' +
        ' page.\\n\\nKind regards, the Mauro Data Mapper folks.\\n\\n(This is an automated mail).",\n' +
        '  "email.invite_view.subject": "Mauro Data Mapper Invitation",\n' +
        '  "email.from.address": "username@gmail.com",\n' +
        '  "email.self_register.body": "Dear ${firstName},\\nYou have self-registered for the Mauro Data Mapper at ${catalogueUrl}\\n\\nYour ' +
        'username / email address is: ${emailAddress}\\nYour registration is marked as pending: you\'ll be sent another email when your request has' +
        ' been confirmed by an administrator. \\nKind regards, the Mauro Data Mapper folks.\\n\\n(This is an automated mail).",\n' +
        '  "email.password_reset.body": "Dear ${firstName},\\nYour password has been reset for the Mauro Data Mapper at ${catalogueUrl}.\\n\\nYour' +
        ' new temporary password is: ${tempPassword} \\nand you will be asked to update this when you next log on.\\n\\nKind regards, the Mauro ' +
        'Data Mapper folks.\\n\\n(This is an automated mail).",\n' +
        '  "email.from.name": "Mauro Data Mapper",\n' +
        '  "site.url": "http://functional-test.com",\n' +
        '  "email.admin_confirm_registration.subject": "Mauro Data Mapper Registration - Confirmation",\n' +
        '  "email.forgotten_password.body": "Dear ${firstName},\\nA request has been made to reset the password for the Mauro Data Mapper at ' +
        '${catalogueUrl}.\\nIf you did not make this request please ignore this email.\\n\\nPlease use the following link to reset your password ' +
        '${passwordResetLink}.\\nKind regards, the Mauro Data Mapper folks.\\n\\n(This is an automated mail).",\n' +
        '  "email.password_reset.subject": "Mauro Data Mapper Password Reset",\n' +
        '  "email.invite_view.body": "Dear ${firstName},\\nYou have been invited to view the item \'${itemLabel}\' in the Mauro Data Mapper at ' +
        '${catalogueUrl}\\n\\nYour username / email address is: ${emailAddress}\\nYour password is: ${tempPassword}\\n and you will be asked to ' +
        'update this when you first log on.\\nKind regards, the Mauro Data Mapper folks.\\n\\n(This is an automated mail)."\n' +
        '}'
    }
}
