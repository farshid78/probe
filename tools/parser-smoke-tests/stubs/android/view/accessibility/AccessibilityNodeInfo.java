package android.view.accessibility;
import android.graphics.Rect;
import java.util.*;
public class AccessibilityNodeInfo {
 private String text, desc, id, pkg="test.operator"; private final List<AccessibilityNodeInfo> children=new ArrayList<>();
 private int top, left=0, right=100, bottom, scrollY=0, maxScrollY=0; private boolean visible=true, editable=false, password=false, scrollable=false, failChildCount=false;
 public AccessibilityNodeInfo(String t,int y){text=t;top=y;bottom=y+20;}
 public AccessibilityNodeInfo add(AccessibilityNodeInfo n){children.add(n);return this;} public AccessibilityNodeInfo viewId(String v){id=v;return this;} public AccessibilityNodeInfo description(String v){desc=v;return this;} public AccessibilityNodeInfo packageName(String v){pkg=v;return this;}
 public AccessibilityNodeInfo visible(boolean v){visible=v;return this;}
 public AccessibilityNodeInfo editable(boolean v){editable=v;return this;}
 public AccessibilityNodeInfo password(boolean v){password=v;return this;}
 public AccessibilityNodeInfo scrollable(boolean v){scrollable=v;return this;} public AccessibilityNodeInfo scrollRange(int y,int max){scrollY=y;maxScrollY=max;return this;}
 public AccessibilityNodeInfo failChildCount(boolean v){failChildCount=v;return this;}
 public AccessibilityNodeInfo bounds(int l,int t,int r,int b){left=l;top=t;right=r;bottom=b;return this;}
 public boolean isVisibleToUser(){return visible;} public boolean isEditable(){return editable;} public boolean isPassword(){return password;}
 public void getBoundsInScreen(Rect r){r.left=left;r.right=right;r.top=top;r.bottom=bottom;}
 public CharSequence getText(){return text;} public CharSequence getContentDescription(){return desc;}
 public CharSequence getClassName(){return "android.view.View";} public String getViewIdResourceName(){return id;} public int getChildCount(){if(failChildCount)throw new IllegalStateException("stale node");return children.size();}
 public AccessibilityNodeInfo getChild(int i){return children.get(i);} public CharSequence getPackageName(){return pkg;} public void recycle(){}
 public static final int ACTION_SCROLL_FORWARD=4096;
 public boolean isScrollable(){return scrollable;} public int getScrollY(){return scrollY;} public int getMaxScrollY(){return maxScrollY;}
 public boolean performAction(int a){return false;}
}
