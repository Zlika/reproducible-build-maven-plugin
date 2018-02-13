import java.nio.file.Files;

// Check that both artifacts produced by the two builds are the same
byte[] content1 = Files.readAllBytes(new File(basedir, "target1/same-jar-1.0-SNAPSHOT-tar.tar").toPath());
byte[] content2 = Files.readAllBytes(new File(basedir, "target2/same-jar-1.0-SNAPSHOT-tar.tar").toPath());
assert content1 == content2
