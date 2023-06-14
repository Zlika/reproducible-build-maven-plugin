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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.CompressorException;

/**
 * Strip archives of file dates and users,groups informations that are not reproducible.
 */
public class ArchiveStripper implements Stripper
{
    private final long timestamp;

    /**
     * Constructor.
     * @param reproducibleDateTime the date/time to use in TAR entries.
     */
    public ArchiveStripper(LocalDateTime reproducibleDateTime)
    {
        this.timestamp = reproducibleDateTime.atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    @Override
    public void strip(File in, File out) throws IOException
    {
        Path tmp = Files.createTempDirectory("tmp-" + in.getName());

        try (InputStream is = new BufferedInputStream(new FileInputStream(in));
             OutputStream os = new BufferedOutputStream(new FileOutputStream(out)))
        {
            strip(is, os, tmp);
        }
        catch (ArchiveException | CompressorException e)
        {
            throw new IOException(e);
        }
        finally
        {
            org.codehaus.plexus.util.FileUtils.deleteDirectory(tmp.toFile());
        }
    }

    void strip(InputStream in, OutputStream out, Path tmp)
            throws IOException, ArchiveException, CompressorException
    {
        String format = ArchiveStreamFactory.detect(in);
        try (ArchiveInputStream ain = ArchiveStreamFactory.DEFAULT.createArchiveInputStream(format, in);
             ArchiveOutputStream aout = ArchiveStreamFactory.DEFAULT.createArchiveOutputStream(format, out))
        {
            if (aout instanceof TarArchiveOutputStream)
            {
                ((TarArchiveOutputStream) aout).setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            }
            strip(ain, aout, tmp);
        }
    }

    void strip(ArchiveInputStream ain, ArchiveOutputStream aout, Path tmp) throws IOException
    {
        List<ArchiveEntry> sortedNames = new ArrayList<>();
        ArchiveEntry entry;
        while ((entry = ain.getNextEntry()) != null)
        {
            sortedNames.add(entry);
            File copyTo = new File(tmp.toFile(), entry.getName());
            zipSlipProtection(copyTo, tmp);
            if (entry.isDirectory())
            {
                FileUtils.mkdirs(copyTo);
            }
            else
            {
                File destParent = copyTo.getParentFile();
                FileUtils.mkdirs(destParent);
                Files.copy(ain, copyTo.toPath());
            }
        }

        sortedNames.sort(Comparator.comparing(ArchiveEntry::getName));

        for (ArchiveEntry sortedEntry : sortedNames)
        {
            File copyFrom = new File(tmp.toFile(), sortedEntry.getName());
            if (!sortedEntry.isDirectory())
            {
                byte[] fileContent = Files.readAllBytes(copyFrom.toPath());
                if (sortedEntry instanceof TarArchiveEntry)
                {
                    TarArchiveEntry tarEntry = (TarArchiveEntry) sortedEntry;
                    tarEntry.setSize(fileContent.length);
                }
                else if (sortedEntry instanceof ArArchiveEntry)
                {
                    ArArchiveEntry arEntry = (ArArchiveEntry) sortedEntry;
                    sortedEntry = new ArArchiveEntry(arEntry.getName(), fileContent.length, arEntry.getUserId(),
                            arEntry.getGroupId(), arEntry.getMode(), arEntry.getLastModified());
                }
                else if (sortedEntry instanceof CpioArchiveEntry)
                {
                    CpioArchiveEntry cpioEntry = (CpioArchiveEntry) sortedEntry;
                    cpioEntry.setSize(fileContent.length);
                }
                aout.putArchiveEntry(filterEntry(sortedEntry));
                aout.write(fileContent);
                aout.closeArchiveEntry();
            }
            else
            {
                aout.putArchiveEntry(filterEntry(sortedEntry));
                aout.closeArchiveEntry();
            }
        }
    }

    private ArchiveEntry filterEntry(ArchiveEntry entry)
    {
        if (entry instanceof TarArchiveEntry)
        {
            TarArchiveEntry tarEntry = (TarArchiveEntry) entry;
            tarEntry.setModTime(timestamp);
            tarEntry.setGroupId(0);
            tarEntry.setUserId(0);
            tarEntry.setUserName("");
            tarEntry.setGroupName("");
        }
        else if (entry instanceof ArArchiveEntry)
        {
            ArArchiveEntry arEntry = (ArArchiveEntry) entry;
            return new ArArchiveEntry(arEntry.getName(), arEntry.getSize(), 0, 0, arEntry.getMode(), timestamp / 1000);
        }
        else if (entry instanceof CpioArchiveEntry)
        {
            CpioArchiveEntry cpioEntry = (CpioArchiveEntry) entry;
            cpioEntry.setTime(timestamp / 1000);
            cpioEntry.setUID(0);
            cpioEntry.setGID(0);
        }
        return entry;
    }
    
    /**
     * Protection against 'Zip Slip' vulnerability.
     * This method checks that the file path is located inside the expected folder.
     * @param file the file path to check.
     * @param extractFolder the folder.
     * @throws IOException if a 'Zip Slip' attack is detected.
     */
    private void zipSlipProtection(File file, Path extractFolder) throws IOException
    {
        if (!file.toPath().normalize().startsWith(extractFolder))
        {
            throw new IOException("Bad zip entry");
        }
    }
}
