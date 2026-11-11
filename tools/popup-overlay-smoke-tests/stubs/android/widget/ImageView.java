package android.widget;
import android.content.Context;
public class ImageView extends android.view.View {
  public enum ScaleType { FIT_CENTER, CENTER_CROP }
  public ImageView(Context c){super(c);}
  public void setImageResource(int id){} public void setColorFilter(int color){}
  public void setScaleType(ScaleType type){}
}
