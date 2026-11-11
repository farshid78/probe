package android.graphics;
public class RectF {
    public float left, top, right, bottom;
    public RectF(float a,float b,float c,float d){left=a;top=b;right=c;bottom=d;}
    public RectF(RectF r){this(r.left,r.top,r.right,r.bottom);}
    public void offset(float x,float y){left+=x;top+=y;right+=x;bottom+=y;}
    public void inset(float x,float y){left+=x;top+=y;right-=x;bottom-=y;}
}
