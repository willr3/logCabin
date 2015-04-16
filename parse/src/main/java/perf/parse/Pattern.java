package perf.parse;

import org.json.JSONArray;
import org.json.JSONObject;
import perf.parse.util.file.Parser;

import java.util.*;
import java.util.regex.Matcher;

/**
 *
 */
public class Pattern {

    public static enum Match { NewMatch, Repeat, LineStart, ConsumeSection, ConsumeLine, SetContext, PopContext}

    public static enum Merge { Default, KeyValue, Previous}//KeyValue is technically a merge but really impacts how types are managed

    public static enum Type { List, BooleanKey, BooleanValue, Count, String, Sum}

    private final Matcher nameMatcher = java.util.regex.Pattern.compile("\\(\\?<([^>]+)>").matcher("");

    private boolean debug = false;

    private HashMap<String,Type> namedMatches;

    private String name;
    private Matcher matcher;
    private String state = null;

    private Parser parser = null;

    private Set<Merge> mergeRules;
    private Set<Match> matchRules;

    private List<Pattern> children;

    private List<MatchAction> actions;

    private static enum GroupRule { String,Key}

    private LinkedHashMap<String,GroupRule> groupBy;

    public Pattern(String name, String pattern){
        this.name = name;
        this.matcher = java.util.regex.Pattern.compile(pattern).matcher("");
        this.namedMatches = parsePattern(pattern);

        this.groupBy = new LinkedHashMap<String, GroupRule>();
        this.children = new LinkedList<Pattern>();
        this.mergeRules = new HashSet<Merge>();
        this.matchRules = new HashSet<Match>();
        this.actions = new LinkedList<MatchAction>();


    }

    public void setPattern(Parser parser){
        this.parser = parser;
    }
    public Parser getParser(){return parser;}

    public Pattern debug(){
        this.debug=true;
        return this;
    }
    public boolean isDebug(){return this.debug;}
    public String getName(){return name;}

    public Pattern setState(String state){
        this.state = state;
        return this;
    }
    public Pattern addChild(Pattern child){
        children.add(child);
        return this;
    }

    public Pattern onMatch(MatchAction action){
        actions.add(action);
        return this;
    }

    public Pattern groupBy(String group){
        groupBy.put(group, GroupRule.String);
        return this;
    }
    public Pattern groupByValue(String key){
        groupBy.put(key, GroupRule.Key);
        return this;
    }

    public Pattern set(String name,Type rule){
        namedMatches.replace(name,rule);
        return this;
    }

    public Pattern set(Merge rule){
        mergeRules.add(rule);
        return this;
    }

    public Pattern set(Match rule){
        matchRules.add(rule);
        return this;
    }

    public boolean is(Match rule){
        return matchRules.contains(rule);
    }
    public boolean is(Merge rule){
        return mergeRules.contains(rule);
    }
    public boolean is(String name,Type rule){
        return namedMatches.containsKey(name) && rule == namedMatches.get(name);
    }
    public JSONObject getMatch(StringBuffer line){
        return getMatch(line,0);
    }
    public JSONObject getMatch(StringBuffer line,int start){
        if(isDebug()){
            System.out.println("getMatch("+line+", "+start+")");
        }
        matcher.reset(line);
        if(! is(Match.LineStart) ){
           matcher.region(start,line.length());
        }
        if ( !matcher.find() ){
            return null;
        }

        JSONObject rtrn = buildJSON(line,start);


        if (rtrn == null ){
            return null;
        }

        if( children.size() > 0 ){
            JsonBuilder builder = new JsonBuilder(rtrn);
            for( Pattern child : children ){
                JSONObject childMatch = child.getMatch(line,start);
                if(childMatch != null){
                    child.mergeTo(childMatch,builder);

                }
            }
        }
        JSONObject grouped = rtrn;
        for ( String key : groupBy.keySet()){
            GroupRule rule = groupBy.get(key);
            JSONObject push = new JSONObject();
            switch(rule){
                case Key:
                    push.put(rtrn.get(key).toString(),grouped);
                    break;
                case String:
                    push.put(key,grouped);
                    break;
            }
            grouped = push;
        }
        rtrn = grouped; // will only change the referenced object if there was a groupBy rule

        for(MatchAction action : actions){
            //action.onMatch(rtrn,this,getParser());
        }

        return rtrn;
    }

