package perf.parse.factory;

import perf.parse.Exp;
import perf.parse.util.file.Parser;

/**
 *
 */
public class ServerLogFactory {

    private Parser parser;

    public Exp newStartEntryExp(){
        return new Exp("timestamp","(?<timestamp>\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3})")
            .eat(Exp.Eat.Match)
            .add(new Exp("level", " (?<level>[A-Z]+)\\s+")
                    .eat(Exp.Eat.Match))
            .add(new Exp("component","\\[(?<component>[^\\]]+)\\]\\s+")
                    .eat(Exp.Eat.Match))
            .add(new Exp("threadName","\\((?<threadName>[^\\)]+)\\)")
                    .eat(Exp.Eat.Match))
            .add(new Exp("message"," (?<message>.+\n?)")
                    .eat(Exp.Eat.Match));
    }
    public Exp newFrameExp(){
        return new Exp("frame","\\s+at (?<frame>[^\\(]+)")
            .eat(Exp.Eat.Match)
            .group("stack").set(Exp.Merge.Entry)
            .add(new Exp("nativeMethod", "\\((?<nativeMethod>Native Method)\\)")
                    .eat(Exp.Eat.Line)
                    .set("nativeMethod", Exp.Value.BooleanKey))
            .add(new Exp("lineNumber","\\((?<file>[^:]+):(?<line>[^\\)]+)\\)")
                    .eat(Exp.Eat.Line));
    }
    public Exp newCausedByExp(){
        return new Exp("causedBy","Caused by: (?<exception>[^:]+): (?<message>.+\n?)")
            //.group("stack")
            .group("causedBy")
            .set(Exp.Rule.PushContext).eat(Exp.Eat.Line);
    }
    public Exp newStackRemainderExp(){
        return new Exp("more","\\s+\\.\\.\\. (?<stackRemainder>\\d+) more").eat(Exp.Eat.Line);
    }
    public Exp newMessageExp(){
        return new Exp("message","(?<message>.+\n?)").set("message", Exp.Value.String);
    }
    public Parser newLogEntryParser(){
        Parser p = new Parser();
            p.add(newStartEntryExp());
            p.add(newFrameExp());
            p.add(newCausedByExp());
            p.add(newStackRemainderExp());
            p.add(newMessageExp());
        return p;
    }
}
