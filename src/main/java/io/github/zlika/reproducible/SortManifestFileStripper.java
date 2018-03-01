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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

/**
 * Sorts a MANIFEST file by attribute.
 */
final class SortManifestFileStripper implements Stripper
{
    private static final Comparator<String> MANIFEST_ENTRY_COMPARATOR = new Comparator<String>()
    {
        @Override
        public int compare(String o1, String o2)
        {
            if (o1.startsWith("Manifest-Version") || o2.trim().isEmpty())
            {
                return -1;
            }
            else if (o2.startsWith("Manifest-Version") || o1.trim().isEmpty())
            {
                return 1;
            }
            else
            {
                return o1.compareTo(o2);
            }
        }
    };
    
    @Override
    public void strip(File in, File out) throws IOException
    {
        final List<String> lines = Files.readAllLines(in.toPath(), StandardCharsets.UTF_8);
        
        try (final BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(out), StandardCharsets.UTF_8)))
        {
            parseAttributes(lines)
                .forEach(s ->
                {
                    try
                    {
                        writer.write(s);
                    }
                    catch (IOException e)
                    {
                    }
                });
        }
    }
    
    private TreeSet<String> parseAttributes(List<String> lines)
    {
        final TreeSet<String> attributes = new TreeSet<>(MANIFEST_ENTRY_COMPARATOR);
        String currentAttribute = "";
        for (String line : lines)
        {
            if (line.startsWith(" "))
            {
                currentAttribute += line + "\r\n";
            }
            else
            {
                if (!currentAttribute.isEmpty())
                {
                    attributes.add(currentAttribute);
                }
                currentAttribute = line + "\r\n";
            }
        }
        if (!currentAttribute.isEmpty())
        {
            attributes.add(currentAttribute);
        }
        return attributes;
    }
}
