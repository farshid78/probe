package android.content.res;
import android.util.DisplayMetrics;
public class Resources {
    public DisplayMetrics getDisplayMetrics(){ return new DisplayMetrics(); }
    public Configuration getConfiguration(){ return new Configuration(); }
    public int getDimensionPixelSize(int id){ return 48; }
    public String getString(int id){ return app.bastehyab.S.fallback(id); }
    public String getString(int id, Object... args){ return String.format(java.util.Locale.US, app.bastehyab.S.fallback(id), args); }
}
