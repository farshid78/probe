package android.view.accessibility;
import android.graphics.Rect;
import java.util.*;
// Test double built from captured accessibility-tree dumps (text/desc/id/bounds/visibility only).
public class AccessibilityNodeInfo {
 final String text, desc, id; final int l,t,r,b; final boolean vis;
 public final List<AccessibilityNodeInfo> kids=new ArrayList<>();
 public AccessibilityNodeInfo(String id,String text,String desc,int l,int t,int r,int b,boolean vis){this.id=id;this.text=text;this.desc=desc;this.l=l;this.t=t;this.r=r;this.b=b;this.vis=vis;}
 public boolean isVisibleToUser(){return vis;} public boolean isEditable(){return false;} public boolean isPassword(){return false;}
 public void getBoundsInScreen(Rect x){x.left=l;x.top=t;x.right=r;x.bottom=b;}
 public CharSequence getText(){return text;} public CharSequence getContentDescription(){return desc;}
 public String getViewIdResourceName(){return id;} public int getChildCount(){return kids.size();}
 public AccessibilityNodeInfo getChild(int i){return kids.get(i);} public CharSequence getPackageName(){return "x";}
 public void recycle(){}
}
