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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Fix the produced artifacts to make the build reproducible.
 */
@Mojo(name = "strip", defaultPhase = LifecyclePhase.PACKAGE)
public class StripMojo extends AbstractMojo
{
    private static final String[] ZIP_EXT = { "zip", "jar", "war", "ear" };
    private static final String PLUGIN_TMP_FOLDER = "reproducible-maven-plugin";
    
    @Parameter(defaultValue = "${project.build.directory}", property = "outputDir", required = true)
    private File outputDirectory;

    public void execute() throws MojoExecutionException
    {
        final File[] zipFiles = findZipFiles(outputDirectory);
        for (File zip : zipFiles)
        {
            getLog().info("Stripping " + zip.getAbsolutePath());
            try
            {
                final File tmp = File.createTempFile("reproducible", null, outputDirectory);
                try (final FileInputStream zin = new FileInputStream(zip);
                    final FileOutputStream zout = new FileOutputStream(tmp))
                {
                    new ZipStripper(getZipStripperTmpFolder(zip))
                        .addFileStripper("META-INF/MANIFEST.MF", new ManifestStripper())
                        .addFileStripper("META-INF/maven/org.test/test/pom.properties", new PomPropertiesStripper())
                        .strip(zin, zout);
                }
                Files.move(tmp.toPath(), zip.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            catch (IOException e)
            {
                throw new MojoExecutionException("Error when stripping " + zip.getAbsolutePath(), e);
            }
        }
    }
    
    private File[] findZipFiles(File folder)
    {
        return folder.listFiles((dir, name) ->
                Arrays.stream(ZIP_EXT).anyMatch(ext -> name.toLowerCase().endsWith(ext)));
    }
    
    private File getZipStripperTmpFolder(File zipFile)
    {
        return new File(new File(outputDirectory, PLUGIN_TMP_FOLDER), filenameWithoutExtension(zipFile.getName()));
    }
    
    private String filenameWithoutExtension(String filename)
    {
        return filename.replaceFirst("[.][^.]+$", "");
    }
}
