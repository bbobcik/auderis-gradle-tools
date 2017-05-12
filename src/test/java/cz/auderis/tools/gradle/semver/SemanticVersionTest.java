/*
 * Copyright 2017 Boleslav Bobcik - Auderis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cz.auderis.tools.gradle.semver;

import cz.auderis.test.category.UnitTest;
import cz.auderis.tools.gradle.semver.SemanticVersion;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.gradle.api.InvalidUserDataException;
import org.hamcrest.Matcher;
import org.hamcrest.number.OrderingComparison;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;

@RunWith(JUnitParamsRunner.class)
@Category(UnitTest.class)
public class SemanticVersionTest {

    @Rule
    public ExpectedException errors = ExpectedException.none();

    @Test
    @Category(UnitTest.class)
    @Parameters({
        "0.0.0           |   0 |   0 |   0",
        "0.1.2           |   0 |   1 |   2",
        "1.5.0           |   1 |   5 |   0",
        "256.256.256     | 256 | 256 | 256",
        "10.4.6-SNAPSHOT |  10 |   4 |   6",
        "1.0.3+BACKUP    |   1 |   0 |   3"
    })
    public void shouldParseCorrectKeyParts(String spec, int major, int minor, int patch) throws Exception {
        final SemanticVersion ver = SemanticVersion.is(spec);
        //
        assertThat(ver, hasProperty("majorVersion", is(major)));
        assertThat(ver, hasProperty("minorVersion", is(minor)));
        assertThat(ver, hasProperty("patchRevision", is(patch)));
    }

    @Test
    @Category(UnitTest.class)
    @Parameters({
            "1",
            "2.3",
            "4..5",
            "01.2.3",
            "4.05.6",
            "7.8.09",
            "10.11.12--bad",
            "1.1.1-.x",
            "1.1.1-x.",
            "2.2.2-x.02",
            "3.3.3+.y",
            "3.3.3+y.",
            "4.4.4+y.03"
    })
    public void shouldDeclineInvalidSpecification(String spec)  throws Exception {
        errors.expect(InvalidUserDataException.class);
        final SemanticVersion ver = SemanticVersion.is(spec);
    }

    @Test
    @Category(UnitTest.class)
    @Parameters({
        "5.4.3",
        "1.0.0-SNAPSHOT",
        "3.1.415-PI+By-Ludolf.Not-rational"
    })
    public void shouldRenderCorrectSpecificationString(String spec) throws Exception {
        final SemanticVersion ver = SemanticVersion.is(spec);
        final String generatedSpec = ver.toString();
        //
        assertThat(generatedSpec, is(spec));
    }

    @Test
    @Parameters({
        "1.2.3 | = | 1.2.3",
        "1.2.3 | < | 1.2.4",
        "1.2.3 | < | 1.3.3",
        "1.2.3 | < | 2.2.3",
        "1.2.3 | = | 1.2.3+Build",
        "1.2.3 | > | 1.2.3-SNAPSHOT",
    })
    @Category(UnitTest.class)
    public void shouldCorrectlyCompareVersions(String spec1, char relation, String spec2) throws Exception {
        // Given
        final SemanticVersion referenceVersion = SemanticVersion.is(spec2);
        final Matcher<? super SemanticVersion> correctlyComparesToReference;
        if ('=' == relation) {
            correctlyComparesToReference = OrderingComparison.comparesEqualTo(referenceVersion);
        } else if ('>' == relation) {
            correctlyComparesToReference = OrderingComparison.greaterThan(referenceVersion);
        } else {
            correctlyComparesToReference = OrderingComparison.lessThan(referenceVersion);
        }

        // When
        final SemanticVersion testedVersion = SemanticVersion.is(spec1);

        // Then
        assertThat(testedVersion, correctlyComparesToReference);
    }

}
