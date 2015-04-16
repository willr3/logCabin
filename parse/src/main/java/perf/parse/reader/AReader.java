package perf.parse.reader;

import perf.parse.util.file.FileUtility;
import perf.parse.util.file.Parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 *
 */
public abstract class AReader {


    protected abstract void processInputStream(InputStream stream);

    public void read(String path) {

        Iterator<Parser> iter = null;
        iter = parsers();
        while(iter.hasNext()){
            iter.next().setup();
        }
        try {
            processInputStream(FileUtility.getInputStream(FileUtility.getFile(path)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        iter = parsers();
        while(iter.hasNext()){
            iter.next().close();
        }
    }

    private Set<Parser> parsers;

    public AReader(){
        parsers = new HashSet<Parser>();
    }

    public void addParser(Parser toAdd){
        parsers.add(toAdd);
    }
    public int parserCount(){return parsers.size();}

    protected Iterator<Parser> parsers(){return parsers.iterator();}
}
