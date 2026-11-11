package android.view;
import android.content.Context;
public class View {
  public static final int LAYOUT_DIRECTION_RTL=1, TEXT_DIRECTION_RTL=2, TEXT_DIRECTION_LTR=3, TEXT_DIRECTION_ANY_RTL=4;
  public static final int TEXT_ALIGNMENT_GRAVITY=1, TEXT_ALIGNMENT_CENTER=4;
  public interface OnTouchListener { boolean onTouch(View v, MotionEvent e); }
  public interface OnClickListener { void onClick(View v); }
  public interface OnAttachStateChangeListener { void onViewAttachedToWindow(View v); void onViewDetachedFromWindow(View v); }
  public OnTouchListener touchL; public OnClickListener clickL;
  public CharSequence contentDescription="";
  public View(Context c){}
  public boolean isAttachedToWindow(){return false;}
  public void setElevation(float e){} public void setLayoutDirection(int d){} public void setPadding(int a,int b,int c,int d){}
  public void setBackground(android.graphics.drawable.Drawable d){}
  public void setOnTouchListener(OnTouchListener l){touchL=l;} public void setOnClickListener(OnClickListener l){clickL=l;}
  public void addOnAttachStateChangeListener(OnAttachStateChangeListener l){}
  public void setClickable(boolean b){} public void setLongClickable(boolean b){} public void setFocusable(boolean b){}
  public void setImportantForAccessibility(int mode){} public void setLabelFor(int id){} public void setTooltipText(CharSequence t){}
  public void setContentDescription(CharSequence t){contentDescription=t;}
  public int getWidth(){return 0;} public int getHeight(){return 0;}
  public void setTextDirection(int d){} public void setTextAlignment(int d){}
  public void setStateListAnimator(Object o){} public void setMinimumHeight(int h){} public void setMinimumWidth(int w){}
  public void setClipToOutline(boolean b){} public void invalidate(){}
  public static final int VISIBLE=0;
}
