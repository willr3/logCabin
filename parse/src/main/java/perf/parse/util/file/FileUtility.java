package perf.parse.util.file;

import perf.parse.util.file.ZipEntryFile;

import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 *
 */
public class FileUtility {

    public static final String ARCHIVE_KEY = "#";

    public static List<String> getFiles(String baseDir, String nameSubstring,
                                 boolean recursive) {
        return search(baseDir, nameSubstring, recursive, true, false, false);
    }

    public static List<String> getDirectories(String baseDir, String nameSubstring,
                                       boolean recursive) {
        return search(baseDir, nameSubstring, recursive, false, true, false);
    }

    public static boolean isArchiveEntryPath(String fileName) {
        if(fileName == null || !fileName.contains(ARCHIVE_KEY)){
            return false;
        }
        File parentFile = new File(fileName.substring(0,fileName.indexOf(ARCHIVE_KEY)));
        File tmpFile = new File(fileName);

        return (!tmpFile.exists() && parentFile.exists());

    }

    public static InputStream getInputStream(File file){
        InputStream rtrn = null;
        if(file instanceof ZipEntryFile){
            ZipEntryFile zef = (ZipEntryFile)file;
            try {
                rtrn = zef.getZipFile().getInputStream(zef.getZipEntry());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            try {
                rtrn = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return rtrn;
    }



    public static String getArchiveEntrySubPath(String archiveEntryPath) {
        if (archiveEntryPath == null || archiveEntryPath.isEmpty())
            return "";
        if (isArchiveEntryPath(archiveEntryPath))
            return archiveEntryPath.substring(archiveEntryPath
                    .indexOf(ARCHIVE_KEY) + ARCHIVE_KEY.length());

        return "";
    }
    public static String getArchiveFilePath(String archiveEntryPath) {
        if (archiveEntryPath == null || archiveEntryPath.isEmpty())
            return "";
        if (isArchiveEntryPath(archiveEntryPath))
            return archiveEntryPath.substring(0,
                    archiveEntryPath.indexOf(ARCHIVE_KEY));

        return "";
    }

    public static File getFile(String filePath) throws IOException {
        File rtrn = null;
        if (isArchiveEntryPath(filePath)) {
            String archiveFilePath = getArchiveFilePath(filePath);
            String subPath = getArchiveEntrySubPath(filePath);
            ZipFile zip = new ZipFile(archiveFilePath);
            ZipEntry ze = zip.getEntry(subPath);
            rtrn = new ZipEntryFile(ze, zip);
        }else{
            rtrn = new ImmutableFile(filePath);
        }

        return rtrn;
    }

    public static String getDirectory(String fileName) {
        if (isArchiveEntryPath(fileName))
            fileName = fileName.substring(0, fileName.indexOf(ARCHIVE_KEY));
        if (fileName.contains("\\")) {
            fileName = fileName.replaceAll("\\\\", "/");
        }
        if (fileName.endsWith("/"))
            return fileName;
        else
            return fileName.substring(0, fileName.lastIndexOf("/") + 1);
    }
    public static String getParentDirectory(String fileName){
        if(fileName==null)
            return "";
        if (fileName.contains("\\")) {
            fileName = fileName.replaceAll("\\\\", "/");
        }
        if (fileName.endsWith("/"))
            fileName=fileName.substring(0, fileName.length()-1);

        return fileName.substring(0, fileName.lastIndexOf("/")+1);
    }
    private static final List<String> search(String baseDir, String nameSubstring, boolean recursive, boolean wantFiles, boolean depthFirst, boolean inArchive) {
        List<String> rtrn = new ArrayList<String>();
        List<String> toParse = new ArrayList<String>();
        toParse.add(baseDir);

        while (!toParse.isEmpty()) {
            try {
                String next = toParse.remove(0);
                File f = getFile(next);
                if (f.exists() && f.isDirectory()) {
                    for (File sub : f.listFiles()) {
                        if (recursive && sub.isDirectory()) {
                            if (depthFirst)
                                toParse.add(0, sub.getAbsolutePath());
                            else
                                toParse.add(sub.getAbsolutePath());
                        }
                        // probably don't need both boolean comparisons but I'm
                        // curious if isFile!=isDirectory is a contract
                        if (sub.isFile() == wantFiles && sub.isDirectory() != wantFiles) {
                            // if there is name filtering
                            if (nameSubstring != null
                                    && !nameSubstring.isEmpty()) {
                                if (sub.getName().contains(nameSubstring) && !isArchive(sub)) {
                                    rtrn.add(sub.getAbsolutePath());
                                }
                            } else if (!isArchive(sub)){
                                rtrn.add(sub.getAbsolutePath());
                            }
                        }
                        if (inArchive && sub.isFile() && isArchive(sub)) {

                            try {
                                ZipFile zip = new ZipFile(sub);
                                Enumeration<? extends ZipEntry> entries = zip
                                        .entries();
                                while (entries.hasMoreElements()) {
                                    ZipEntry entry = entries.nextElement();
                                    String toAdd = ZipEntryFile.getEntryPath(entry, zip);
                                    if (depthFirst)
                                        toParse.add(0, toAdd);
                                    else
                                        toParse.add(toAdd);
                                }
                            } catch (ZipException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    }

                }
            } catch (ZipException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return rtrn;
    }
    public static Object readObjectFile(String fileName){
        return readObjectFile(new File(fileName));
    }
    public static Object readObjectFile(File file){
        ObjectInputStream ois = null;
        Object rtrn = null;
        try{
            ois = new ObjectInputStream(new FileInputStream(file));
            rtrn = ois.readObject();
            ois.close();
        }catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }finally{
            if(ois!=null){
                try {
                    ois.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return rtrn;
    }
    public static void writeObjectFile(String fileName,Object object){
        writeObjectFile(new File(fileName),object);
    }
    public static void writeObjectFile(File file,Object object){
        ObjectOutputStream oos = null;
        try{
            oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(object);
            oos.flush();
            oos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(oos!=null){
                try {
                    oos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    protected static boolean isArchive(File file) {
        String n = file.getName();
        if ( n.endsWith(".zip") || n.endsWith(".tar") || n.endsWith(".gz") || n.endsWith(".Z") || n.endsWith(".jar") || n.endsWith(".bzip2") ) {
            return true;
        }
        return false;
    }
}
