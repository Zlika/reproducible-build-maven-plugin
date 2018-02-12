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
 * Process and strips ZIP (jar, war, as well) archives.
 * @author Umberto Nicoletti (umberto.nicoletti@gmail.com)
 * @version $Id$
 */
public final class ZipConsumer implements FileConsumer
{
    /**
     * Whether the original file should be overwritten.
     */
    private boolean overwrite;

    /**
     * Ctor.
     * @param overwrite Overwrite original file.
     */
    public ZipConsumer(boolean overwrite)
    {
        this.overwrite = overwrite;
    }

    @Override
    public void strip(final File zip, final File stripped) throws MojoExecutionException
    {
        try
        {
            final ZipStripper stripper = new ZipStripper();
            stripper.addFileStripper("META-INF/MANIFEST.MF", new ManifestStripper())
                .addFileStripper("META-INF/maven/\\S*/pom.properties", new PomPropertiesStripper())
                .addFileStripper("META-INF/maven/plugin.xml", new MavenPluginToolsStripper())
                .addFileStripper("META-INF/maven/\\S*/plugin-help.xml", new MavenPluginToolsStripper());
            stripper.strip(zip, stripped);
            if (this.overwrite)
            {
                Files.move(stripped.toPath(), zip.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (final IOException ioe)
        {
            throw new MojoExecutionException("Error when stripping " + zip.getAbsolutePath(), ioe);
        }
    }
}
