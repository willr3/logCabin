package perf.parse.factory;

import perf.parse.Exp;
import perf.parse.util.file.Parser;

/**
 *
 */
public class JStackFactory {
    public Exp newThreadDump(){
        return new Exp("start", "Full thread dump (?<vm>[^\\(]+)\\((?<version>[^\\(]+)\\)").set(Exp.Merge.NewStart);
    }
    public Exp newTidPattern(){
        return new Exp("tid", " tid=(?<tid>0x[0-9a-f]+) nid=(?<nid>0x[0-9a-f]+)").set(Exp.Merge.NewStart)
                .add(new Exp("os_prio", " os_prio=(?<osprio>\\d+)").set(Exp.Rule.LineStart))
                .add(new Exp("prio", " prio=(?<prio>\\d+)").set(Exp.Rule.LineStart))
                .add(new Exp("daemon", " (?<daemon>daemon) ").set("daemon", Exp.Value.BooleanKey).set(Exp.Rule.LineStart))
                .add(new Exp("name", "\\\"(?<name>.+)\\\"").set(Exp.Rule.LineStart))
                .add(new Exp("hex", "\\[(?<hex>0x[0-9a-f]+)\\]").eat(Exp.Eat.Match))
                .add(new Exp("status", " (?<status>[^\\[\n]+) "));
    }
    public Exp newThreadStatePattern(){
        return new Exp("ThreadState","\\s+java\\.lang\\.Thread\\.State: (?<state>.*)");
    }
    public Exp newStackFramePattern(){
        return new Exp("stack", "\\s+at (?<frame>[^\\(]+)").group("stack").set(Exp.Merge.Entry)
            .add(new Exp("nativeMethod", "\\((?<nativeMethod>Native Method)\\)").set("nativeMethod", Exp.Value.BooleanKey))
            .add(new Exp("lineNumber", "\\((?<file>[^:]+):(?<line>\\d+)\\)"));
    }
    public Exp newLockPattern(){
        return new Exp("stack","\\s+- locked <(?<id>0x[0-9a-f]+)> \\(a (?<class>[^\\)]+)\\)").extend("stack").group("lock").set(Exp.Merge.Entry);
    }
    public Exp newWaitPattern(){
        return new Exp("stack","\\s+- waiting on <(?<id>0x[0-9a-f]+)> \\(a (?<class>[^\\)]+)\\)").extend("stack").group("wait").set(Exp.Merge.Entry);
    }
    public Parser newFileStartParser(){
        Parser p = new Parser();
        p.add(newThreadDump());
        return p;
    }
    public Parser newThreadParser(){
        Parser p = new Parser();
        p.add(newTidPattern());
        p.add(newThreadStatePattern());
        p.add(newStackFramePattern());
        p.add(newLockPattern());
        p.add(newWaitPattern());
        return p;
    }
}
