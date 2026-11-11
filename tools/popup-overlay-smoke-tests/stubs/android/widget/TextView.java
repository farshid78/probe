package android.widget;
import android.content.Context;
public class TextView extends android.view.View {
  public CharSequence text="";
  public TextView(Context c){super(c);}
  public CharSequence getText(){return text;}
  public void setText(CharSequence s){text=s;}
  public void setTextSize(float s){} public void setTextColor(int c){}
  public void setTypeface(android.graphics.Typeface t){} public android.graphics.Typeface getTypeface(){return new android.graphics.Typeface();}
  public void setGravity(int g){} public void setMinWidth(int w){} public void setMinHeight(int h){}
  public void setSingleLine(boolean b){} public void setEllipsize(android.text.TextUtils.TruncateAt t){}
  public void setMaxLines(int n){} public void setIncludeFontPadding(boolean b){}
  public void setLineSpacing(float add,float mult){} public void setLetterSpacing(float v){}
}
