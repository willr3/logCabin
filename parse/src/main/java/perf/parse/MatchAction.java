package perf.parse;

import org.json.JSONObject;
import perf.parse.util.file.Parser;

/**
 *
 */
public interface MatchAction {

    public void onMatch(JSONObject match,Exp pattern, Parser parser);
}
