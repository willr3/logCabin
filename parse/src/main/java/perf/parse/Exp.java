package perf.parse;

import org.json.JSONArray;
import org.json.JSONObject;
import perf.parse.util.file.Parser;

import java.util.*;
import java.util.regex.Matcher;

/**
 *
 */
public class Exp {

    public static final String CHILD_KEY = "_children";

    public enum Value {
        NestLength("_nestLength_"),
        Number("_number_"),
        KMG("_kmg_"),
        Count("_count_"),
        Sum("_sum_"),
        Key("_key_"),//will value of key will be the key for another key's value
        BooleanKey("_booleanKey_"),
        BooleanValue("_booleanValue_"),
        Position("_position_"),
        String("_string_"),
        List("_list_");

        private String id;
        private Value(String id){this.id = id;};
        public String getId(){return id;}

        public static Value from(String value){
            switch(value){
                case "_nestLength_": return NestLength;
                case "_number_": return Number;
                case "_kmg_": return KMG;
                case "_count_": return Count;
                case "_sum_": return Sum;
                case "_booleanKey_": return BooleanKey;
                case "_booleanValue_": return BooleanValue;
                case "_position_": return Position;
                case "_string_" : return String;
                case "_list_" : return List;
                case "_key_":
                default:
                    return Key;
            }
        }
    }
    public enum Eat {
        Width(1),
        None(0),
        Match(-1),
        Line(-2);
        private int id;
        private Eat(int id){
            this.id = id;
        }
        public int getId(){return id;}

        public static Eat from(int value){
            switch(value){
                case -2: return Line;
                case -1: return Match;
                case  0: return None;
                default: return Width;
            }
        }
    }
    public enum Rule {LineStart,Repeat,PushContext,AvoidContext,PopContext,ClearContext}
    public enum Merge {NewStart,Entry,Collection,Extend}


    private final Matcher fieldMatcher = java.util.regex.Pattern.compile("\\(\\?<([^>]+)>").matcher("");

    //Execute Rule
    private LinkedList<MatchAction> callbacks;

    private IMatcher matcher;
    private LinkedHashMap<String,String> fieldValues;

    private LinkedHashSet<Rule> rules;
    private LinkedList<String> grouping;

    private int eat=Eat.None.getId();
    private Merge merge = Merge.Collection;

    private String name;

    private LinkedList<Exp> children;

    private boolean debug=false;
    public Exp debug(){
        debug=true;
        return this;
    }
    public boolean isDebug(){return debug;}
    public String matcherClass(){return matcher.getClass().toString();}


    public Exp(String name, String pattern){
        this.name = name;

        this.matcher = /* StringMatcher.canMatch(pattern) ? new StringMatcher(pattern) :*/ new RegexMatcher(pattern);


        this.fieldValues = parsePattern(pattern);

        this.callbacks = new LinkedList<>();
        this.rules = new LinkedHashSet<>();
        this.grouping = new LinkedList<>();
        this.children = new LinkedList<>();

    }

    public String getName(){return this.name;}

    public static long parseKMG(String kmg){
        Matcher m = java.util.regex.Pattern.compile("(?<number>\\d+\\.?\\d*)(?<kmg>[kmgtpezyKMGTPEZY]*)(?<bB>[bB]*)").matcher(kmg);
        if(m.matches()){

            double mult = 1;

            switch(m.group("kmg").toUpperCase()){
                case "Y": mult*=1024;//8
                case "Z": mult*=1024;//7
                case "E": mult*=1024;//6
                case "P": mult*=1024;//5
                case "T": mult*=1024;//4
                case "G": mult*=1024;//3
                case "M": mult*=1024;//2
                case "K": mult*=1024;//1
            }
            double bytes = m.group("bB").equals("b") ? 1.0/8 : 1;
            Double v =Double.parseDouble(m.group("number"))*mult*bytes;
            return v.longValue();
        }else{
            throw new IllegalArgumentException(kmg+" does not match expected pattern for KMG");
        }
    }
    public LinkedHashMap<String,String> parsePattern(String pattern){
        LinkedHashMap<String,String> rtrn = new LinkedHashMap<>();

        fieldMatcher.reset(pattern);
        while(fieldMatcher.find()){
            rtrn.put(fieldMatcher.group(1),Value.List.getId());
        }
        return rtrn;
    }
    public List<String> getGroups(){return grouping;}
    public static boolean isExtendGroup(String value){return value.startsWith("^");}
    public static String  getExtendGroup(String value){return value.substring(1);}
    public static boolean isKeyGroup(String value){
        return value.startsWith("{") && value.endsWith("}");
    }
    public static String getKeyGroup(String value){
        return value.substring(1,value.length()-1);
    }
    //Set status methods, all return a pointer to this for chaining
    public Exp group(String name){
        grouping.add(name);
        return this;
    }
    public Exp key(String name){
        grouping.add("{"+name+"}");
        return this;
    }
    public Exp extend(String name){
        grouping.add("^"+name);
        return this;
    }
    public Exp eat(int width){
        eat = width;
        return this;
    }
    public Exp eat(Eat toEat){
        eat = toEat.getId();
        return this;
    }
    public Exp execute(MatchAction action){
        callbacks.add(action);
        return this;
    }
    public Exp add(Exp child){
        children.add(child);
        return this;
    }
    public boolean hasChildren(){return !children.isEmpty();}
    public int childCount(){return children.size();}
    public Exp set(String name,Value value){
        if(value == Value.Key){
            throw new IllegalArgumentException("set(String name,Value value) cannot be used for Value.Key, use Set(String name,String valueKey)");
        }
        fieldValues.put(name,value.getId());
        return this;
    }
    public Exp set(String name,String valueKey){
        if(fieldValues.containsKey(valueKey)){
            fieldValues.remove(valueKey);
        }
        fieldValues.put(name,valueKey);
        return this;
    }
    public Exp set(Rule rule){
        rules.add(rule);
        return this;
    }
    public Exp set(Merge merge){
        this.merge = merge;
        return this;
    }

