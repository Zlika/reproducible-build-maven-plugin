import java.nio.file.Files

File actual = new File(basedir, "target/generated-sources/jaxb/ObjectFactory.java")
File expected = new File(basedir, "ObjectFactory-fixed.java")
byte[] content1 = Files.readAllBytes(actual.toPath())
byte[] content2 = Files.readAllBytes(expected.toPath())
assert content1 == content2
