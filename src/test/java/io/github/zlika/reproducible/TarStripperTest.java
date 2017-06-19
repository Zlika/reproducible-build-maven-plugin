package io.github.zlika.reproducible;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.Assert;
import org.junit.Test;

public class TarStripperTest {

    /**
     * Tests stripping on a reference Tar file.
     * @throws IOException
     */
    @Test
    public void testStripZip() throws IOException
    {
        final String testTarName = "test-tar.tar";
        final String strippedTarName = "test-tar-stripped.tar";
        
        final File inFile = new File(this.getClass().getResource(testTarName).getFile());
        final File outFile = File.createTempFile("test-tar", "tar");
        outFile.deleteOnExit();
        final File expected = new File(this.getClass().getResource(strippedTarName).getFile());
        
        new TarStripper()
            .addFileStripper("META-INF/MANIFEST.MF", new ManifestStripper())
            .addFileStripper("META-INF/\\S*/pom.properties", new PomPropertiesStripper())
            .strip(inFile, outFile);

        Assert.assertArrayEquals(Files.readAllBytes(expected.toPath()), Files.readAllBytes(outFile.toPath()));
        outFile.delete();
    }
	
}
