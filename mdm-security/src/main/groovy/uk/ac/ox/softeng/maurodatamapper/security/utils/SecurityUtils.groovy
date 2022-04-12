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
package uk.ac.ox.softeng.maurodatamapper.security.utils

import org.apache.commons.text.RandomStringGenerator

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class SecurityUtils {

    static String generateRandomPassword() {
        RandomStringGenerator gen = SecureRandomStringGenerator.alphanumericGenerator()
        "${gen.generate(3)}-${gen.generate(3)}-${gen.generate(3)}-${gen.generate(3)}"
    }

    static String normaliseEmailAddress(String emailAddress) {
        return emailAddress?.trim()?.toLowerCase()
    }

    static byte[] getHash(String password, byte[] salt) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        if (!password) return new byte[0]
        MessageDigest digest = MessageDigest.getInstance('SHA-256')
        digest.reset()
        digest.update(salt)
        return digest.digest(password.trim().getBytes('UTF-8'))
    }

    static String saltPassword(String password, byte[] salt) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        new String(getHash(password, salt), 'UTF-8')
    }
}
