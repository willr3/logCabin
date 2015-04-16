# logCabin
Built with logs. Main goal is to convert the data we get from performance benchmarks to JSON so we can graph them.

## Parse
Parses through various log files and convert them to JSON for graphing

```java
WriteJsonConsumer writer = new WriteJsonConsumer("~/out.json");
OpenJdkGcFactory f = new OpenJdkGcFactory();
TextLineReader reader = new TextLineReader();
Parser gcParser = f.newGcParser();
//Add logic for custom tenure logging in our jdk build
gcParser.add(new Exp("tenured", "PERF tenuring (?<class>\\S+) ")
  .group("tenured")
  .set("class", Exp.Value.Count)
  .eat(Exp.Eat.Line));
gcParser.add(writer);
reader.addParser(gcParser);
reader.read("~/server.gclog");
```

### TODO
- vmstat
- netstat
- iostat
- *stat
