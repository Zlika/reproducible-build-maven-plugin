import java.nio.file.Files;

// Try to load the produced test artifact to check it is ok
File artifact1 = new File(basedir, "target1/same-jar-1.0-SNAPSHOT.jar");
this.class.classLoader.parent.parent.addURL(artifact1.toURI().toURL());
def cls = Class.forName("io.github.zlika.it.Main").newInstance();
cls.main();

// Check that both artifacts produced by the two builds are the same
byte[] content1 = Files.readAllBytes(artifact1.toPath());
byte[] content2 = Files.readAllBytes(new File(basedir, "target2/same-jar-1.0-SNAPSHOT.jar").toPath());
assert content1 == content2
