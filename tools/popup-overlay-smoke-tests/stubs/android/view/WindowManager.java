package android.view;
public interface WindowManager {
  void addView(View v, ViewGroup.LayoutParams p); void removeView(View v); void updateViewLayout(View v, ViewGroup.LayoutParams p);
  class LayoutParams extends ViewGroup.LayoutParams {
    public static final int TYPE_ACCESSIBILITY_OVERLAY=2032, FLAG_NOT_FOCUSABLE=8, FLAG_NOT_TOUCH_MODAL=32, FLAG_LAYOUT_IN_SCREEN=256, FLAG_NOT_TOUCHABLE=16;
    public int x,y,gravity,flags;
    public LayoutParams(int w,int h,int type,int flags,int fmt){super(w,h);this.flags=flags;} public void setTitle(CharSequence t){} } }
