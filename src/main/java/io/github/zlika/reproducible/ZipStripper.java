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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Strips non-reproducible data from a ZIP file.
 * It rebuilds the ZIP file with a predictable order for the zip entries and sets zip entry dates to a fixed value.
 */
final class ZipStripper implements Stripper
{
    private static final int BUFFER_SIZE = 1024;
    private final Map<File, Stripper> subFilters = new HashMap<>();
    private final File tempFolder;
    
    /**
     * Constructor.
     * @param tempFolder the temporary folder to unzip the file.
     */
    public ZipStripper(File tempFolder)
    {
        this.tempFolder = tempFolder;
    }
    
    /**
     * Adds a stripper for a given file in the Zip.
     * @param filename the name of the file in the Zip.
     * @param stripper the stripper to apply on the file.
     * @return this object (for method chaining).
     */
    public ZipStripper addFileStripper(String filename, Stripper stripper)
    {
        subFilters.put(new File(tempFolder, filename), stripper);
        return this;
    }
    
    @Override
    public void strip(InputStream is, OutputStream os) throws IOException
    {
        try (ZipInputStream zis = new ZipInputStream(is))
        {
            unzipToFolder(zis, tempFolder);
        }
        final List<String> sortedNames = sortFilesByName(tempFolder);
        try (ZipOutputStream zos = new ZipOutputStream(os))
        {
            sortedNames.stream().forEach(name ->
                {
                    try
                    {
                        addFileToZip(name, zos);
                    }
                    catch (IOException e)
                    {
                        throw new UncheckedIOException(e);
                    }
                });
        }
    }
    
    private void addFileToZip(String name, ZipOutputStream zos) throws IOException
    {
        final File file = new File(tempFolder, name);
        if (!file.isDirectory())
        {
            final ZipEntry strippedZe = new ZipEntry(name);
            strippedZe.setTime(0);
            zos.putNextEntry(strippedZe);
            final Stripper fileStripper = subFilters.get(file);
            try (FileInputStream fis = new FileInputStream(file))
            {
                if (fileStripper != null)
                {
                    fileStripper.strip(fis, zos);
                }
                else
                {
                    copy(fis, zos);
                }
            }
            zos.closeEntry();
        }
        else
        {
            final ZipEntry strippedZe = new ZipEntry(name + "/");
            strippedZe.setTime(0);
            zos.putNextEntry(strippedZe);
        }
    }
    
    private void unzipToFolder(ZipInputStream zis, File folder) throws IOException
    {
        deleteFolder(folder);
        createFolders(folder);
        ZipEntry ze;
        while ((ze = zis.getNextEntry()) != null)
        {
            if (ze.isDirectory())
            {
                createFolders(new File(folder, ze.getName()));
            }
            else
            {
                final File file = new File(folder, ze.getName());
                createFolders(file.getParentFile());
                try (BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file)))
                {
                    copy(zis, os);
                }
            }
            zis.closeEntry();
        }
    }
    
    private void deleteFolder(File folder) throws IOException
    {
        if (folder.exists())
        {
            Files.walkFileTree(folder.toPath(), new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
                {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
                {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
    
    private List<String> sortFilesByName(File folder) throws IOException
    {
        return Files.walk(folder.toPath())
                .map(f -> folder.toPath().relativize(f).toString())
                .filter(s -> !s.isEmpty())
                .sorted()
                .collect(Collectors.toList());
    }
    
    private void createFolders(File folder) throws IOException
    {
        if (!folder.exists() && !folder.mkdirs())
        {
            throw new IOException("Cannot create folder " + folder.getAbsolutePath());
        }
    }

    private void copy(InputStream is, OutputStream os) throws IOException
    {
        final byte[] buffer = new byte[BUFFER_SIZE];
        while (is.available() > 0)
        {
            int read = is.read(buffer);
            if (read > 0)
            {
                os.write(buffer, 0, read);
            }
        }
    }
}
