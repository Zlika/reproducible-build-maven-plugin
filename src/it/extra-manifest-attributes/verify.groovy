def zip = new java.util.zip.ZipFile(new File(basedir, "target/extra-manifest-attributes-1.0-SNAPSHOT.jar"))
def manifest = null
zip.entries().findAll{ it.name == "META-INF/MANIFEST.MF" }
    .each{ manifest = zip.getInputStream(it).text }
assert manifest.trim().isEmpty()

