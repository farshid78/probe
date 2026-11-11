package android.content;
import java.io.File; import java.util.HashMap; import java.util.Map; import android.content.res.Resources;
public class Context {
    private static final Map<String, SharedPreferences> PREFS = new HashMap<>();
    public Context getApplicationContext(){return this;}
    public File getFilesDir(){return new File(".");}
    public Resources getResources(){return new Resources();}
    public android.content.res.AssetManager getAssets(){return new android.content.res.AssetManager();}
    public synchronized SharedPreferences getSharedPreferences(String name,int mode){
        SharedPreferences p=PREFS.get(name); if(p==null){p=new MemoryPreferences();PREFS.put(name,p);} return p;
    }
    public String getPackageName(){return "app.bastehyab";}
    public void startActivity(Intent intent){}
    private static final class MemoryPreferences implements SharedPreferences {
        final Map<String,Object> values=new HashMap<>();
        public String getString(String k,String d){Object v=values.get(k);return v instanceof String?(String)v:d;}
        public int getInt(String k,int d){Object v=values.get(k);return v instanceof Integer?(Integer)v:d;}
        public float getFloat(String k,float d){Object v=values.get(k);return v instanceof Float?(Float)v:d;}
        public boolean getBoolean(String k,boolean d){Object v=values.get(k);return v instanceof Boolean?(Boolean)v:d;}
        public SharedPreferences.Editor edit(){return new MemoryEditor(this);}
    }
    private static final class MemoryEditor implements SharedPreferences.Editor {
        final MemoryPreferences p; final Map<String,Object> changes=new HashMap<>();
        MemoryEditor(MemoryPreferences p){this.p=p;}
        public SharedPreferences.Editor putString(String k,String v){changes.put(k,v);return this;}
        public SharedPreferences.Editor putInt(String k,int v){changes.put(k,v);return this;}
        public SharedPreferences.Editor putFloat(String k,float v){changes.put(k,v);return this;}
        public SharedPreferences.Editor putBoolean(String k,boolean v){changes.put(k,v);return this;}
        public SharedPreferences.Editor remove(String k){changes.put(k,null);return this;}
        public void apply(){for(Map.Entry<String,Object> e:changes.entrySet()){if(e.getValue()==null)p.values.remove(e.getKey());else p.values.put(e.getKey(),e.getValue());}}
    }
}
