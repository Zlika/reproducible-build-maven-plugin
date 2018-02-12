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
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for tar Stripper.
 *
 * @author tglman
 * @author unicolet
 *
 */
public class TarStripperTest
{

    /**
     * Tests stripping on a reference Tar file.
     *
     * @throws IOException in case of error on test file operations
     */
    @Test
    public void testStripTar() throws IOException
    {
        final File original = new File(this.getClass().getResource("test-tar.tar").getFile());
        final File stripped = File.createTempFile("test-tar", ".tar");
        stripped.deleteOnExit();
        final File expected = new File(this.getClass().getResource("test-tar-stripped.tar").getFile());

        new TarStripper().strip(original, stripped);

        final TarArchiveEntry[] expectedEntries = new TarFile(expected).entries();
        Assert.assertEquals(8, expectedEntries.length);
        Assert.assertArrayEquals(
            "Stripped tar should match expected tar",
            expectedEntries,
            new TarFile(stripped).entries()
        );
        Assert.assertFalse(
            "Original tar should not match the stripped tar",
            expectedEntries.equals(new TarFile(original).entries())
        );
        for (final TarArchiveEntry entry : expectedEntries)
        {
            Assert.assertEquals("user id", 1000L, entry.getLongUserId());
            Assert.assertEquals("user name", "", entry.getUserName());
            Assert.assertEquals("group id", 1000L, entry.getLongGroupId());
            Assert.assertEquals("group name", "", entry.getGroupName());
            Assert.assertEquals("modified time", 0, entry.getModTime().getTime());
        }
    }

}
