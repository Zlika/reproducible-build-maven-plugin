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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.codehaus.plexus.util.FileUtils;

/**
 * Strip Tar archives of file dates and users,groups informations that are not reproducible.
 *
 * @author tglman
 *
 */
public class TarStripper implements Stripper
{

    /**
     * Factory that create a new instance of tar input stream, for allow extension for different file compression
     * format.
     *
     * @param in
     * @return
     * @throws FileNotFoundException
     *             if the file as parameter is not found
     * @throws IOException
     *             if there are error reading the file given as parameter
     */
    protected TarArchiveInputStream createInputStream(File in) throws FileNotFoundException, IOException
    {
        return new TarArchiveInputStream(new FileInputStream(in));
    }

    /**
     * Factory that create a new instance of tar output stream, for allow extension for different file compression
     * format.
     *
     *
     * @param out
     * @return
     * @throws FileNotFoundException
     *             if the file as parameter is not found
     * @throws IOException
     *             if there are error reading the file given as parameter
     *
     */
    protected TarArchiveOutputStream createOutputStream(File out) throws FileNotFoundException, IOException
    {
        return new TarArchiveOutputStream(new FileOutputStream(out));
    }

    @Override
    public void strip(File in, File out) throws IOException
    {
        final Path tmp = Files.createTempDirectory("tmp-" + in.getName());

        List<TarArchiveEntry> sortedNames = new ArrayList<>();
        try (final TarArchiveInputStream tar = createInputStream(in))
        {

            TarArchiveEntry entry;
            while ((entry = tar.getNextTarEntry()) != null)
            {
                sortedNames.add(entry);
                File copyTo = new File(tmp.toFile(), entry.getName());
                if (entry.isDirectory())
                {
                    copyTo.mkdirs();
                }
                else
                {
                    File destParent = copyTo.getParentFile();
                    destParent.mkdirs();
                    Files.copy(tar, copyTo.toPath());
                }
            }
        }
        sortedNames = sortTarEntries(sortedNames);
        try (final TarArchiveOutputStream tout = createOutputStream(out))
        {
            for (TarArchiveEntry entry : sortedNames)
            {
                File copyFrom = new File(tmp.toFile(), entry.getName());
                if (!entry.isDirectory())
                {
                    final byte[] fileContent = Files.readAllBytes(copyFrom.toPath());
                    entry.setSize(fileContent.length);
                    tout.putArchiveEntry(filterTarEntry(entry));
                    tout.write(fileContent);
                    tout.closeArchiveEntry();
                }
                else
                {
                    tout.putArchiveEntry(filterTarEntry(entry));
                    tout.closeArchiveEntry();
                }
            }
        }
        FileUtils.deleteDirectory(tmp.toFile());
    }

    private List<TarArchiveEntry> sortTarEntries(List<TarArchiveEntry> sortedNames)
    {
        return sortedNames.stream().sorted((a, b) -> a.getName().compareTo(b.getName()))
                .collect(Collectors.toList());
    }

    private TarArchiveEntry filterTarEntry(TarArchiveEntry entry)
    {
        entry.setModTime(0L);
        entry.setGroupId(0);
        entry.setUserId(0);
        entry.setUserName("");
        entry.setGroupName("");
        if (entry.isDirectory())
        {
            entry.setMode(TarArchiveEntry.DEFAULT_DIR_MODE);
        }
        else
        {
            entry.setMode(TarArchiveEntry.DEFAULT_FILE_MODE);
        }
        return entry;
    }

}
