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
import java.util.Arrays;
import java.util.List;

/**
 * Strips non-reproducible data from a properties file.
 * This stripper removes all comment lines (as some of them can contain date/time),
 * fix the line endings and optionally remove some user-defined properties.
 */
public final class PropertiesFileStripper implements Stripper
{
    private final List<String> propertiesToRemove;
    
    /**
     * Constructor.
     * @param propertiesToRemove list of properties to remove from the file.
     */
    public PropertiesFileStripper(String... propertiesToRemove)
    {
        this.propertiesToRemove = Arrays.asList(propertiesToRemove);
    }
    
    @Override
    public void strip(File in, File out) throws IOException
    {
        final TextFileStripper stripper = new TextFileStripper();
        stripper.addPredicate(s -> s.startsWith("#"));
        propertiesToRemove.forEach(property ->
            stripper.addPredicate(s -> s.startsWith(property + "=")));
        stripper.strip(in, out);
    }
}
