import java.nio.file.Files;

["strip-tar-1.0-SNAPSHOT-tar.tar", "strip-tar-1.0-SNAPSHOT-tar.tar.gz", "strip-tar-1.0-SNAPSHOT-tar.tar.bz2"].each { file ->
    // Check that both archives produced by the two builds are the same
    byte[] content1 = Files.readAllBytes(new File(basedir, "target1/${file}").toPath());
    byte[] content2 = Files.readAllBytes(new File(basedir, "target2/${file}").toPath());
    assert content1 == content2
}

return true