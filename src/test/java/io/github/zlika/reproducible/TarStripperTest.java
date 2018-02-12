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
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for tar Stripper.
 *
 * @author tglman
 *
 */
public class TarStripperTest
{

    /**
     * Tests stripping on a reference Tar file.
     *
     * @throws IOException in case of error on test file operations
     */
    @Test
    public void testStripTar() throws IOException
    {
        final File original = new File(this.getClass().getResource("test-tar.tar").getFile());
        final File stripped = File.createTempFile("test-tar", ".tar");
        stripped.deleteOnExit();
        final File expected = new File(this.getClass().getResource("test-tar-stripped.tar").getFile());

        new TarStripper().strip(original, stripped);

        Assert.assertArrayEquals(
            "Stripped tar does not match expected tar",
            new TarFile(expected).entries(),
            new TarFile(stripped).entries()
        );
        Assert.assertFalse(
            "Original tar matched the stripped tar",
            new TarFile(expected).entries().equals(new TarFile(original).entries())
        );
    }

}
