// $Id$
import org.codehaus.mojo.unix.rpm.RpmUtil
import org.codehaus.mojo.unix.rpm.RpmUtil.FileInfo

import java.io.*

boolean success = true

def rpm = new File(localRepositoryPath, "org/codehaus/mojo/rpm/its/rpm-1/1.0/rpm-1-1.0.rpm")
if (!rpm.exists())
{
    throw new java.lang.AssertionError("RPM artifact not created: " + rpm.getAbsolutePath());
}

def spec = RpmUtil.getSpecFileFromRpm(rpm)
if (!spec.name.equals("rpm-1"))
{
    throw new java.lang.AssertionError("Incorrect name in spec file: rpm-1 =! " + spec.name);
}
if (!spec.version.equals("1.0"))
{
    throw new java.lang.AssertionError("Incorrect version in spec file: 1.0 != " + spec.version);
}
if (!spec.release == 1)
{
    throw new java.lang.AssertionError("Incorrect release in spec file: 1 != " + spec.release);
}
if (!spec.license.equals("(c) My self - 2013"))
{
    throw new java.lang.AssertionError("Incorrect license in spec file: (c) My self - 2013 != " + spec.license);
}

List fileInfos = RpmUtil.queryPackageForFileInfo(rpm)

int fileCnt = fileInfos.size()
if (fileCnt != 15)
{
    throw new java.lang.AssertionError("Incorrect file count: 15 != " + fileCnt);
}

boolean nameScript = false;
boolean osNameScript = false;
String expectedOsNameScript = "name-" + System.getProperty("os.name") + ".sh";
boolean oldNameLink = false;
boolean tempLink = false;
boolean unversionedWar = false;

for (Iterator i = fileInfos.iterator(); i.hasNext();)
{
    FileInfo fileInfo = (FileInfo) i.next()
    System.out.println("File: " + fileInfo.path);
    if (!fileInfo.user.equals("myuser"))
    {
        throw new java.lang.AssertionError("Incorrect user in file info: myuser !=" + fileInfo.user);
    }
    if (!fileInfo.group.equals("mygroup"))
    {
        throw new java.lang.AssertionError("Incorrect group in file info: mygroup != " + fileInfo.group);
    }

    //check for executable mode
    if (fileInfo.path.startsWith("/usr/myusr/app/bin/"))
    {
        //oldname.sh is a link, so the filemode is different
        if (fileInfo.path.endsWith("/oldname.sh"))
        {
            oldNameLink = true;
            if (!fileInfo.mode.equals("lrwxr-xr-x"))
            {
                throw new java.lang.AssertionError("Incorrect mode for '" + fileInfo.path + "': lrwxr-xr-x != " + fileInfo.mode);
            }
        }
        else
        {
            if (!fileInfo.mode.equals("-rwxr-xr-x"))
            {
                throw new java.lang.AssertionError("Incorrect mode for '" + fileInfo.path + "': -rwxr-xr-x != " + fileInfo.mode);
            }

            if (fileInfo.path.endsWith("/name.sh"))
            {
                nameScript = true;
            }
            else if (fileInfo.path.endsWith(expectedOsNameScript))
            {
                osNameScript = true;
            }
        }
    }
    else if (fileInfo.path.equals("/tmp/myapp/somefile2"))
    {
        tempLink = true;
        if (!fileInfo.mode.startsWith("l"))
        {
            throw new java.lang.AssertionError("temp link mode: '" + fileInfo.mode + "' doesn't start with 'l'");
        }
    }
    else if (fileInfo.path.endsWith("grizzly-comet-counter.war"))
    {
        unversionedWar = true;
    }
}

if (!tempLink)
{
    throw new java.lang.AssertionError("Temp link missing");
}
if (!nameScript)
{
    throw new java.lang.AssertionError("Name script missing");
}
if (!osNameScript)
{
    throw new java.lang.AssertionError("OS name script missing, expected: " + expectedOsNameScript);
}
if (!oldNameLink)
{
    throw new java.lang.AssertionError("Old name script missing");
}
if (!unversionedWar)
{
    throw new java.lang.AssertionError("Unversioned war missing");
}

//now test that we actually filtered the file
def proc = ["sh", "-c", "rpm2cpio ${rpm.getAbsolutePath()} | cpio -iv --to-stdout '.*filter.txt'"].execute()
proc.waitFor()
content = proc.in.text
if (!"org.codehaus.mojo.rpm.its".equals(content))
    throw new java.lang.AssertionError("contents of filter.txt expected[org.codehaus.mojo.rpm.its] actual[${content}]");

proc = ["sh", "-c", "rpm2cpio ${rpm.getAbsolutePath()} | cpio -iv --to-stdout '.*filter-version.txt'"].execute()
proc.waitFor()
content = proc.in.text

if (!"1.0-1".equals(content))
    throw new java.lang.AssertionError("contents of filter-version.txt expected[1.0-1] actual[${content}]");

return success
