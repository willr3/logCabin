package perf.parse.util.file;

import org.json.JSONObject;
import perf.parse.*;

import java.util.LinkedList;
import java.util.List;

/**
 *
 */
public class Parser {


    private List<JsonConsumer> consumers;
    private LinkedList<Exp> patterns;
    private JsonBuilder builder;

    public Parser(){
        consumers = new LinkedList<JsonConsumer>();
        patterns = new LinkedList<Exp>();
        builder = new JsonBuilder();
    }

    public JsonBuilder getBuilder(){return builder;}

    public void addAhead(Exp pattern){
        patterns.add(0,pattern);
    }
    public void add(Exp pattern){
        patterns.add(pattern);
    }
    public void add(JsonConsumer consumer){
        consumers.add(consumer);
    }

    public void onLine(CheatChars line){
        boolean matched = false;
        for(Exp pattern : patterns){
            matched = pattern.apply(line,builder,this) && matched;
            if(line.isEmpty()){
                break;
            }
        }
        emit();
    }

    public void setup(){
        for (JsonConsumer consumer : consumers) {
            consumer.start();
        }
    }

    public void close(){
        builder.close();
        emit();
        for (JsonConsumer consumer : consumers) {
            consumer.close();
        }

    }
    private void emit(){
        JSONObject toEmit = builder.takeClosedRoot();
        if(toEmit != null) {
            for (JsonConsumer consumer : consumers) {
                consumer.consume(toEmit);
            }
        }
    }
}
