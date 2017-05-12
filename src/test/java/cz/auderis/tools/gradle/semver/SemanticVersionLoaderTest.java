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

import cz.auderis.test.category.IntegrationTest;
import cz.auderis.test.category.UnitTest;
import cz.auderis.test.rule.WorkFolder;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(JUnitParamsRunner.class)
@Category(UnitTest.class)
public class SemanticVersionLoaderTest {

    @Rule
    public WorkFolder folder = WorkFolder.basic();

    private final static AtomicBoolean handlerDefined = new AtomicBoolean();

    @BeforeClass
    public static void prepareUrlHandler() throws Exception {
        final URLStreamHandler streamHandler = new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL u) throws IOException {
                return new TestUrlConnection(u);
            }
        };
        final URLStreamHandlerFactory factory = new URLStreamHandlerFactory() {
            @Override
            public URLStreamHandler createURLStreamHandler(String protocol) {
                return "testres".equals(protocol) ? streamHandler : null;
            }
        };
        if (handlerDefined.compareAndSet(false, true)) {
            URL.setURLStreamHandlerFactory(factory);
        }
    }


    @Test
    @Category(IntegrationTest.class)
    @Parameters({
            "basic.version      | 2.4.6-SNAPSHOT+Build-4843",
            "whitespace.version | 11.13.17-Beta",
            "comment1.version   | 5.8.13+Fibonacci",
            "comment2.version   | 2.7.18-Euler",
    })
    public void shouldLoadCorrectVersionFromFile(String src, String expectedVersionSpec) throws Exception {
        // Given
        final File srcFile = folder.newResourceCopy(src, "spec/" + src);
        final SemanticVersion expectedVersion = SemanticVersion.is(expectedVersionSpec);

        // When
        final SemanticVersion ver = SemanticVersion.from(srcFile);

        // Then
        assertThat(ver, is(expectedVersion));
    }

    @Test
    @Category(IntegrationTest.class)
    @Parameters({
            "testres:///spec/basic.version      | 2.4.6-SNAPSHOT+Build-4843",
            "testres:///spec/whitespace.version | 11.13.17-Beta",
            "testres:///spec/comment1.version   | 5.8.13+Fibonacci",
            "testres:///spec/comment2.version   | 2.7.18-Euler",
    })
    public void shouldLoadCorrectVersionFromUrl(String url, String expectedVersionSpec) throws Exception {
        final URL srcUrl = new URL(url);
        final SemanticVersion expectedVersion = SemanticVersion.is(expectedVersionSpec);

        // When
        final SemanticVersion ver = SemanticVersion.from(srcUrl);

        // Then
        assertThat(ver, is(expectedVersion));
    }


    static class TestUrlConnection extends URLConnection {
        final String resourcePath;

        TestUrlConnection(URL url) {
            super(url);
            final String path = url.getPath();
            resourcePath = path.startsWith("/") ? path.substring(1) : path;
        }

        @Override
        public void connect() throws IOException {
            final URL resourceURL = getClass().getResource(resourcePath);
            if (null == resourceURL) {
                throw new IOException("Cannot find resource " + resourcePath);
            }
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return getClass().getResourceAsStream(resourcePath);
        }
    }

}
