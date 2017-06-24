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
 * Tests for tar Stripper.
 * 
 * @author tglman
 *
 */
public class TarStripperTest
{

    /**
     * Tests stripping on a reference Tar file.
     * 
     * @throws IOException
     *             in case of error on test file operations
     */
    @Test
    public void testStripTar() throws IOException
    {
        final String testTarName = "test-tar.tar";
        final String strippedTarName = "test-tar-stripped.tar";

        final File inFile = new File(this.getClass().getResource(testTarName).getFile());
        final File outFile = File.createTempFile("test-tar", "tar");
        outFile.deleteOnExit();
        final File expected = new File(this.getClass().getResource(strippedTarName).getFile());

        new TarStripper().strip(inFile, outFile);

        Assert.assertArrayEquals(Files.readAllBytes(expected.toPath()), Files.readAllBytes(outFile.toPath()));
        outFile.delete();
    }

}
