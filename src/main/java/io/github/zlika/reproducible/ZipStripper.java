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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Strips non-reproducible data from a ZIP file.
 * This includes zip entry dates.
 */
final class ZipStripper implements Stripper
{
    private static final int BUFFER_SIZE = 1024;
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
    public void strip(InputStream is, OutputStream os) throws IOException
    {
        final byte[] buffer = new byte[BUFFER_SIZE];
        try (ZipInputStream zis = new ZipInputStream(is); ZipOutputStream zos = new ZipOutputStream(os))
        {
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null)
            {
                zos.putNextEntry(clearZipEntryDate(ze));
                final Stripper fileStripper = subFilters.get(ze.getName());
                if (fileStripper != null)
                {
                    fileStripper.strip(zis, zos);
                }
                else
                {
                    writeZipEntryContent(zis, zos, buffer);
                }
            }
        }
    }

    private ZipEntry clearZipEntryDate(ZipEntry ze) throws IOException
    {
        final ZipEntry strippedZe = new ZipEntry(ze.getName());
        strippedZe.setTime(0);
        return strippedZe;
    }

    private void writeZipEntryContent(ZipInputStream zis, ZipOutputStream zos, byte[] buffer) throws IOException
    {
        while (zis.available() > 0)
        {
            int read = zis.read(buffer);
            if (read > 0)
            {
                zos.write(buffer, 0, read);
            }
        }
    }
}
