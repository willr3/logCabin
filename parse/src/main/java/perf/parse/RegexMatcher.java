package perf.parse;

import java.util.regex.*;

/**
 *
 */
public class RegexMatcher implements IMatcher {

    private Matcher matcher;

    public RegexMatcher(String pattern){
        matcher = java.util.regex.Pattern.compile(pattern).matcher("");
    }


    public void reset(CharSequence input){
        matcher.reset(input);
    }
    public boolean find(){
        return matcher.find();
    }
    public void region(int start,int end){
        matcher.region(start, end);
    }
    public int start(){
        return matcher.start();
    }
    public int end(){
        return matcher.end();
    }
    public String group(String name){
        return matcher.group(name);
    }
}
