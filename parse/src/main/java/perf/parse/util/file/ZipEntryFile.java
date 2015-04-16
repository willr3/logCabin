package perf.parse.util.file;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 *
 */
public class ZipEntryFile extends ImmutableFile{

    private static final long serialVersionUID = 6178470866727739416L;

    ZipEntry zipEntry;
    ZipFile zipFile;
    String name;
    String path;
    String fullName;
    long bytes;

    public ZipEntryFile(ZipEntry zipEntry,ZipFile zipFile){
        super(zipEntry.getName());
        this.zipEntry = zipEntry;
        name = zipEntry.getName();
        path = zipFile.getName();
        fullName = getEntryPath(zipEntry,zipFile);
        bytes = this.zipEntry.getSize();
    }
    public ZipEntry getZipEntry(){return zipEntry;}
    public ZipFile getZipFile(){return zipFile;}
    public static String getEntryPath(ZipEntry zipEntry,ZipFile zipFile){
        return zipFile.getName()+FileUtility.ARCHIVE_KEY+zipEntry.getName();
    }
    @Override
    public String getName() {
        return name;
    }
    @Override
    public String getParent(){
        return path;
    }
    @Override
    public File getParentFile(){
        return new File(getParent());
    }
    @Override
    public String getPath(){
        return path;
    }
    @Override
    public boolean isAbsolute(){
        return false;
    }
    @Override
    public String getAbsolutePath(){
        return fullName;
    }
    @Override
    public File getAbsoluteFile(){
        return this;
    }
    @Override
    public String getCanonicalPath(){
        return fullName;
    }
    @Override
    public File getCanonicalFile() throws IOException {
        return this;
    }
    @Override
    public URL toURL() throws MalformedURLException {
        return new URL("file", "",escape(getAbsolutePath(),isDirectory()));
    }
    @Override
    public URI toURI(){
        try{
            File f = getAbsoluteFile();
            String sp = escape(f.getPath(),f.isDirectory());
            if(sp.startsWith("//"))
                sp = "//" + sp;
            return new URI("file",null,sp,null);
        }catch(URISyntaxException e){
            throw new RuntimeException(e);
        }
    }
    @Override
    public boolean canRead(){
        return true;
    }
    @Override
    public boolean canWrite(){
        return false;
    }
    @Override
    public boolean exists(){
        return true;
    }
    @Override
    public boolean isDirectory(){
        return zipEntry.isDirectory();
    }
    @Override
    public boolean isFile(){
        return !isDirectory();
    }
    @Override
    public boolean isHidden(){
        return true;
    }
    @Override
    public long lastModified(){
        return zipEntry.getTime();
    }
    @Override
    public long length(){
        return bytes;
    }
    @Override public String[] list(){
        return new String[]{};
    }
    @Override
    public String[] list(FilenameFilter filter) {
        return list();
    }
    @Override
    public File[] listFiles(){
        return new File[]{};
    }
    @Override
    public File[] listFiles(FilenameFilter filter) {
        return listFiles();
    }
    @Override
    public File[] listFiles(FileFilter filter) {
        return listFiles();
    }

    @Override
    public boolean canExecute() {return false;}
    @Override
    public long getTotalSpace() {
        //TODO fix getTotalSpace
        return -1;//return zipFile.getTotalSpace();
    }
    @Override
    public long getFreeSpace() {
        //TODO fix getFreeSpace
        return -1;//return zipFile.getFreeSpace();
    }
    @Override
    public long getUsableSpace() {
        //TODO fix getUsableSpace
        return -1;//return zipFile.getUsableSpace();
    }
    @Override
    public int compareTo(File pathname) {
        if(pathname==null){
            return 1;
        }else
            return fullName.compareTo(pathname.getAbsolutePath());
    }
    @Override
    public boolean equals(Object obj){
        return super.equals(obj);
    }

    @Override
    public int hashCode(){
        //TODO hash function is probably broken
        return super.hashCode();
    }
    private static String escape(String path, boolean isDirectory) {
        String p = path;
        if (File.separatorChar != '/')
            p = p.replace(File.separatorChar, '/');
        if (!p.startsWith("/"))
            p = "/" + p;
        if (!p.endsWith("/") && isDirectory)
            p = p + "/";
        return p;
    }
}