    private JSONObject buildJSON(StringBuffer line, int start){
        if(isDebug()){
            System.out.println("buildJSON("+line+", "+start+")");
        }
        JSONObject rtrn = new JSONObject();

        matcher.region(start,line.length());

        if ( !matcher.find() ){
            return null;
        }
        if ( is(Merge.KeyValue) ) {
            rtrn.accumulate(matcher.group("key"), matcher.group("value"));
        } else {

            for( String key : namedMatches.keySet() ) {
                Type type = namedMatches.get(key);
                String value = matcher.group(key);

                switch(type){
                    case Count:
                        rtrn.increment(value);
                        break;
                    case Sum:
                        rtrn.put( key, rtrn.optDouble(key,0) + Double.parseDouble(value) );
                        break;
                    case BooleanKey:
                        rtrn.put(key, true);
                        break;
                    case BooleanValue:
                        rtrn.put(value, true);
                        break;
                    case String:
                        rtrn.put(key, rtrn.getString(key) + value);
                        break;
                    case List:
                    default:
                        rtrn.accumulate(key, value);
                        break;
                }
            }
        }
        int matchStart = matcher.start();
        int matchEnd = matcher.end();
        if(isDebug()){
            System.out.println(" matchStart="+matchStart+" matchEnd ="+matchEnd);
        }
        if ( is(Match.ConsumeSection)){
            line.replace(matchStart,matchEnd,"");
            matcher.region(matchStart,line.length());
            if(isDebug()){
                System.out.println("ConSumeSection ["+matchStart+", "+matchEnd+")");
            }
            matchEnd = matchStart;
        }else{
            //matchEnd = matchStart + 1;
        }
        if ( is(Match.Repeat) ) {
            JSONObject next = this.buildJSON(line,matchEnd);
            if(next!=null) {
                if(next.has(this.name)) {
                    next.accumulate(this.name, rtrn);
                }else{ //TODO key value doesn't need this treatment
                    JSONObject newNext = new JSONObject();
                    newNext.accumulate(this.name,next);
                    newNext.accumulate(this.name,rtrn);
                    next = newNext;
                }

                //rtrn = new JSONObject();
                //rtrn.put(this.name,next);
                rtrn = next;
            }

        }
        return rtrn;
    }


    public void mergeTo(JSONObject from,JsonBuilder builder){
        if( is(Match.NewMatch) ){
            //TODO mergeTo for a NewMatch is an error?
            return;
        }

        JSONObject target = builder.getCurrentContext();
        for(String key : from.keySet()){
            JSONObject keyTarget = target;
            Type type = namedMatches.containsKey(key) ? namedMatches.get(key) : Type.List;

            if( is(Merge.Previous) && from.get(key) instanceof JSONObject) {
                if( keyTarget.has(key) ) {
                    Object named = keyTarget.get(key);
                    if( named instanceof JSONArray){
                        keyTarget = ((JSONArray)named).getJSONObject(((JSONArray)named).length()-1);
                    }else {

                    }
                    JSONObject subFrom = from.getJSONObject(key);
                    for(String sKey : subFrom.keySet()){
                        keyTarget.accumulate( sKey,subFrom.get(sKey) );
                    }
                }else {
                    target.accumulate(key,from.get(key));
                }
            } else {
                switch(type){
                    case Count:
                        target.increment(key);
                        break;
                    case Sum:
                        target.put( key, target.optDouble(key, 0) + Double.parseDouble(from.getString(key)) );
                        break;
                    case BooleanKey:
                        target.put(key, true);
                        break;
                    case BooleanValue: // this conversion is handled by buildJSON
                        target.put(key, true);
                        break;
                    case String:
                        target.put(key, target.getString(key) + from.getString(key));
                        break;
                    case List:
                    default:
                        target.accumulate(key, from.get(key));
                        break;
                }

            }

        }

        if( is(Match.PopContext) ){
            builder.popContext();
        }

        if( is(Match.SetContext) && !groupBy.isEmpty()){

            List<String> reversedKeys = Arrays.asList( groupBy.keySet().toArray(new String[groupBy.size()] ) );
            Collections.reverse(reversedKeys);

            JSONObject newContext = target;
            for(String key : reversedKeys){
                newContext = newContext.getJSONObject(key);
            }
            builder.setCurrentContext(newContext);
        }
    }

    protected HashMap<String,Type> parsePattern(String pattern){
        HashMap<String,Type> rtrn = new HashMap<String, Type>();
        nameMatcher.reset(pattern);
        while(nameMatcher.find()){
            rtrn.put(nameMatcher.group(1),Type.List);
        }
        return rtrn;
    }
}
