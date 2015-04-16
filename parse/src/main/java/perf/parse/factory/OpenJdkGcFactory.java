package perf.parse.factory;

import perf.parse.Exp;
import perf.parse.consumers.JsonKeyMapConsumer;
import perf.parse.consumers.WriteJsonConsumer;
import perf.parse.reader.TextLineReader;
import perf.parse.util.file.Parser;

import java.util.Date;

/**
 *
 */
public class OpenJdkGcFactory {

    public Exp newTimestampPattern(){
        return new Exp("timestamp","^(?<timestamp>\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}-\\d{4}): ").set(Exp.Merge.NewStart).eat(Exp.Eat.Match);
    }
    public Exp newElapsedPattern(){
        return new Exp("elapsed","^(?<elapsed>\\d+\\.\\d{3}): ")
            .set("elapsed", Exp.Value.Number).set(Exp.Merge.NewStart).eat(Exp.Eat.Match);
    }
    public Exp newStopTimePattern() {
        return new Exp("stopTime","Total time for which application threads were stopped: (?<threadpause>\\d+\\.\\d+) seconds").eat(Exp.Eat.Line);
    }
    public Exp newUserSysRealPattern(){
        return new Exp("usersysreal","\\[Times: user=(?<user>\\d+\\.\\d{2}) sys=(?<sys>\\d+\\.\\d{2}), real=(?<real>\\d+\\.\\d{2}) secs\\]").group("times").eat(Exp.Eat.Match);
    }
    public Exp newRegionPattern(){
        return new Exp("region","\\[(?<name>\\w+): (?<pregc>\\d+[KMG]?)->(?<postgc>\\d+[KMG]?)\\((?<size>\\d+[KMG]?)\\)\\][ ,]")
            .group("region")
            .key("name") // key name means we do not need to Merge as new Entry
            //.set(Exp.Merge.Entry)
            .set(Exp.Rule.Repeat)
            .eat(Exp.Eat.Match)
            .set("pregc", Exp.Value.KMG)
            .set("postgc", Exp.Value.KMG)
            .set("size", Exp.Value.KMG);
    }
    public Exp newSurvivorThresholdPattern(){
        return new Exp("survivorthreshold","Desired survivor size (?<survivorsize>\\d+) bytes, new threshold (?<threshold>\\d+) \\(max (?<maxThreshold>\\d+)\\)");
    }
    public Exp newGCReasonPattern(){
        return new Exp("gcreason","\\((?<gcreason>[\\w ]+)\\) ")
            .eat(Exp.Eat.Match);
    }
    public Exp newGCTimePattern(){
        return new Exp("gctime",", (?<gctime>\\d+\\.\\d+) secs\\] ");
    }
    public Exp newHeapSizePattern(){
        return new Exp("heapsize","(?<pregc>\\d+[KMG]?)->(?<postgc>\\d+[KMG]?)\\((?<size>\\d+[KMG]?)\\)")
            .group("heap")
            .eat(Exp.Eat.Match)
            .set("pregc", Exp.Value.KMG)
            .set("postgc", Exp.Value.KMG)
            .set("size", Exp.Value.KMG);
    }
    public Exp newFullGCPattern(){
        return new Exp("FullGC","^\\[(?<gctype>Full GC) ").set(Exp.Merge.NewStart).eat(Exp.Eat.Match);
    }
    public Exp newGCPattern(){
        return new Exp("GC","^\\[(?<gctype>GC) ").set(Exp.Merge.NewStart).eat(Exp.Eat.Match);
    }
    public Exp newPolicyPattern(){
        return new Exp("policy","^(?<key>\\w+)::(?<value>[^:]+):").set("key","value").group("policy").set(Exp.Merge.Entry).eat(Exp.Eat.Match)
            .add(new Exp("K,Vnumber", "  (?<key>\\w+): (?<value>\\d+\\.?\\d*)").set("key", "value").set(Exp.Rule.Repeat));
    }
    public Parser newGcParser(){
        Parser p = new Parser();
        p.add(newTimestampPattern());
        p.add(newElapsedPattern());
        p.add(newGCPattern());
        p.add(newFullGCPattern());
        p.add(newStopTimePattern());
        p.add(newUserSysRealPattern());
        p.add(newRegionPattern());
        p.add(newSurvivorThresholdPattern());
        p.add(newGCReasonPattern());
        p.add(newGCTimePattern());
        p.add(newHeapSizePattern());
        p.add(newPolicyPattern());
        return p;
    }
}
