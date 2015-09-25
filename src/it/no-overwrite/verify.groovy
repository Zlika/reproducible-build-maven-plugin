File artifact1 = new File(basedir, "target/no-overwrite-1.0-SNAPSHOT.jar")
File artifact2 = new File(basedir, "target/no-overwrite-1.0-SNAPSHOT-stripped.jar")
assert artifact1.exists()
assert artifact2.exists()
