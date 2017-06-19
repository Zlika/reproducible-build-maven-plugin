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

public class TarBzStripper extends TarStripper {
	
	protected TarArchiveInputStream createInputStream(File in) throws FileNotFoundException, IOException {
		return new TarArchiveInputStream(new BZip2CompressorInputStream(new FileInputStream(in)));
	}

	protected TarArchiveOutputStream createOutputStream(File out) throws FileNotFoundException, IOException {
		return new TarArchiveOutputStream(new BZip2CompressorOutputStream(new FileOutputStream(out)));
	}

}
