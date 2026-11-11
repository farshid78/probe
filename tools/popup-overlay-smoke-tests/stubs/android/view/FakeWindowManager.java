package android.view;
import java.util.*;
/** Behaves like WindowManagerGlobal: strict about double-add / unknown views. */
public class FakeWindowManager implements WindowManager {
  public final List<View> windows = new ArrayList<>(); public final Map<View,ViewGroup.LayoutParams> params = new IdentityHashMap<>();
  public int adds, removes, updates;
  public void addView(View v, ViewGroup.LayoutParams p){ if (windows.contains(v)) throw new IllegalStateException("already added"); windows.add(v); params.put(v,p); adds++; }
  public void removeView(View v){ if (!windows.remove(v)) throw new IllegalArgumentException("not attached"); params.remove(v); removes++; }
  public void updateViewLayout(View v, ViewGroup.LayoutParams p){ if (!windows.contains(v)) throw new IllegalArgumentException("not attached"); updates++; }
}
