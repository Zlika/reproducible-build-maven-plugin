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

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link FileUtils}.
 */
public class FileUtilsTest
{
    /**
     * Tests the method getNameWithoutExtension().
     */
    @Test
    public void testGetNameWithoutExtension()
    {
        Assert.assertEquals("name", FileUtils.getNameWithoutExtension(new File("name.ext")));
        Assert.assertEquals("name", FileUtils.getNameWithoutExtension(new File("name")));
        Assert.assertEquals("", FileUtils.getNameWithoutExtension(new File(".ext")));
        Assert.assertEquals("name", FileUtils.getNameWithoutExtension(new File("name.")));
    }
    
    /**
     * Tests the method getFileExtension().
     */
    @Test
    public void testGetFileExtension()
    {
        Assert.assertEquals("ext", FileUtils.getFileExtension(new File("name.ext")));
        Assert.assertEquals("", FileUtils.getFileExtension(new File("name")));
        Assert.assertEquals("ext", FileUtils.getFileExtension(new File(".ext")));
        Assert.assertEquals("", FileUtils.getFileExtension(new File("name.")));
    }
}
