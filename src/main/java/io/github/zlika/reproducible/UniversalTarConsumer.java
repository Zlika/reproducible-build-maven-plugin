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
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Process tar formats: tar, tar.gz, tar.bz2.
 * @author Umberto Nicoletti (umberto.nicoletti@gmail.com)
 */
public final class UniversalTarConsumer implements FileConsumer
{
    /**
     * Whether the original file should be overwritten.
     */
    private final boolean overwrite;

    /**
     * Ctor.
     * @param overwrite Overwrite original file.
     */
    public UniversalTarConsumer(final boolean overwrite)
    {
        this.overwrite = overwrite;
    }

    @Override
    public void strip(final File file, final File stripped) throws MojoExecutionException
    {
        try
        {
            final Stripper stripper = this.findImplementation(file);
            stripper.strip(file, stripped);
            if (this.overwrite)
            {
                Files.move(stripped.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (IOException e)
        {
            throw new MojoExecutionException("Error when stripping " + file.getAbsolutePath(), e);
        }
    }

    /**
     * Return Stripper implementation depending on file extension.
     * @param file File to strip.
     * @return Stripper implementation.
     */
    private Stripper findImplementation(final File file)
    {
        final String name = file.getName();
        Stripper impl = new TarStripper();
        if (name.endsWith(".tar.gz"))
        {
            impl = new TarGzStripper();
        }
        if (name.endsWith(".tar.bz"))
        {
            impl = new TarBzStripper();
        }
        return impl;
    }
}
