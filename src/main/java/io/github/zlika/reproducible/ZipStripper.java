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
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
public final class ZipStripper implements Stripper
{
    // The ZipArchiveEntry.setXxxTime() methods write the time taking into account the local time zone,
    // so we must first convert the desired timestamp value in the local time zone to have the
    // same timestamps in the ZIP file when the project is built on another computer in a
    // different time zone.
    private static final long DEFAULT_ZIP_TIMESTAMP
                = LocalDateTime.of(2000, 1, 1, 0, 0, 0, 0).atZone(ZoneOffset.systemDefault())
                    .toInstant().toEpochMilli();
    
    private final Map<String, Stripper> subFilters = new HashMap<>();
    
    /**
     * Adds a stripper for a given file in the Zip.
     * @param filename the name of the file in the Zip (regular expression).
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
                final Stripper stripper = getSubFilter(name);
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
                    zout.addRawArchiveEntry(strippedEntry, zip.getRawInputStream(entry));
                }
            }
        }
    }
    
    private Stripper getSubFilter(String name)
    {
        for (Entry<String, Stripper> filter : subFilters.entrySet())
        {
            if (name.matches(filter.getKey()))
            {
                return filter.getValue();
            }
        }
        return null;
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
        entry.setCreationTime(FileTime.fromMillis(DEFAULT_ZIP_TIMESTAMP));
        entry.setLastAccessTime(FileTime.fromMillis(DEFAULT_ZIP_TIMESTAMP));
        entry.setLastModifiedTime(FileTime.fromMillis(DEFAULT_ZIP_TIMESTAMP));
        entry.setTime(DEFAULT_ZIP_TIMESTAMP);
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
