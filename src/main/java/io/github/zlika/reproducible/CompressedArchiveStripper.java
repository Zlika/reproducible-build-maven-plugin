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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

/**
 * Stripper implementation for compressed archives.
 */
public class CompressedArchiveStripper extends ArchiveStripper
{
    /**
     * Constructor.
     * @param reproducibleDateTime the date/time to use in the archive entries.
     */
    public CompressedArchiveStripper(LocalDateTime reproducibleDateTime)
    {
        super(reproducibleDateTime);
    }

    @Override
    void strip(InputStream in, OutputStream out, Path tmp)
            throws IOException, ArchiveException, CompressorException
    {
        String format = CompressorStreamFactory.detect(in);

        CompressorStreamFactory compressorFactory = CompressorStreamFactory.getSingleton();
        try (InputStream cis = new BufferedInputStream(compressorFactory.createCompressorInputStream(format, in));
             OutputStream cout = new BufferedOutputStream(compressorFactory.createCompressorOutputStream(format, out)))
        {
            super.strip(cis, cout, tmp);
        }
    }
}
