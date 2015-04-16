package perf.parse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 *
 */
public class PatternTest {

    @Test public void groupByOrder(){
        Pattern p = new Pattern("foo","(?<key>\\w+)=(?<value>\\w+)").groupBy("first").groupBy("second");

        JSONObject obj = p.getMatch(new StringBuffer("foo=bar"));

        assertEquals("second",obj.keys().next());
    }
    @Test public void groupByKey(){
        Pattern p = new Pattern("foo","(?<key>\\w+)=(?<value>\\w+)").groupByValue("key");
        JSONObject obj = p.getMatch(new StringBuffer("foo=bar"));
        assertEquals("foo", obj.keys().next());
    }
    @Test public void testBooleanKey(){
        Pattern p = new Pattern("foo","key=(?<key>\\w+)").set("key", Pattern.Type.BooleanKey);
        JSONObject obj = p.getMatch(new StringBuffer("key=foo"));
        assertEquals(true,obj.getBoolean("key"));
    }
    @Test public void testBooleanValue(){
        Pattern p = new Pattern("foo","key=(?<key>\\w+)").set("key", Pattern.Type.BooleanValue);
        JSONObject obj = p.getMatch(new StringBuffer("key=foo"));
        assertEquals(true,obj.getBoolean("foo"));
    }
    @Test public void testCount(){
        Pattern p = new Pattern("foo","(?<key>\\w+)=(?<value>\\w+)").set("key", Pattern.Type.Count);

        JSONObject obj = p.getMatch(new StringBuffer("foo=bar"));

        assertEquals(1,obj.getInt("foo"));
    }

    @Test public void testSum(){
        Pattern p = new Pattern("foo","value=(?<key>\\d+)").set("key", Pattern.Type.Sum);
        JSONObject obj = p.getMatch(new StringBuffer("value=5"));
        assertEquals(5,obj.getInt("key"));
    }

    @Test public void testRepeat(){
        Pattern p = new Pattern("matches","(?<key>\\w+)=(?<value>\\w+)").set(Pattern.Match.Repeat).set(Pattern.Merge.KeyValue);
        JSONObject obj = p.getMatch(new StringBuffer("foo=bar fizz=fuzz bool"));

        assertEquals("matches",obj.keys().next());
        assertEquals(2,obj.getJSONArray("matches").length());
    }

    @Test public void mergePrevious(){
        JSONObject b = new JSONObject();
        b.accumulate("base",new JSONObject("{'foo':'bar'}"));
        b.accumulate("base", new JSONObject("{'foo':'bar'}"));
        Pattern p = new Pattern("base","(?<key>\\w+)=(?<value>\\w+)").groupBy("base").set(Pattern.Merge.Previous);

        JSONObject obj = p.getMatch(new StringBuffer("foo=bar"));

        JsonBuilder bld = new JsonBuilder(b);

        p.mergeTo(obj,bld);

        assertEquals(true,bld.getRoot().get("base") instanceof JSONArray );
        assertEquals(2,bld.getRoot().getJSONArray("base").length());
        assertEquals(3,bld.getRoot().getJSONArray("base").getJSONObject(1).keySet().size());
        assertTrue(bld.getRoot().getJSONArray("base").getJSONObject(1).has("key"));
        assertTrue(bld.getRoot().getJSONArray("base").getJSONObject(1).has(""));

    }
}
