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
import java.time.LocalDateTime;

/**
 * Process tar formats: tar, tar.gz, tar.bz2 using the default configuriaton
 * and the right tar stripper implementation.
 * @author Umberto Nicoletti (umberto.nicoletti@gmail.com)
 */
final class SmartTarStripper implements Stripper
{
    /**
     * Whether the original file should be overwritten.
     */
    private final boolean overwrite;
    private final LocalDateTime reproducibleDateTime;

    /**
     * Constructor.
     * @param overwrite Overwrite original file.
     * @param reproducibleDateTime the date/time to use in TAR entries.
     */
    public SmartTarStripper(boolean overwrite, LocalDateTime reproducibleDateTime)
    {
        this.overwrite = overwrite;
        this.reproducibleDateTime = reproducibleDateTime;
    }

    @Override
    public void strip(final File file, final File stripped) throws IOException
    {
        final Stripper stripper = findImplementation(file);
        stripper.strip(file, stripped);
        if (this.overwrite)
        {
            Files.move(stripped.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Return Stripper implementation depending on file extension.
     * @param file File to strip.
     * @return Stripper implementation.
     */
    private Stripper findImplementation(File file)
    {
        final String name = file.getName();
        final Stripper impl;
        if (name.endsWith(".tar.gz"))
        {
            impl = new TarGzStripper(reproducibleDateTime);
        }
        else if (name.endsWith(".tar.bz2"))
        {
            impl = new TarBzStripper(reproducibleDateTime);
        }
        else
        {
            impl = new TarStripper(reproducibleDateTime);
        }
        return impl;
    }
}
