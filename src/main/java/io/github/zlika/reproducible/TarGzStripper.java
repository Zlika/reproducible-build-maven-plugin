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
import java.time.LocalDateTime;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

/**
 * Stripper implementation for support Tar compressed with gzip.
 */
public class TarGzStripper extends TarStripper
{
    /**
     * Constructor.
     * @param reproducibleDateTime the date/time to use in TAR entries.
     */
    public TarGzStripper(LocalDateTime reproducibleDateTime)
    {
        super(reproducibleDateTime);
    }

    @Override
    protected TarArchiveInputStream createInputStream(File in) throws FileNotFoundException, IOException
    {
        return new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(in)));
    }

    @Override
    protected TarArchiveOutputStream createOutputStream(File out) throws FileNotFoundException, IOException
    {
        final TarArchiveOutputStream stream = new TarArchiveOutputStream(
                new GzipCompressorOutputStream(new FileOutputStream(out)));
        stream.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
        return stream;
    }
}
