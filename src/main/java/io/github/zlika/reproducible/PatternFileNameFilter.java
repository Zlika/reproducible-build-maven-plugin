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
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.logging.Log;

/**
 * A pattern-based filename filter.
 * <p>
 * An incoming filename is first checked to see if it has a suitable file
 * extension. If it doesn't, evaluation halts and the file is rejected.
 * <p>
 * The name is then checked against a series of inclusion patterns. If the
 * name doesn't match any of the inclusion patterns, evaluation halts and the
 * file is rejected.
 * <p>
 * The name is then checked against a series of exclusion patterns. If the
 * name matches at least one of the exclusion patterns, evaluation halts and
 * the file is rejected.
 * <p>
 * Otherwise, the file is accepted.
 */

public final class PatternFileNameFilter implements FilenameFilter
{
    private final Log log;
    private final List<Pattern> includes;
    private final List<Pattern> excludes;
    private final List<String> extensions;

    private PatternFileNameFilter(
            final Log inLog,
            final List<Pattern> inIncludes,
            final List<Pattern> inExcludes,
            final List<String> inExtensions)
    {
        this.log = Objects.requireNonNull(inLog, "log");
        this.includes = Objects.requireNonNull(inIncludes, "includes");
        this.excludes = Objects.requireNonNull(inExcludes, "excludes");
        this.extensions = Objects.requireNonNull(inExtensions, "extensions");
    }

    /**
     * Construct a new pattern-based filename filter.
     *
     * @param log        A logger
     * @param includes   The inclusion patterns
     * @param excludes   The exclusion patterns
     * @param extensions The filename extensions to which this filter applies
     *
     * @return A new filter
     */

    public static PatternFileNameFilter of(
            final Log log,
            final List<String> includes,
            final List<String> excludes,
            final List<String> extensions)
    {
        final ArrayList<Pattern> includePatterns =
                new ArrayList<>(includes.size());
        final ArrayList<Pattern> excludePatterns =
                new ArrayList<>(includes.size());

        for (final String include : includes)
        {
            if (include != null)
            {
                final String trimmed = include.trim();
                if (!trimmed.isEmpty())
                {
                    includePatterns.add(Pattern.compile(trimmed));
                }
            }
        }
        for (final String exclude : excludes)
        {
            if (exclude != null)
            {
                final String trimmed = exclude.trim();
                if (!trimmed.isEmpty())
                {
                    excludePatterns.add(Pattern.compile(trimmed));
                }
            }
        }

        return new PatternFileNameFilter(
                log, includePatterns, excludePatterns, extensions
        );
    }

    @Override
    public boolean accept(final File dir, final String name)
    {
        this.log.debug("Checking if " + name + " should be processed...");

        if (!this.fileIncludedByExtensions(name))
        {
            this.log.debug("File " + name + " will not be processed: Inappropriate file name extension");
            return false;
        }

        if (!this.fileIncludedByInclusionPatterns(name))
        {
            this.log.debug("File " + name + " will not be processed: No inclusion patterns match");
            return false;
        }

        if (this.fileExcluded(name))
        {
            this.log.debug("File " + name + " will not be processed: An exclusion pattern matches");
            return false;
        }

        this.log.debug("File " + name + " will be processed");
        return true;
    }

    private boolean fileExcluded(final String name)
    {
        boolean excluded = false;
        for (final Pattern exclusion : this.excludes)
        {
            final Matcher matcher = exclusion.matcher(name);
            if (matcher.matches())
            {
                this.log.debug("Exclusion: match: " + name + " -> " + exclusion);
                excluded = true;
                break;
            }
            else
            {
                this.log.debug("Exclusion: no-match: " + name + " -> " + exclusion);
            }
        }

        return excluded;
    }

    private boolean fileIncludedByExtensions(final String name)
    {
        final String lowercase = name.toLowerCase();
        for (final String extension : this.extensions)
        {
            if (lowercase.endsWith(extension))
            {
                return true;
            }
        }

        return false;
    }

    private boolean fileIncludedByInclusionPatterns(final String name)
    {
        boolean included = false;
        for (final Pattern inclusion : this.includes)
        {
            final Matcher matcher = inclusion.matcher(name);
            if (matcher.matches())
            {
                this.log.debug("Inclusion: match: " + name + " -> " + inclusion);
                included = true;
                break;
            }
            else
            {
                this.log.debug("Inclusion: no match: " + name + " -> " + inclusion);
            }
        }
        return included;
    }
}