    //Check status methods
    public boolean is(Rule r){
        return rules.contains(r);
    }
    public boolean is(Merge m){
        return this.merge == m;
    }
    public boolean is(String field,Value value){
        return value == Value.from(fieldValues.get(field));
    }

    public void populate(IMatcher m,JsonBuilder builder){
        JSONObject targetContext = builder.getCurrentContext();
        for(String fieldName : fieldValues.keySet()){
            String vString = fieldValues.get(fieldName);
            Value v = Value.from(vString);
            String fieldValue = m.group(fieldName);

            switch(v){
                case NestLength:

                    int length = fieldValue.length();
                    if( targetContext.has(fieldName) ) { // child
                        if( length > targetContext.getInt(fieldName) ){

                            JSONObject newChild = new JSONObject();
                            targetContext.append(CHILD_KEY,newChild);
                            builder.setCurrentContext(newChild);
                            targetContext = newChild;
                        } else if( length < targetContext.getInt(fieldName) ) { // parent

                            builder.popContext();
                            targetContext = builder.getCurrentContext();
                        } else { // sibling
                            builder.popContext();

                            targetContext = builder.getCurrentContext();
                            JSONObject newJSON = new JSONObject();
                            targetContext.append(CHILD_KEY, newJSON);
                            builder.setCurrentContext(newJSON);
                            targetContext = newJSON;
                        }
                    }
                    //NestLength starts a new Object
                    targetContext.put(fieldName,length);

                    break;
                case Number:
                    double number = Double.parseDouble(fieldValue);
                    targetContext.put(fieldName,number);
                    break;
                case KMG:
                    long kmg = parseKMG(fieldValue);
                    targetContext.put(fieldName,kmg);
                    break;
                case Count:
                    targetContext.increment(fieldValue);
                    break;
                case Sum:
                    double sum = Double.parseDouble(fieldValue);
                    targetContext.put(fieldName,targetContext.optDouble(fieldName,0)+sum);
                    break;
                case Key:
                    String keyValue = m.group(vString);
                    if(!keyValue.isEmpty()){
                        targetContext.put(fieldValue,keyValue);
                    }
                    break;
                case BooleanKey:
                    targetContext.put(fieldName,true);
                    break;
                case BooleanValue:
                    targetContext.put(fieldValue,true);
                    break;
                case Position:
                    targetContext.put(fieldName,m.start());
                    break;
                case String:
                    targetContext.put(fieldName,targetContext.optString(fieldName)+fieldValue);
                    break;
                case List:
                defaut:
                    targetContext.accumulate(fieldName,fieldValue);
                    break;
            }
        }
    }
    public boolean apply(CheatChars line,JsonBuilder builder,Parser parser){
        boolean result = applyWithStart(line,builder,parser,0);
        return result;
    }
    private boolean applyWithStart(CheatChars line,JsonBuilder builder,Parser parser,int start){
        if(isDebug()){
            System.out.println("line = "+line);
        }
        boolean rtrn = false;
        matcher.reset(line);
        matcher.region( is(Rule.LineStart) ? 0 : start,line.length() );

        if(matcher.find()){
            rtrn = true;
            if ( is(Merge.NewStart) ) {
                builder.close();
            }else if( is(Rule.AvoidContext) ) {
                builder.popContext();
            }

            JSONObject context = builder.getCurrentContext();
            JSONObject target = context;

            do {
                target = context;
                JSONObject grouped = target;
                for(int gi=0; gi < grouping.size(); gi++){
                    String groupName = grouping.get(gi);
                    boolean extend = false;
                    if( isKeyGroup(groupName) ) {
                        groupName = matcher.group(getKeyGroup(groupName));
                        if( groupName.isEmpty() ) {
                           throw new IllegalArgumentException("Cannot group with "+grouping.get(gi)+", match not found in line="+line);
                        }
                    }
                    if( isExtendGroup(groupName) ) {
                        extend=true;
                        groupName = getExtendGroup(groupName);
                    }
                    if (gi== grouping.size()-1) {
                        if ( is(Merge.Entry) ) {
                            if( grouped.has(groupName) ) {
                                JSONObject entry = new JSONObject();
                                grouped.append(groupName,entry);
                                grouped = entry;
                            } else {
                                JSONObject entry = new JSONObject();
                                JSONArray arry = new JSONArray();
                                arry.put(entry);
                                grouped.put(groupName,arry);
                                grouped = entry;
                            }
                        } else if ( is(Merge.Extend) ) {
                            if( grouped.has(groupName) ) {
                                JSONArray arry = grouped.getJSONArray(groupName);
                                JSONObject last = arry.getJSONObject(arry.length()-1);
                                grouped = last;
                            }else{
                                JSONObject entry = new JSONObject();
                                JSONArray arry = new JSONArray();
                                arry.put(entry);
                                grouped.put(groupName,arry);
                                grouped = entry;
                            }

                        } else if ( is(Merge.Collection) ) {
                            if( grouped.has(groupName) ) {
                                grouped = grouped.getJSONObject(groupName);
                            } else {
                                JSONObject newJSON = new JSONObject();
                                grouped.put(groupName,newJSON);
                                grouped = newJSON;
                            }
                        } else {
                            //will happen if NewEntry, no action because new-entry merges with the root context :)
                        }
                    } else {
                        //same behavior as Merge.Collection
                        if(grouped.has(groupName)){
                            //could be an array
                            Object obj = grouped.get(groupName);
                            if(obj instanceof JSONArray){
                                JSONArray groupArry = (JSONArray)obj;
                                if( (extend || is(Merge.Extend)) && groupArry.length()>0){
                                    grouped = groupArry.getJSONObject(groupArry.length()-1);
                                }else{
                                    JSONObject newInstance = new JSONObject();
                                    groupArry.put(newInstance);
                                    grouped = newInstance;
                                }
                            }else {
                                grouped = grouped.getJSONObject(groupName);
                            }
                        }else{
                            JSONObject newJSON = new JSONObject();
                            grouped.put(groupName,newJSON);
                            grouped = newJSON;
                        }
                    }
                }
                boolean needPop = false;
                if(target != grouped){
                    target = grouped; // will only change target if there was a grouping
                    builder.setCurrentContext(target);
                    needPop = true;
                }

                populate(matcher,builder);

                Eat toEat = Eat.from(this.eat);
                int mStart = matcher.start();
                int mEnd = matcher.end();
                switch(toEat){
                    case Match:
                        int mStop = matcher.end();
                        line.drop(mStart,mStop);
                        matcher.region(mStart,line.length());
                        mEnd = mStart;
                        break;
                    case Width:
                        int wStop = this.eat;
                        line.drop(mStart,wStop);
                        matcher.region(mStart,line.length());
                        mEnd = mStart;
                        break;
                }

                //call each child
                for(Exp child : children){
                    child.applyWithStart(line,builder,parser,mEnd);
                }

                if(needPop) { //TODO I'm sure this breaks NestLength
                    builder.popContext();//pop target
                }

                for(MatchAction action : callbacks){
                    action.onMatch(target,this,parser);
                }

            }while( is(Rule.Repeat) && matcher.find() );

            if( is(Rule.PopContext) ) {
                builder.popContext();
            }
            if( is(Rule.ClearContext) ) {
                builder.clearContext();
            }
            if( is(Rule.PushContext) ) {
                builder.setCurrentContext(target);
            }

            if(Eat.from(this.eat) == Eat.Line){ // eat the line after applying children and repeating
                line.drop(0,line.length());
            }

        }//matched

        return rtrn;
    }
    public static String pad(int i){
        if(i<=0)
            return "";
        return "                                                                                                                                                                                                              ".substring(0,i);
    }
}
