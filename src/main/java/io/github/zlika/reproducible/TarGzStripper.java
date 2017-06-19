package io.github.zlika.reproducible;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

public class TarGzStripper extends TarStripper {

	protected TarArchiveInputStream createInputStream(File in) throws FileNotFoundException, IOException {
		return new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(in)));
	}

	protected TarArchiveOutputStream createOutputStream(File out) throws FileNotFoundException, IOException {
		return new TarArchiveOutputStream(new GzipCompressorOutputStream(new FileOutputStream(out)));
	}

}
