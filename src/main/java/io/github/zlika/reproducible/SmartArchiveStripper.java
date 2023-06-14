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

/**
 * Process archive formats: tar, tar.gz, tar.bz2, ar, cpio using the default configuration
 * and the right stripper implementation.
 * @author Umberto Nicoletti (umberto.nicoletti@gmail.com)
 */
final class SmartArchiveStripper implements Stripper
{
    private final LocalDateTime reproducibleDateTime;

    /**
     * Constructor.
     * @param reproducibleDateTime the date/time to use in the archive entries.
     */
    public SmartArchiveStripper(LocalDateTime reproducibleDateTime)
    {
        this.reproducibleDateTime = reproducibleDateTime;
    }

    @Override
    public void strip(final File file, final File stripped) throws IOException
    {
        final Stripper stripper = findImplementation(file);
        stripper.strip(file, stripped);
    }

    /**
     * Return Stripper implementation depending on file extension.
     * @param file File to strip.
     * @return Stripper implementation.
     */
    private Stripper findImplementation(File file)
    {
        final String name = file.getName();
        if (name.endsWith(".tar.gz") || name.endsWith(".tar.bz2"))
        {
            return new CompressedArchiveStripper(reproducibleDateTime);
        }
        else
        {
            return new ArchiveStripper(reproducibleDateTime);
        }
    }
}
