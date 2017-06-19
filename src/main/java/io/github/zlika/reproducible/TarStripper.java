package io.github.zlika.reproducible;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

public class TarStripper extends AbstractStripper {

	protected TarArchiveInputStream createInputStream(File in) throws FileNotFoundException, IOException {
		return new TarArchiveInputStream(new FileInputStream(in));
	}

	protected TarArchiveOutputStream createOutputStream(File out) throws FileNotFoundException, IOException {
		return new TarArchiveOutputStream(new FileOutputStream(out));
	}

	@Override
	public void strip(File in, File out) throws IOException {
		final Path tmp = Files.createTempDirectory("tmp-" + in.getName());

		List<TarArchiveEntry> sortedNames = new ArrayList<>();
		try (final TarArchiveInputStream tar = createInputStream(in)) {

			TarArchiveEntry entry;
			while ((entry = tar.getNextTarEntry()) != null) {
				sortedNames.add(entry);
				File copyTo = new File(tmp.toFile(), entry.getName());
				if (entry.isDirectory()) {
					copyTo.mkdirs();
				} else {
					File destParent = copyTo.getParentFile();
					destParent.mkdirs();

					final Stripper stripper = getSubFilter(entry.getName());
					if (stripper != null) {
						File dumpTo = new File(tmp.toFile(), entry.getName() + ".pre");
						dumpTo.createNewFile();
						Files.copy(tar, dumpTo.toPath());
						stripper.strip(dumpTo, copyTo);
					} else {
						Files.copy(tar, copyTo.toPath());
					}
				}
			}
		}
		sortedNames = sortTarEntries(sortedNames);
		try (final TarArchiveOutputStream tout = createOutputStream(out)) {
			for (TarArchiveEntry entry : sortedNames) {
				File copyFrom = new File(tmp.toFile(), entry.getName());
				if (!entry.isDirectory()) {
					final byte[] fileContent = Files.readAllBytes(copyFrom.toPath());
					entry.setSize(fileContent.length);
					tout.putArchiveEntry(filterTarEntry(entry));
					tout.write(fileContent);
					tout.closeArchiveEntry();
				} else {
					tout.putArchiveEntry(filterTarEntry(entry));
					tout.closeArchiveEntry();
				}
			}
		}
	}

	private List<TarArchiveEntry> sortTarEntries(List<TarArchiveEntry> sortedNames) {
		sortedNames = sortedNames.stream().sorted((a, b) -> a.getName().compareTo(b.getName()))
				.collect(Collectors.toList());
		return sortedNames;
	}

	private TarArchiveEntry filterTarEntry(TarArchiveEntry entry) {
		// Set times
		entry.setModTime(0);
		// Remove GID and UID that are not deterministic, to evaluate though
		entry.setGroupId(1000);
		entry.setUserId(1000);
		return entry;
	}

}
