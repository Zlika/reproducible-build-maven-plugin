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
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.compress.archivers.zip.X5455_ExtendedTimestamp;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipExtraField;
import org.apache.commons.compress.archivers.zip.ZipFile;

/**
 * Strips non-reproducible data from a ZIP file.
 * It rebuilds the ZIP file with a predictable order for the zip entries and sets zip entry dates to a fixed value.
 */
final class ZipStripper implements Stripper
{
    private final Map<String, Stripper> subFilters = new HashMap<>();
    
    /**
     * Adds a stripper for a given file in the Zip.
     * @param filename the name of the file in the Zip.
     * @param stripper the stripper to apply on the file.
     * @return this object (for method chaining).
     */
    public ZipStripper addFileStripper(String filename, Stripper stripper)
    {
        subFilters.put(filename, stripper);
        return this;
    }
    
    @Override
    public void strip(File in, File out) throws IOException
    {
        try (final ZipFile zip = new ZipFile(in);
             final ZipArchiveOutputStream zout = new ZipArchiveOutputStream(out))
        {
            final List<String> sortedNames = sortEntriesByName(zip.getEntries());
            for (String name : sortedNames)
            {
                final ZipArchiveEntry entry = zip.getEntry(name);
                // Strip Zip entry
                final ZipArchiveEntry strippedEntry = filterZipEntry(entry);
                // Strip file if required
                final Stripper stripper = subFilters.get(name);
                if (stripper != null)
                {
                    // Unzip entry to temp file
                    final File tmp = File.createTempFile("tmp", null);
                    tmp.deleteOnExit();
                    Files.copy(zip.getInputStream(entry), tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    final File tmp2 = File.createTempFile("tmp", null);
                    tmp2.deleteOnExit();
                    stripper.strip(tmp, tmp2);
                    final byte[] fileContent = Files.readAllBytes(tmp2.toPath());
                    strippedEntry.setSize(fileContent.length);
                    zout.putArchiveEntry(strippedEntry);
                    zout.write(fileContent);
                    zout.closeArchiveEntry();
                }
                else
                {
                    // Copy the Zip entry as-is
                    zout.addRawArchiveEntry(strippedEntry, getRawInputStream(zip, entry));
                }
            }
        }
    }
    
    private InputStream getRawInputStream(ZipFile zip, ZipArchiveEntry ze) throws IOException
    {
        try
        {
            // Call ZipFile.getRawInputStream(ZipArchiveEntry) by reflection
            // because it is a private method but we need it!
            final Method method = zip.getClass().getDeclaredMethod("getRawInputStream", ZipArchiveEntry.class);
            method.setAccessible(true);
            return (InputStream) method.invoke(zip, ze);
        }
        catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e)
        {
            throw new IOException(e);
        }
    }
    
    private List<String> sortEntriesByName(Enumeration<ZipArchiveEntry> entries)
    {
        return Collections.list(entries).stream()
                .map(e -> e.getName())
                .sorted()
                .collect(Collectors.toList());
    }
    
    private ZipArchiveEntry filterZipEntry(ZipArchiveEntry entry)
    {
        // Set times
        entry.setCreationTime(FileTime.fromMillis(0));
        entry.setLastAccessTime(FileTime.fromMillis(0));
        entry.setLastModifiedTime(FileTime.fromMillis(0));
        entry.setTime(0);
        // Remove extended timestamps
        for (ZipExtraField field : entry.getExtraFields())
        {
            if (field instanceof X5455_ExtendedTimestamp)
            {
                entry.removeExtraField(field.getHeaderId());
            }
        }
        return entry;
    }
}
