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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Normalizes ObjectFactory java files generated by the JAXB xjc tool
 * and removes timestamps from JAXB generated files.
 */
@Mojo(name = "strip-jaxb", defaultPhase = LifecyclePhase.PROCESS_SOURCES, threadSafe = true)
public final class StripJaxbMojo extends AbstractMojo
{
    private static final int JAXB_FILE_JAXB_COMMENT_LINE_NUMBER = 1;
    private static final int JAXB_FILE_TIMESTAMP_LINE_NUMBER = 4;
    private static final int JAXB_EPISODE_JAXB_COMMENT_LINE_NUMBER = 4;
    private static final int JAXB_EPISODE_TIMESTAMP_LINE_NUMBER = 7;

    /**
     * Defines the supported XJC implementations.
     */
    enum XjcGenerator
    {
        /**
         * Sun implmentation of XJC, used for example in mojohaus jaxb2-maven-plugin.
         */
        COM_SUN_XML_BIND("JavaTM Architecture for XML Binding (JAXB)"),

        /**
         * Glassfish implementation of XJC, used for example in jvnet maven-jaxb2-plugin.
         */
        ORG_GLASSFISH_JAXB("Eclipse Implementation of JAXB");

        private final String matchingCommentText;

        XjcGenerator(String matchingCommentText)
        {
            this.matchingCommentText = matchingCommentText;
        }

        public String getMatchingCommentText()
        {
            return matchingCommentText;
        }
    }

    /**
     * The file encoding to use when reading the source files.
     * If the property project.build.sourceEncoding is not set,
     * the platform default encoding is used.
     */
    @Parameter(defaultValue = "${project.build.sourceEncoding}")
    private String encoding;
    
    /**
     * Directory where to find the source files generated by xjc.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources",
            property = "reproducible.generatedDirectory", required = true)
    private File generatedDirectory;
    
    /**
     * If true, skips the execution of the goal.
     */
    @Parameter(defaultValue = "false", property = "reproducible.skip")
    private boolean skip;
    
    /**
     * Fixes ObjectFactory java files generated by the JAXB xjc tool.
     * xjc (before JAXB 2.2.11) generates ObjectFactory.java files where the methods
     * are put in a non-predictable order (cf.https://java.net/jira/browse/JAXB-598).
     * If true, the methods in ObjectFactory.java file will be sorted in a reproducible order.
     */
    @Parameter(defaultValue = "true", property = "reproducible.fixJaxbOrder")
    private boolean fixJaxbOrder;
    
    /**
     * If true, the timestamps generated by JAXB will be removed.
     */
    @Parameter(defaultValue = "true", property = "reproducible.removeJaxbTimestamps")
    private boolean removeJaxbTimestamps;

    /**
     * Text which allow to identify the files generated by the JAXB xjc tool.
     * If provided, value is appended to the list of values defined in the known xjc generators.
     */
    @Parameter(defaultValue = "", property = "reproducible.matchingCommentText")
    private String matchingCommentText;

    @Parameter(defaultValue = "CRLF", property = "reproducible.lineSeparator")
    private LineSeparators lineSeparator;

    @Override
    public void execute() throws MojoExecutionException
    {
        if (skip)
        {
            getLog().info("Skipping execution of goal \"strip-jaxb\"");
        }
        else
        {
            fix();
        }
    }
    
