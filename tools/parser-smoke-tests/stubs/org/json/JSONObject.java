package org.json;
import java.util.HashMap;
import java.util.Map;
public class JSONObject {
 private final Map<String,Object> values=new HashMap<>();
 public JSONObject(){}
 public JSONObject put(String k,Object v){values.put(k,v);return this;}
 public boolean has(String k){return values.containsKey(k);}
 public String getString(String k){Object v=values.get(k);if(v==null)throw new IllegalArgumentException(k);return String.valueOf(v);}
 public double getDouble(String k){Object v=values.get(k);if(v==null)throw new IllegalArgumentException(k);return v instanceof Number?((Number)v).doubleValue():Double.parseDouble(String.valueOf(v));}
 public long getLong(String k){Object v=values.get(k);if(v==null)throw new IllegalArgumentException(k);return v instanceof Number?((Number)v).longValue():Long.parseLong(String.valueOf(v));}
 public String optString(String k,String d){Object v=values.get(k);return v==null?d:String.valueOf(v);}
 public double optDouble(String k,double d){Object v=values.get(k);try{return v==null?d:v instanceof Number?((Number)v).doubleValue():Double.parseDouble(String.valueOf(v));}catch(RuntimeException e){return d;}}
 public long optLong(String k,long d){Object v=values.get(k);try{return v==null?d:v instanceof Number?((Number)v).longValue():Long.parseLong(String.valueOf(v));}catch(RuntimeException e){return d;}}
 public boolean optBoolean(String k,boolean d){Object v=values.get(k);return v==null?d:v instanceof Boolean?(Boolean)v:Boolean.parseBoolean(String.valueOf(v));}
 public int optInt(String k,int d){Object v=values.get(k);try{return v==null?d:v instanceof Number?((Number)v).intValue():Integer.parseInt(String.valueOf(v));}catch(RuntimeException e){return d;}}
 public JSONArray optJSONArray(String k){Object v=values.get(k);return v instanceof JSONArray?(JSONArray)v:null;}
}
