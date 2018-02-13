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

/**
 * Process and strips ZIP (jar, war, as well) archives,
 * supplying a default configuration.
 * @author Umberto Nicoletti (umberto.nicoletti@gmail.com)
 */
public final class DefaultZipStripper implements Stripper
{
    /**
     * Whether the original file should be overwritten.
     */
    private final boolean overwrite;
    /**
     * The ZipStripper to configure.
     */
    private final ZipStripper stripper;

    /**
     * Ctor.
     * @param stripper The ZipStripper to wrap with default config.
     * @param overwrite Overwrite original file.
     */
    public DefaultZipStripper(final ZipStripper stripper, final boolean overwrite)
    {
        this.stripper = DefaultZipStripper.configure(stripper);
        this.overwrite = overwrite;
    }

    @Override
    public void strip(final File zip, final File stripped) throws IOException
    {
        this.stripper.strip(zip, stripped);
        if (this.overwrite)
        {
            Files.move(stripped.toPath(), zip.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Configure the supplied ZipStripper.
     * @param zip The ZipStripper to configure.
     * @return The configured ZipStripper.
     */
    private static ZipStripper configure(ZipStripper zip)
    {
        zip.addFileStripper("META-INF/MANIFEST.MF", new ManifestStripper())
            .addFileStripper("META-INF/maven/\\S*/pom.properties", new PomPropertiesStripper())
            .addFileStripper("META-INF/maven/plugin.xml", new MavenPluginToolsStripper())
            .addFileStripper("META-INF/maven/\\S*/plugin-help.xml", new MavenPluginToolsStripper());
        return zip;
    }
}
