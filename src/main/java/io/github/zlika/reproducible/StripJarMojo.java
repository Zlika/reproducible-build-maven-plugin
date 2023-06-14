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
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Fixes the produced artifacts to make the build reproducible.
 */
@Mojo(name = "strip-jar", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST,
        requiresProject = false, threadSafe = true)
public final class StripJarMojo extends AbstractMojo
{
    private static final List<String> ZIP_EXT = Arrays.asList("zip", "jar", "war", "ear", "hpi", "adapter");
    private static final List<String> ARCHIVE_EXT =
            Arrays.asList(".tar", ".tar.gz", ".tar.bz2", ".tgz", ".cpio", ".rpm", ".ar", ".deb");
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

    /**
     * A list of filename inclusion patterns. File names are checked against
     * inclusion patterns and, if at least one inclusion pattern matches, the
     * file is considered a candidate for stripping. Inclusion patterns are
     * checked *before* exclusion patterns.
     *
     * By default, all files are included.
     *
     * @see PatternFileNameFilter
     */

    @Parameter(property = "reproducible.includes")
    private List<String> includes;

    /**
     * A list of filename exclusion patterns. File names are checked against
     * exclusion patterns and, if at least one exclusion pattern matches, the
     * file is *not* considered a candidate for stripping. Exclusion patterns are
     * checked *after* inclusion patterns.
     *
     * By default, no files are excluded.
     *
     * @see PatternFileNameFilter
     */

    @Parameter(property = "reproducible.excludes")
    private List<String> excludes;

    /**
     * A list of nested filename inclusion patterns. File names are checked against
     * nested inclusion patterns and, if at least one nested inclusion pattern matches, the
     * file is considered a candidate for stripping.
     *
     * By default, no nested files are included.
     */

    @Parameter(property = "reproducible.nestedIncludes")
    private List<String> nestedIncludes;

    @Override
    public void execute() throws MojoExecutionException
    {
        if (skip)
        {
            getLog().info("Skipping execution of goal \"strip-jar\"");
        }
        else
        {
            if (this.includes == null || this.includes.isEmpty())
            {
                this.includes = Collections.singletonList(".*");
            }
            if (this.excludes == null)
            {
                this.excludes = Collections.emptyList();
            }

            final LocalDateTime reproducibleDateTime = LocalDateTime.parse(zipDateTime,
                    DateTimeFormatter.ofPattern(zipDateTimeFormatPattern));
            final ZipStripper zipStripper = new ZipStripper(reproducibleDateTime, fixZipExternalFileAttributes);
            newLineTextFiles.forEach(f -> zipStripper.addFileStripper(f, LineEndingsStripper.INSTANCE));
            final Stripper stripper = new OverwriteStripper(this.overwrite, new DefaultZipStripper(zipStripper,
                    this.manifestAttributes));

            if (this.nestedIncludes != null && !this.nestedIncludes.isEmpty())
            {
                final Stripper nestedFileStripper =
                        new DefaultZipStripper(zipStripper, this.manifestAttributes);
                for (final String include : this.nestedIncludes)
                {
                    if (include.endsWith("jar") || include.endsWith("zip"))
                    {
                        zipStripper.addFileStripper(include, nestedFileStripper);
                    }
                }
            }

            this.process(
                this.findZipFiles(this.outputDirectory),
                stripper
            );
            this.process(
                this.findSpringBootExecutable(this.outputDirectory),
                new OverwriteStripper(this.overwrite,
                        new SpringBootExecutableStripper(
                                new DefaultZipStripper(zipStripper, this.manifestAttributes)))
            );
            this.process(
                this.findArchiveFiles(this.outputDirectory),
                        new OverwriteStripper(this.overwrite, new SmartArchiveStripper(reproducibleDateTime))
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

    private File[] findZipFiles(final File folder)
    {
        final PatternFileNameFilter filter =
                PatternFileNameFilter.of(this.getLog(), this.includes, this.excludes, ZIP_EXT);

        final File[] zipFiles = folder.listFiles((dir, name) ->
                filter.accept(dir, name)
                && new File(dir, name).isFile()
                && Arrays.equals(getFileHeader(new File(dir, name), ZIP_FILE_HEADER.length),
                                ZIP_FILE_HEADER));
        return zipFiles != null ? zipFiles : new File[0];
    }

    /**
     * Finds JAR/WAR/ZIP files repackaged by the spring-boot-maven-plugin plugin.
     */
    private File[] findSpringBootExecutable(final File folder)
    {
        final PatternFileNameFilter filter =
                PatternFileNameFilter.of(this.getLog(), this.includes, this.excludes, ZIP_EXT);

        final File[] zipFiles = folder.listFiles((dir, name) ->
                filter.accept(dir, name)
                && new File(dir, name).isFile()
                && Arrays.equals(getFileHeader(new File(dir, name), SPRING_BOOT_EXEC_HEADER.length),
                        SPRING_BOOT_EXEC_HEADER));
        return zipFiles != null ? zipFiles : new File[0];
    }

    private byte[] getFileHeader(final File file, final int length)
    {
        final byte[] header = new byte[length];
        try (FileInputStream is = new FileInputStream(file))
        {
            if (is.read(header) != length)
            {
                return null;
            }
        }
        catch (final IOException e)
        {
            return null;
        }
        return header;
    }

    private File[] findArchiveFiles(final File folder)
    {
        final PatternFileNameFilter filter =
                PatternFileNameFilter.of(this.getLog(), this.includes, this.excludes, ARCHIVE_EXT);
        final File[] archiveFiles = folder.listFiles(filter);
        return archiveFiles != null ? archiveFiles : new File[0];
    }

    private File createStrippedFilename(final File originalFile)
    {
        final String filenameWithoutExt = FileUtils.getNameWithoutExtension(originalFile);
        final String ext = FileUtils.getFileExtension(originalFile);
        return new File(originalFile.getParentFile(), filenameWithoutExt + "-stripped"
                + (ext.isEmpty() ? "" : ".") + ext);
    }
}
