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
import java.util.List;

/** Removes a given line in a text file based on the line number. */
class LineNumberStripper implements Stripper
{
    private int lineNumber;
    
    /**
     * Constructor.
     * @param lineNumber the line number to remove.
     */
    public LineNumberStripper(int lineNumber)
    {
        this.lineNumber = lineNumber;
    }
    
    @Override
    public void strip(File in, File out) throws IOException
    {
        try (final BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(out), StandardCharsets.UTF_8)))
        {
            final List<String> lines = Files.readAllLines(in.toPath(), StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++)
            {
                if (i != lineNumber)
                {
                    try
                    {
                        writer.write(lines.get(i));
                        writer.write("\r\n");
                    }
                    catch (IOException e)
                    {
                    }
                }
            }
        }
    }
}
