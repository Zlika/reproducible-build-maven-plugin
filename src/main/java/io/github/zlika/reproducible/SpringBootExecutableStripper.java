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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Strips non-reproducible data from a JAR/WAR/ZIP file repackaged by
 * the spring-boot-maven-plugin plugin.
 * This plugin, when its "executable" option is set, prepends a launch script at the
 * front of the JAR/WAR/ZIP file to make it executable.
 * This class processes such files by extracting the embedded ZIP file,
 * stripping it, and then prepending the Sprint Boot launch script.
 */
public class SpringBootExecutableStripper implements Stripper
{
    private static final byte[] ZIP_FILE_HEADER = new byte[] { 0x50, 0x4B, 0x03, 0x04 };
    
    private final DefaultZipStripper zipStripper;
    private final boolean overwrite;
    
    /**
     * Constructor.
     * @param zipStripper Stripper to use to process the ZIP file.
     */
    public SpringBootExecutableStripper(boolean overwrite, DefaultZipStripper zipStripper)
    {
        this.zipStripper = zipStripper;
        this.overwrite = overwrite;
    }
    
    @Override
    public void strip(File in, File out) throws IOException
    {
        final byte[] launchScript = extractLaunchScript(in);
        final File tmp = Files.createTempFile(null, null).toFile();
        final File tmp2 = Files.createTempFile(null, null).toFile();
        tmp.deleteOnExit();
        tmp2.deleteOnExit();
        try
        {
            extractZipFile(in, launchScript.length, tmp);
            zipStripper.strip(tmp, tmp2);
            repackLaunchScript(launchScript, tmp2, out);
        }
        finally
        {
            Files.delete(tmp.toPath());
            Files.delete(tmp2.toPath());
        }
        if (this.overwrite)
        {
            Files.move(out.toPath(), in.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private byte[] extractLaunchScript(File file) throws IOException
    {
        final int startZipOffset;
        try (RandomAccessFile raf = new RandomAccessFile(file, "r"))
        {
            int nextZipFileHeaderPos = 0;
            int matches = 0;
            while (matches != ZIP_FILE_HEADER.length)
            {
                int b = raf.read();
                if (b == -1)
                {
                    throw new IOException("Cannot extract launch script");
                }
                if (b == ZIP_FILE_HEADER[nextZipFileHeaderPos])
                {
                    matches++;
                    nextZipFileHeaderPos++;
                }
                else
                {
                    matches = 0;
                    nextZipFileHeaderPos = 0;
                }
            }
            startZipOffset = (int) raf.getFilePointer() - ZIP_FILE_HEADER.length;
        }
        final byte[] launchScript = new byte[startZipOffset];
        try (FileInputStream is = new FileInputStream(file))
        {
            is.read(launchScript);
        }
        return launchScript;
    }
    
    private void extractZipFile(File in, int offset, File out) throws IOException
    {
        try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(in));
                BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(out)))
        {
            for (int i = 0; i < offset; i++)
            {
                is.read();
            }
            int b;
            while ((b = is.read()) != -1)
            {
                os.write(b);
            }
        }
    }
    
    private void repackLaunchScript(byte[] launchScript, File in, File out) throws IOException
    {
        try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(in));
                BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(out)))
        {
            os.write(launchScript);
            int b;
            while ((b = is.read()) != -1)
            {
                os.write(b);
            }
        }
    }
}
