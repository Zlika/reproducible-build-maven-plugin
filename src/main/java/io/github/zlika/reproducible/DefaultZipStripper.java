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
import java.util.Collections;
import java.util.List;

/**
 * Process and strips ZIP (jar, war, as well) archives,
 * supplying a default configuration.
 */
final class DefaultZipStripper implements Stripper
{
    /**
     * Whether the original file should be overwritten.
     */
    private final boolean overwrite;
    /**
     * The ZipStripper to configure.
     */
    private final ZipStripper stripper;
    private final List<String> manifestAttributes;

    /**
     * Constructor.
     * @param stripper The ZipStripper to wrap with default config.
     * @param overwrite Overwrite original file.
     * @param manifestAttributes Additional manifest attributes to skip.
     */
    public DefaultZipStripper(ZipStripper stripper, boolean overwrite, List<String> manifestAttributes)
    {
        this.overwrite = overwrite;
        this.manifestAttributes = Collections.unmodifiableList(manifestAttributes);
        this.stripper = configure(stripper);
    }

    @Override
    public void strip(File zip, File stripped) throws IOException
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
    private ZipStripper configure(ZipStripper zip)
    {
        zip.addFileStripper("META-INF/MANIFEST.MF", new ManifestStripper(manifestAttributes))
            .addFileStripper("META-INF/maven/\\S*/pom.properties", new PropertiesFileStripper())
            .addFileStripper("META-INF/maven/plugin.xml", new MavenPluginToolsStripper())
            .addFileStripper("META-INF/maven/\\S*/plugin-help.xml", new MavenPluginToolsStripper())
            .addFileStripper("META-INF/sisu/javax.inject.Named", LineEndingsStripper.INSTANCE)
            .addFileStripper("META-INF/build-info.properties", new PropertiesFileStripper("build.time"))
            .addFileStripper("BOOT-INF/classes/git.properties", new PropertiesFileStripper(
                    "git.build.host", "git.build.time", "git.build.user.email", "git.build.user.name"));
        return zip;
    }
}
