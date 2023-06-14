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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.archivers.ar.ArArchiveOutputStream;
import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;
import org.apache.commons.compress.archivers.cpio.CpioArchiveOutputStream;
import org.apache.commons.compress.archivers.cpio.CpioConstants;
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
public class ArchiveStripperTest
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
        new ArchiveStripper(dateTime).strip(original, stripped);

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

    @Test
    public void testStripCpio() throws Exception
    {
        File original = new File("target/test-classes/test.cpio");
        CpioArchiveOutputStream out = new CpioArchiveOutputStream(new FileOutputStream(original));

        CpioArchiveEntry entry = new CpioArchiveEntry("foo.txt");
        entry.setTime(123456789);
        entry.setUID(1000);
        entry.setGID(1000);
        entry.setMode(CpioConstants.C_ISREG);
        entry.setSize(3);

        out.putArchiveEntry(entry);
        out.write("Foo".getBytes());
        out.closeArchiveEntry();

        entry = new CpioArchiveEntry("bar.txt");
        entry.setTime(123456789);
        entry.setUID(1000);
        entry.setGID(1000);
        entry.setMode(CpioConstants.C_ISREG);
        entry.setSize(3);

        out.putArchiveEntry(entry);
        out.write("Bar".getBytes());
        out.closeArchiveEntry();

        out.close();

        File stripped = File.createTempFile("test", ".cpio");
        stripped.deleteOnExit();
        LocalDateTime dateTime = LocalDateTime.now();
        new ArchiveStripper(dateTime).strip(original, stripped);

        List<String> entries = new ArrayList<>();
        CpioArchiveInputStream in = new CpioArchiveInputStream(new FileInputStream(stripped));
        while ((entry = in.getNextCPIOEntry()) != null)
        {
            String name = entry.getName();
            entries.add(name);
            Assert.assertEquals(name + " user id", 0L, entry.getUID());
            Assert.assertEquals(name + " group id", 0L, entry.getGID());
            // CPIO timestamps have 1s accuracy
            final long expectedTimestamp = dateTime.toInstant(ZoneOffset.UTC).toEpochMilli() / 1000;
            Assert.assertEquals(name + " modified time", expectedTimestamp, entry.getTime());
        }
        Assert.assertEquals("File order", "bar.txt,foo.txt", String.join(",", entries));
    }

    @Test
    public void testStripAr() throws Exception
    {
        File original = new File("target/test-classes/test.ar");
        ArArchiveOutputStream out = new ArArchiveOutputStream(new FileOutputStream(original));

        ArArchiveEntry entry = new ArArchiveEntry("foo.txt", 3, 1000, 1000, 0x644, 123456789);
        out.putArchiveEntry(entry);
        out.write("Foo".getBytes());
        out.closeArchiveEntry();

        entry = new ArArchiveEntry("bar.txt", 3, 1000, 1000, 0x644, 123456789);
        out.putArchiveEntry(entry);
        out.write("Bar".getBytes());
        out.closeArchiveEntry();

        out.close();

        File stripped = File.createTempFile("test", ".ar");
        stripped.deleteOnExit();
        LocalDateTime dateTime = LocalDateTime.now();
        new ArchiveStripper(dateTime).strip(original, stripped);

        List<String> entries = new ArrayList<>();
        ArArchiveInputStream in = new ArArchiveInputStream(new FileInputStream(stripped));
        while ((entry = in.getNextArEntry()) != null)
        {
            String name = entry.getName();
            entries.add(name);
            Assert.assertEquals(name + " user id", 0L, entry.getUserId());
            Assert.assertEquals(name + " group id", 0L, entry.getGroupId());
            final long expectedTimestamp = dateTime.toInstant(ZoneOffset.UTC).toEpochMilli() / 1000;
            Assert.assertEquals(name + " modified time", expectedTimestamp, entry.getLastModified());
        }
        Assert.assertEquals("File order", "bar.txt,foo.txt", String.join(",", entries));
    }
}
