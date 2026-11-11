package android.graphics;
public class Rect {
 public int left, top, right, bottom;
 public Rect(){}
 public Rect(Rect r){left=r.left;top=r.top;right=r.right;bottom=r.bottom;}
 public int centerX(){return (left+right)/2;} public int centerY(){return (top+bottom)/2;} public int height(){return bottom-top;} public int width(){return right-left;}
}
