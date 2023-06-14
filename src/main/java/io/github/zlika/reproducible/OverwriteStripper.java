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

/**
 * Stripper optionnally overwritting the input file with the stripped file.
 */
public class OverwriteStripper implements Stripper
{
    private final boolean overwrite;
    private final Stripper stripper;

    /**
     * Constructor.
     * @param overwrite true to overwrite the original file.
     * @param stripper  Stripper to use to process the file.
     */
    public OverwriteStripper(boolean overwrite, Stripper stripper)
    {
        this.overwrite = overwrite;
        this.stripper = stripper;
    }

    @Override
    public void strip(File in, File out) throws IOException
    {
        stripper.strip(in, out);
        if (this.overwrite)
        {
            Files.move(out.toPath(), in.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