    private void fix() throws MojoExecutionException
    {
        if (!generatedDirectory.exists() || !generatedDirectory.isDirectory())
        {
            return;
        }
        final Charset charset = Charset.forName(encoding);
        final JaxbObjectFactoryFixer objectFactoryFixer = new JaxbObjectFactoryFixer(getMatchingCommentTexts(),
                charset);
        final LineNumberStripper jaxbFileDateStripper =
            new LineNumberStripper(JAXB_FILE_TIMESTAMP_LINE_NUMBER, lineSeparator);
        final LineNumberStripper jaxbEpisodeDateStripper =
            new LineNumberStripper(JAXB_EPISODE_TIMESTAMP_LINE_NUMBER, lineSeparator);
        final File tmpFile = createTempFile();
        
        try
        {
            Files.walk(generatedDirectory.toPath())
                .filter(Files::isRegularFile)
                .forEach(f ->
                {
                    try
                    {
                        final List<String> lines = Files.readAllLines(f, charset);
                        // We cannot rely on an exact comment text to check if it is a JAXB generated file
                        // because it depends on the current locale
                        final boolean isJaxbFile = isJaxbFile(lines);
                        final boolean isObjectFactoryFile = isJaxbFile
                                && "ObjectFactory.java".equals(f.toFile().getName());
                        final boolean isEpisodeFile = isEpisodeFile(f.toFile().getName(), lines);
                                        
                        if (isObjectFactoryFile || isJaxbFile || isEpisodeFile)
                        {
                            getLog().info("Stripping " + f.toFile().getAbsolutePath());
                            if (isObjectFactoryFile && fixJaxbOrder)
                            {
                                objectFactoryFixer.strip(f.toFile(), tmpFile);
                                Files.move(tmpFile.toPath(), f, StandardCopyOption.REPLACE_EXISTING);
                            }
                            if (isJaxbFile && removeJaxbTimestamps)
                            {
                                jaxbFileDateStripper.strip(f.toFile(), tmpFile);
                                Files.move(tmpFile.toPath(), f, StandardCopyOption.REPLACE_EXISTING);
                            }
                            if (isEpisodeFile && removeJaxbTimestamps)
                            {
                                jaxbEpisodeDateStripper.strip(f.toFile(), tmpFile);
                                Files.move(tmpFile.toPath(), f, StandardCopyOption.REPLACE_EXISTING);
                            }
                        }
                    }
                    catch (IOException e)
                    {
                        getLog().error("Error when normalizing " + f.toFile().getAbsolutePath(), e);
                    }
                });
        }
        catch (IOException e)
        {
            throw new MojoExecutionException("Error when visiting " + generatedDirectory.getAbsolutePath(), e);
        }
    }
    
    private boolean isJaxbFile(List<String> lines)
    {
        return lines.size() > JAXB_FILE_TIMESTAMP_LINE_NUMBER
                && lines.get(0).equals("//")
                && getMatchingCommentTexts().stream().anyMatch(lines.get(JAXB_FILE_JAXB_COMMENT_LINE_NUMBER)::contains)
                && lines.get(JAXB_FILE_TIMESTAMP_LINE_NUMBER).contains(":");
    }
    
    private boolean isEpisodeFile(String filename, List<String> lines)
    {
        return filename.endsWith(".episode")
                && lines.size() > JAXB_EPISODE_TIMESTAMP_LINE_NUMBER
                && getMatchingCommentTexts().stream()
                .anyMatch(lines.get(JAXB_EPISODE_JAXB_COMMENT_LINE_NUMBER)::contains)
                && lines.get(JAXB_EPISODE_TIMESTAMP_LINE_NUMBER).contains(":");
    }

    private File createTempFile() throws MojoExecutionException
    {
        try
        {
            final File out = File.createTempFile("ObjectFactory", null);
            out.deleteOnExit();
            return out;
        }
        catch (IOException e)
        {
            throw new MojoExecutionException("Cannot create temp file", e);
        }
    }

    private List<String> getMatchingCommentTexts()
    {
        ArrayList<String> matchingCommentTexts = Arrays.stream(XjcGenerator.values())
                .map(XjcGenerator::getMatchingCommentText)
                .collect(Collectors.toCollection(ArrayList::new));

        if ((matchingCommentText != null) && (!matchingCommentText.trim().isEmpty()))
        {
            matchingCommentTexts.add(matchingCommentText.trim());
        }

        return matchingCommentTexts;
    }
}
