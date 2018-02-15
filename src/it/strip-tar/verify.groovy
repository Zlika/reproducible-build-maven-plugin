import java.nio.file.Files;

["same-jar-1.0-SNAPSHOT-tar.tar", "same-jar-1.0-SNAPSHOT-tar.tar.gz", "same-jar-1.0-SNAPSHOT-tar.tar.bz2"].each { file ->
    // Check that both artifacts produced by the two builds are the same
    byte[] content1 = Files.readAllBytes(new File(basedir, "target1/${file}").toPath());
    byte[] content2 = Files.readAllBytes(new File(basedir, "target2/${file}").toPath());
    assert content1 == content2
}

return true