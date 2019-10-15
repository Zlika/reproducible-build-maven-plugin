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
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Fixes the produced artifacts (ZIP/JAR/WAR/EAR) to make the build reproducible.
 */
@Mojo(name = "strip-jar", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST,
        requiresProject = false, threadSafe = true)
public final class StripJarMojo extends AbstractMojo
{
    private static final String[] ZIP_EXT = { "zip", "jar", "war", "ear", "hpi" };
    private static final String TAR_GZ_EXT = "tar.gz";
    private static final String TAR_BZ_EXT = "tar.bz2";
    private static final String TAR_EXT = "tar";
    private static final byte[] ZIP_FILE_HEADER = new byte[] { 0x50, 0x4B, 0x03, 0x04 };
    private static final byte[] SPRING_BOOT_EXEC_HEADER = new byte[] { 0x23, 0x21, 0x2F, 0x62, 0x69, 0x6E };

    /**
     * Directory where to find zip/jar/war/ear files for stripping.
     */
    @Parameter(defaultValue = "${project.build.directory}", property = "reproducible.outputDirectory", required = true)
    private File outputDirectory;

    /**
     * By default, the stripping is done in-place. To create new files without changing the original ones, set this
     * parameter to "false". The new files are named by appending "-stripped" to the original file name.
     */
    @Parameter(defaultValue = "true", property = "reproducible.overwrite")
    private boolean overwrite;

    /**
     * If true, skips the execution of the goal.
     */
    @Parameter(defaultValue = "false", property = "reproducible.skip")
    private boolean skip;

    /**
     * By default, timestamp of zip file entries set at midnight on January 1, 2000. Set this parameter to desired
     * date and time if necessary.
     */
    @Parameter(defaultValue = "20000101000000", property = "reproducible.zipDateTime")
    private String zipDateTime;

    /**
     * By default, zipDateTime format pattern is {@code "yyyyMMddHHmmss"}. Set custom format pattern if necessary.
     * Pattern must be valid for {@link java.time.format.DateTimeFormatter#ofPattern(String)}.
     */
    @Parameter(defaultValue = "yyyyMMddHHmmss", property = "reproducible.zipDateTimeFormatPattern")
    private String zipDateTimeFormatPattern;
    
    /**
     * If enabled, the ZIP external file attributes will be forced to rw-r--r for files and rwxr-xr-x for folders.
     * This parameter only applies to JAR/WAR files.
     */
    @Parameter(defaultValue = "false", property = "reproducible.fixZipExternalFileAttributes")
    private boolean fixZipExternalFileAttributes;
    
    /**
     * Additional manifest attributes to strip.
     * Currently, only single-line attributes are supported.
     */
    @Parameter(property = "reproducible.manifestAttributes")
    private List<String> manifestAttributes;
    
    @Parameter(property = "reproducible.newLineTextFiles")
    private List<String> newLineTextFiles;

    @Override
    public void execute() throws MojoExecutionException
    {
        if (skip)
        {
            getLog().info("Skipping execution of goal \"strip-jar\"");
        }
        else
        {
            final LocalDateTime reproducibleDateTime = LocalDateTime.parse(zipDateTime,
                    DateTimeFormatter.ofPattern(zipDateTimeFormatPattern));
            final ZipStripper zipStripper = new ZipStripper(reproducibleDateTime, fixZipExternalFileAttributes);
            newLineTextFiles.forEach(f -> zipStripper.addFileStripper(f, LineEndingsStripper.INSTANCE));
            this.process(
                this.findZipFiles(this.outputDirectory),
                new DefaultZipStripper(zipStripper, this.overwrite, this.manifestAttributes)
            );
            this.process(
                    this.findSpringBootExecutable(this.outputDirectory),
                    new SpringBootExecutableStripper(this.overwrite,
                            new DefaultZipStripper(zipStripper, false, this.manifestAttributes))
            );
            this.process(
                this.findTarFiles(this.outputDirectory),
                new SmartTarStripper(this.overwrite, reproducibleDateTime)
            );
            this.process(
                this.findTarBzFiles(this.outputDirectory),
                new SmartTarStripper(this.overwrite, reproducibleDateTime)
            );
            this.process(
                this.findTarGzFiles(this.outputDirectory),
                new SmartTarStripper(this.overwrite, reproducibleDateTime)
            );
        }
    }

    /**
     * Perform the actual stripping for a set of files using the supplied
     * Stripper implementation.
     * @param files The files to process.
     * @param stripper The stripper to use.
     * @throws MojoExecutionException On error.
     */
    private void process(final File[] files, final Stripper stripper) throws MojoExecutionException
    {
        for (final File file : files)
        {
            this.getLog().info("Stripping " + file.getAbsolutePath());
            try
            {
                stripper.strip(file, this.createStrippedFilename(file));
            }
            catch (final IOException ioe)
            {
                throw new MojoExecutionException(
                    String.format("Error stripping file %s:", file.getAbsolutePath()),
                    ioe
                );
            }
        }
    }

    private File[] findZipFiles(File folder)
    {
        final File[] zipFiles = folder.listFiles((dir, name) ->
                Arrays.stream(ZIP_EXT).anyMatch(ext -> name.toLowerCase().endsWith("." + ext))
                && new File(dir, name).isFile()
                && Arrays.equals(getFileHeader(new File(dir, name), ZIP_FILE_HEADER.length),
                                ZIP_FILE_HEADER));
        return zipFiles != null ? zipFiles : new File[0];
    }
    
    /**
     * Finds JAR/WAR/ZIP files repackaged by the spring-boot-maven-plugin plugin.
     */
    private File[] findSpringBootExecutable(File folder)
    {
        final File[] zipFiles = folder.listFiles((dir, name) ->
        Arrays.stream(ZIP_EXT).anyMatch(ext -> name.toLowerCase().endsWith("." + ext))
        && new File(dir, name).isFile()
        && Arrays.equals(getFileHeader(new File(dir, name), SPRING_BOOT_EXEC_HEADER.length),
                        SPRING_BOOT_EXEC_HEADER));
        return zipFiles != null ? zipFiles : new File[0];
    }
    
    private byte[] getFileHeader(File file, int length)
    {
        final byte[] header = new byte[length];
        try (FileInputStream is = new FileInputStream(file))
        {
            if (is.read(header) != length)
            {
                return null;
            }
        }
        catch (IOException e)
        {
            return null;
        }
        return header;
    }

    private File[] findTarBzFiles(File folder)
    {
        final File[] tbzFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(TAR_BZ_EXT));
        return tbzFiles != null ? tbzFiles : new File[0];
    }

    private File[] findTarGzFiles(File folder)
    {
        final File[] tgzFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(TAR_GZ_EXT));
        return tgzFiles != null ? tgzFiles : new File[0];
    }

    private File[] findTarFiles(File folder)
    {
        final File[] tarFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(TAR_EXT));
        return tarFiles != null ? tarFiles : new File[0];
    }

    private File createStrippedFilename(File originalFile)
    {
        final String filenameWithoutExt = FileUtils.getNameWithoutExtension(originalFile);
        final String ext = FileUtils.getFileExtension(originalFile);
        return new File(originalFile.getParentFile(), filenameWithoutExt + "-stripped"
                + (ext.isEmpty() ? "" : ".") + ext);
    }
}
