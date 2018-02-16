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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

/**
 * Commodity class to handle a Tar archive file in tests.
 * @author Umberto Nicoletti (umberto.nicoletti@gmail.com)
 */
final class TarFile
{
    /**
     * Tar archive entries list.
     */
    private final List<TarArchiveEntry> entryList;

    /**
     * Ctor.
     * @param file A file pointing to a tar archive.
     */
    public TarFile(final File file)
    {
        this.entryList = TarFile.getAllEntries(file);
    }

    /**
     * Return the entries of the archive.
     * @return An array with all the entries of an archive.
     */
    public TarArchiveEntry[] entries()
    {
        return this.entryList.toArray(new TarArchiveEntry[0]);
    }

    /**
     * Extract all entries from tar archive.
     * @param file The file representing the tar archive.
     * @return A list of tar archive entries.
     */
    private static List<TarArchiveEntry> getAllEntries(final File file)
    {
        final List<TarArchiveEntry> result = new ArrayList<>(1);
        try (TarArchiveInputStream tar = new TarArchiveInputStream(new FileInputStream(file)))
        {
            while (tar.getNextTarEntry() != null)
            {
                result.add(tar.getCurrentEntry());
            }
        } catch (final IOException fne)
        {
            throw new IllegalArgumentException(
                String.format(
                    "Error reading file %s: %s",
                    file.getAbsolutePath(),
                    fne.getMessage()
                )
            );
        }
        return result;
    }
}
