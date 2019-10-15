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
import java.time.LocalDateTime;
import java.time.ZoneOffset;

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

        final LocalDateTime dateTime = LocalDateTime.now();
        new TarStripper(dateTime).strip(original, stripped);

        final TarArchiveEntry[] entries = new TarFile(stripped).entries();
        Assert.assertEquals(8, entries.length);
        for (final TarArchiveEntry entry : entries)
        {
            final String name = entry.getName();
            Assert.assertEquals(name + " user id", 0L, entry.getLongUserId());
            Assert.assertEquals(name + " user name", "", entry.getUserName());
            Assert.assertEquals(name + " group id", 0L, entry.getLongGroupId());
            Assert.assertEquals(name + " group name", "", entry.getGroupName());
            // TAR timestamps have 1s accuracy
            final long expectedTimestamp = (dateTime.toInstant(ZoneOffset.UTC).toEpochMilli() / 1000) * 1000;
            Assert.assertEquals(name + " modified time", expectedTimestamp, entry.getModTime().getTime());
        }
        Assert.assertFalse(
            "Original tar should not match the stripped tar",
            entries.equals(new TarFile(original).entries())
        );
    }

}
