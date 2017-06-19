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
import java.util.Arrays;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Fixes the produced artifacts (ZIP/JAR/WAR/EAR) to make the build reproducible.
 */
@Mojo(name = "strip-jar", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST, requiresProject = false)
public final class StripJarMojo extends AbstractMojo
{
    private static final String[] ZIP_EXT = { "zip", "jar", "war", "ear" };
    
    private static final String TAR_GZ_EXT =  "tar.gz";
    
    private static final String TAR_BZ_EXT =  "tar.bz";
    
    /**
     * Directory where to find zip/jar/war/ear files for stripping.
     */
    @Parameter(defaultValue = "${project.build.directory}", property = "reproducible.outputDirectory", required = true)
    private File outputDirectory;
    
    /**
     * By default, the stripping is done in-place.
     * To create new files without changing the original ones, set this parameter to "false".
     * The new files are named by appending "-stripped" to the original file name.
     */
    @Parameter(defaultValue = "true", property = "reproducible.overwrite")
    private boolean overwrite;
    
    /**
     * If true, skips the execution of the goal.
     */
    @Parameter(defaultValue = "false", property = "reproducible.skip")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException
    {
        if (skip)
        {
            getLog().info("Skipping execution of goal \"strip-jar\"");
        }
        else
        {
            strip();
        }
    }
    
    private void strip() throws MojoExecutionException
    {
        final File[] zipFiles = findZipFiles(outputDirectory);
        for (File zip : zipFiles)
        {
            getLog().info("Stripping " + zip.getAbsolutePath());
            try
            {
                final File stripped = createStrippedFilename(zip);
                addFileStrippers(new ZipStripper()).strip(zip, stripped);
                if (overwrite)
                {
                    Files.move(stripped.toPath(), zip.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            catch (IOException e)
            {
                throw new MojoExecutionException("Error when stripping " + zip.getAbsolutePath(), e);
            }
        }
        final File[] tarGzFiles = findTarGzFiles(outputDirectory);
        for (File tarGz : tarGzFiles)
        {
            getLog().info("Stripping " + tarGz.getAbsolutePath());
            try
            {
                final File stripped = createStrippedFilename(tarGz);
                addFileStrippers(new TarStripper()).strip(tarGz, stripped);
                if (overwrite)
                {
                    Files.move(stripped.toPath(), tarGz.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            catch (IOException e)
            {
                throw new MojoExecutionException("Error when stripping " + tarGz.getAbsolutePath(), e);
            }
        }        
        final File[] tarBzFiles = findTarBzFiles(outputDirectory);
        for (File tarBz : tarBzFiles)
        {
            getLog().info("Stripping " + tarBz.getAbsolutePath());
            try
            {
                final File stripped = createStrippedFilename(tarBz);
                addFileStrippers(new TarBzStripper()).strip(tarBz, stripped);
                if (overwrite)
                {
                    Files.move(stripped.toPath(), tarBz.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            catch (IOException e)
            {
                throw new MojoExecutionException("Error when stripping " + tarBz.getAbsolutePath(), e);
            }
        }
    }
    
    private AbstractStripper addFileStrippers(AbstractStripper stripper){
    	return stripper.addFileStripper("META-INF/MANIFEST.MF", new ManifestStripper())
    	.addFileStripper("META-INF/maven/\\S*/pom.properties", new PomPropertiesStripper())
    	.addFileStripper("META-INF/maven/plugin.xml", new MavenPluginToolsStripper())
    	.addFileStripper("META-INF/maven/\\S*/plugin-help.xml", new MavenPluginToolsStripper());
    }
    
    private File[] findZipFiles(File folder)
    {
        final File[] zipFiles = folder.listFiles((dir, name) ->
                Arrays.stream(ZIP_EXT).anyMatch(ext -> name.toLowerCase().endsWith(ext)));
        return zipFiles != null ? zipFiles : new File[0];
    }
    
    private File[] findTarBzFiles(File folder)
    {
        final File[] zipFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(TAR_BZ_EXT));
        return zipFiles != null ? zipFiles : new File[0];
    }    

    private File[] findTarGzFiles(File folder)
    {
        final File[] zipFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(TAR_GZ_EXT));
        return zipFiles != null ? zipFiles : new File[0];
    }    

    
    private File createStrippedFilename(File originalFile)
    {
        final String filenameWithoutExt = FileUtils.getNameWithoutExtension(originalFile);
        final String ext = FileUtils.getFileExtension(originalFile);
        return new File(originalFile.getParentFile(), filenameWithoutExt + "-stripped"
                                                        + (ext.isEmpty() ? "" : ".") + ext);
    }
}
