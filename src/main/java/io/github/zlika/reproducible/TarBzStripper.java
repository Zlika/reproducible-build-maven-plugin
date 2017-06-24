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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

/**
 * Stripper implementation for tar compressed with bz2.
 * 
 * @author tglman
 *
 */
public class TarBzStripper extends TarStripper
{

    @Override
    protected TarArchiveInputStream createInputStream(File in) throws FileNotFoundException, IOException
    {
        return new TarArchiveInputStream(new BZip2CompressorInputStream(new FileInputStream(in)));
    }

    @Override
    protected TarArchiveOutputStream createOutputStream(File out) throws FileNotFoundException, IOException
    {
        return new TarArchiveOutputStream(new BZip2CompressorOutputStream(new FileOutputStream(out)));
    }

}
