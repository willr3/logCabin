package perf.parse;

import org.json.JSONObject;

/**
 *
 */
public interface JsonConsumer {

    public void start();
    public void consume(JSONObject object);
    public void close();
}
