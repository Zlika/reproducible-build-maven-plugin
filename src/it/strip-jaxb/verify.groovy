import java.nio.file.Files

// Checks that the JAXB generated ObjectFactory file has no timestamp and its methods are in a reproducible order
File actual = new File(basedir, "target/generated-sources/jaxb/ObjectFactory.java")
File expected = new File(basedir, "ObjectFactory-fixed.java")
byte[] content1 = Files.readAllBytes(actual.toPath())
byte[] content2 = Files.readAllBytes(expected.toPath())
assert content1 == content2

// Checks that the JAXB generated file has no timestamp
actual = new File(basedir, "target/generated-sources/jaxb/Book.java")
expected = new File(basedir, "Book-fixed.java")
content1 = Files.readAllBytes(actual.toPath())
content2 = Files.readAllBytes(expected.toPath())
assert content1 == content2

// Checks that the JAXB generated episode file has no timestamp
actual = new File(basedir, "target/generated-sources/jaxb/META-INF/sun-jaxb.episode")
expected = new File(basedir, "sun-jaxb-fixed.episode")
content1 = Files.readAllBytes(actual.toPath())
content2 = Files.readAllBytes(expected.toPath())
assert content1 == content2

