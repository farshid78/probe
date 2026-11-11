package android.view;
import android.content.Context;
public class ViewGroup extends View { public ViewGroup(Context c){super(c);} 
  public static class LayoutParams { public int width,height; public LayoutParams(int w,int h){width=w;height=h;} public static final int MATCH_PARENT=-1, WRAP_CONTENT=-2; }
  public static class MarginLayoutParams extends LayoutParams { public MarginLayoutParams(int w,int h){super(w,h);} public void setMargins(int a,int b,int c,int d){} public void setMarginStart(int s){} public void setMarginEnd(int e){} }
  public java.util.List<View> kids=new java.util.ArrayList<>();
  public void addView(View v){kids.add(v);} public void addView(View v, LayoutParams p){kids.add(v);} public int getChildCount(){return kids.size();} public View getChildAt(int i){return kids.get(i);} public void removeAllViews(){kids.clear();} }
