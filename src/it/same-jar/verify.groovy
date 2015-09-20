import java.nio.file.Files;

// Try to load the produced test artifact to check it is ok
File artifact = new File(basedir, "target/same-jar-1.0-SNAPSHOT.jar");

this.class.classLoader.rootLoader.addURL(artifact.toURI().toURL());
def cls = Class.forName("io.github.zlika.it.Main").newInstance();
cls."main";

byte[] artifact1 = Files.readAllBytes(artifact);
// Relaunch Maven and check that the produced artifact is still the same
assert "mvn clean package".execute().exitValue()
byte[] artifact2 = Files.readAllBytes(artifact);
assert artifact1 == artifact2
