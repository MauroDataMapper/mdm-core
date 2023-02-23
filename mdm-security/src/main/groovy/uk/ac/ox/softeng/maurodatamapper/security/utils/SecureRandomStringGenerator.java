/**
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
package uk.ac.ox.softeng.maurodatamapper.security.utils;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;
import org.apache.commons.text.CharacterPredicates;
import org.apache.commons.text.RandomStringGenerator;

import java.nio.charset.Charset;

/**
 * @since 07/08/2017
 */
public class SecureRandomStringGenerator {

    private SecureRandomStringGenerator() {
    }

    /**
     * Codepoint for 0.
     */
    public static final Integer MINIMUM_CODEPOINT = 49;
    /**
     * Codepoint for z.
     */
    public static final Integer MAXIMUM_CODEPOINT = 122;

    /**
     * Creates a {@link RandomStringGenerator} which will generate letters and digits ONLY which fall inside the following set
     * 0123456789:;&lt;=&gt;?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\]^_`abcdefghijklmnopqrstuvwxyz.
     *
     * <p>Whilst the full set above includes punctuation, this generator will only return characters which fall in a-z, A-Z &amp; 0-9.
     *
     * @return RandomStringGenerator for generating alphanumeric strings.
     */
    public static RandomStringGenerator alphanumericGenerator() {
        UniformRandomProvider rng = RandomSource.MT.create();
        return new RandomStringGenerator.Builder()
            .withinRange(MINIMUM_CODEPOINT, MAXIMUM_CODEPOINT)
            .usingRandom(rng::nextInt)
            .filteredBy(CharacterPredicates.LETTERS, CharacterPredicates.DIGITS)
            .build();
    }

    /**
     * Creates a {@link RandomStringGenerator} which will generate any character whichs fall inside the following set
     * 0123456789:;&lt;=&gt;?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\]^_`abcdefghijklmnopqrstuvwxyz.
     *
     * @return RandomStringGenerator for generating alphanumeric strings including punctuation.
     */
    public static RandomStringGenerator fullGenerator() {
        UniformRandomProvider rng = RandomSource.MT.create();
        return new RandomStringGenerator.Builder()
            .withinRange(MINIMUM_CODEPOINT, MAXIMUM_CODEPOINT)
            .usingRandom(rng::nextInt)
            .build();
    }

    public static byte[] generateSalt() {
        return fullGenerator().generate(8).getBytes(Charset.defaultCharset());
    }

}
