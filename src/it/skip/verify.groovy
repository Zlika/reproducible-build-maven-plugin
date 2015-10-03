// The build.log file seems to be created only after the execution of this verification script!
/*
import java.nio.file.Files

File log = new File(basedir, "target/build.log")
List<String> lines = Files.readAllLines(log.toPath())
int skips = 0
for (line in lines)
{
    if (line.contains("Skipping execution of goal"))
    {
        skips++
    }
}
assert skips == 2
*/