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
package uk.ac.ox.softeng.maurodatamapper.core.admin

/**
 * Created by james on 22/04/2017.
 */
enum ApiPropertyEnum {
    SITE_URL('site.url'),
    EMAIL_ADMIN_REGISTER_BODY('email.admin_register.body'),
    EMAIL_ADMIN_REGISTER_SUBJECT('email.admin_register.subject'),
    EMAIL_SELF_REGISTER_BODY('email.self_register.body'),
    EMAIL_SELF_REGISTER_SUBJECT('email.self_register.subject'),
    EMAIL_ADMIN_CONFIRM_REGISTRATION_BODY('email.admin_confirm_registration.body'),
    EMAIL_ADMIN_CONFIRM_REGISTRATION_SUBJECT('email.admin_confirm_registration.subject'),
    EMAIL_INVITE_VIEW_SUBJECT('email.invite_view.subject'),
    EMAIL_INVITE_VIEW_BODY('email.invite_view.body'),
    EMAIL_INVITE_EDIT_SUBJECT('email.invite_edit.subject'),
    EMAIL_INVITE_EDIT_BODY('email.invite_edit.body'),
    EMAIL_FROM_ADDRESS('email.from.address'),
    EMAIL_FROM_NAME('email.from.name'),
    EMAIL_FORGOTTEN_PASSWORD_SUBJECT('email.forgotten_password.subject'),
    EMAIL_FORGOTTEN_PASSWORD_BODY('email.forgotten_password.body'),
    EMAIL_PASSWORD_RESET_SUBJECT('email.password_reset.subject'),
    EMAIL_PASSWORD_RESET_BODY('email.password_reset.body'),
    SECURITY_RESTRICT_ROOT_FOLDER('security.restrict.root.folder'),
    SECURITY_RESTRICT_CLASSIFIER_CREATE('security.restrict.classifier.create'),
    SECURITY_HIDE_EXCEPTIONS('security.hide.exception')

    String key

    ApiPropertyEnum(String key) {
        this.key = key
    }

    String toString() {
        key
    }

    static ApiPropertyEnum findApiProperty(String key) {
        values().find {it.key == key}
    }

}
