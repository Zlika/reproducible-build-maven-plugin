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

/**
 * Strips non-reproducible data from MANIFEST files.
 * This stripper removes the following lines from the manifest:
 * - Built-By
 * - Created-By
 * - Build-Jdk
 * - Build-Date / Build-Time
 * - Bnd-LastModified
 * It also ensures that the MANIFEST entries are in a reproducible order
 * (workaround for MSHARED-511 that was fixed in maven-archiver-3.0.1).
 */
public final class ManifestStripper implements Stripper
{
    @Override
    public void strip(File in, File out) throws IOException
    {
        final TextFileStripper s1 = new TextFileStripper()
            .addPredicate(s -> s.startsWith("Built-By"))
            .addPredicate(s -> s.startsWith("Created-By"))
            .addPredicate(s -> s.startsWith("Build-Jdk"))
            .addPredicate(s -> s.startsWith("Build-Date"))
            .addPredicate(s -> s.startsWith("Build-Time"))
            .addPredicate(s -> s.startsWith("Bnd-LastModified"));
        final SortManifestFileStripper s2 = new SortManifestFileStripper();
        new CompoundStripper(s1, s2).strip(in, out);
    }
}
