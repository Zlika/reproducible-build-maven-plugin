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
import java.io.IOException;
import java.nio.file.Files;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link ZipStripper}.
 */
public class ZipStripperTest
{
    /**
     * Tests stripping on a reference JAR file.
     * @throws IOException
     */
    @Test
    public void testStripZip() throws IOException
    {
        final String testJarName = "test-jar.jar";
        final String strippedJarName = "test-jar-stripped.jar";
        
        final File inFile = new File(this.getClass().getResource(testJarName).getFile());
        final File outFile = File.createTempFile("test-jar", null);
        outFile.deleteOnExit();
        final File expected = new File(this.getClass().getResource(strippedJarName).getFile());
        
        new ZipStripper()
            .addFileStripper("META-INF/MANIFEST.MF", new ManifestStripper())
            .addFileStripper("META-INF/\\S*/pom.properties", new PomPropertiesStripper())
            .strip(inFile, outFile);

        Assert.assertArrayEquals(Files.readAllBytes(expected.toPath()), Files.readAllBytes(outFile.toPath()));
        outFile.delete();
    }
}
