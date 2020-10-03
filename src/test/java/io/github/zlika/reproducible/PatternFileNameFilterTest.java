/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.zlika.reproducible;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import org.apache.maven.monitor.logging.DefaultLog;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the PatternFileNameFilter class.
 */

public class PatternFileNameFilterTest
{
    private static final DefaultLog LOG = new DefaultLog(new ConsoleLogger());

    /**
     * A jar file is trivially included.
     */

    @Test
    public void testJarIsIncluded0()
    {
        Assert.assertTrue(
                PatternFileNameFilter.of(
                        LOG,
                        Arrays.asList(".*"),
                        Collections.emptyList(),
                        Arrays.asList(".jar")
                ).accept(new File(""), "one.jar")
        );
    }

    /**
     * A zip file is not included because it has the wrong extension.
     */

    @Test
    public void testJarWrongExtension()
    {
        Assert.assertFalse(
                PatternFileNameFilter.of(
                        LOG,
                        Arrays.asList(".*"),
                        Collections.emptyList(),
                        Arrays.asList(".jar")
                ).accept(new File(""), "one.zip")
        );
    }

    /**
     * A jar file is not included because it is excluded by a pattern.
     */

    @Test
    public void testJarIsExcluded0()
    {
        Assert.assertFalse(
                PatternFileNameFilter.of(
                        LOG,
                        Arrays.asList(".*"),
                        Arrays.asList("one\\.jar"),
                        Arrays.asList(".jar")
                ).accept(new File(""), "one.jar")
        );
    }

    /**
     * A jar file is not included because no inclusion pattern matches it.
     */

    @Test
    public void testJarIsNotIncluded0()
    {
        Assert.assertFalse(
                PatternFileNameFilter.of(
                        LOG,
                        Arrays.asList(""),
                        Collections.emptyList(),
                        Arrays.asList(".jar")
                ).accept(new File(""), "one.jar")
        );
    }

    /**
     * A jar file is included because no exclusion pattern matches it.
     */

    @Test
    public void testJarIsNotExcluded0()
    {
        Assert.assertTrue(
                PatternFileNameFilter.of(
                        LOG,
                        Arrays.asList(".*"),
                        Arrays.asList("one\\.jar"),
                        Arrays.asList(".jar")
                ).accept(new File(""), "two.jar")
        );
    }

    /**
     * Nonsense.
     */

    @Test
    public void testNonsensePatterns()
    {
        Assert.assertFalse(
                PatternFileNameFilter.of(
                        LOG,
                        Arrays.asList("  "),
                        Arrays.asList("  "),
                        Arrays.asList(".jar")
                ).accept(new File(""), "one.jar")
        );
    }
}
