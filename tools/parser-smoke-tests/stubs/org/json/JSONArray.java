package org.json;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/** Small deterministic JSON-array test double; not used in the Android app. */
public class JSONArray {
 private static final Map<String,List<Object>> REGISTRY = new ConcurrentHashMap<>();
 private final List<Object> values = new ArrayList<>();
 public JSONArray(){}
 public JSONArray(String s){ List<Object> v=REGISTRY.get(s); if(v==null) throw new IllegalArgumentException("Invalid JSON test fixture"); values.addAll(v); }
 public JSONArray(Object o){ if(o instanceof List) values.addAll((List<?>)o); }
 public JSONArray put(Object o){values.add(o);return this;}
 public int length(){return values.size();}
 public long getLong(int i){Object o=values.get(i);return o instanceof Number?((Number)o).longValue():Long.parseLong(String.valueOf(o));}
 public JSONObject getJSONObject(int i){Object o=values.get(i);if(o instanceof JSONObject)return (JSONObject)o;throw new IllegalArgumentException("Not an object");}
 public String getString(int i){return String.valueOf(values.get(i));}
 @Override public String toString(){String token="[test-json-"+REGISTRY.size()+"-"+System.identityHashCode(this)+"]";REGISTRY.put(token,new ArrayList<>(values));return token;}
}
