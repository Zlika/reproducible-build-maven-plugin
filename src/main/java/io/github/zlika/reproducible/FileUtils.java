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
import java.util.Objects;

/**
 * Utility class.
 */
final class FileUtils
{
    private FileUtils()
    {
    }
    
    /**
     * Gets the file name part of a File object, excluding path and extension.
     * @param file the file to get the name from.
     * @return the name of the file without extension, or an empty string.
     */
    public static String getNameWithoutExtension(File file)
    {
        final String filename = file.getName();
        final int index = filename.lastIndexOf(".");
        return index < 0 ? filename : filename.substring(0, index);
    }
    
    /**
     * Gets the extension of a file.
     * @param file the file to get the extension from.
     * @return the extension of the file, or an empty string.
     */
    public static String getFileExtension(File file)
    {
        final String filename = file.getName();
        final int index = filename.lastIndexOf('.');
        return (index < 0) ? "" : filename.substring(index + 1);
    }
    
    /**
     * Creates the directory and if necessary the non-existent parent directories.
     * This method uses File.mkdirs() to create the directory, but throws
     * an IOException in case of error instead of returning a boolean.
     * No exception is thrown if the directory already exists.
     * @param folder the folder to be created.
     * @throws IOException if the folder cannot be created.
     */
    public static void mkdirs(File folder) throws IOException
    {
        Objects.requireNonNull(folder);
        if (!folder.exists() && !folder.mkdirs())
        {
            throw new IOException("Cannot create folder " + folder);
        }
    }
}
