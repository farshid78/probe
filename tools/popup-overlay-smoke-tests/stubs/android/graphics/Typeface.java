package android.graphics;
public class Typeface {
  public static final int NORMAL=0, BOLD=1;
  public static final Typeface DEFAULT_BOLD=new Typeface();
  public boolean isBold(){return false;}
  public static Typeface create(String family,int style){return new Typeface();}
  public static Typeface createFromAsset(android.content.res.AssetManager a,String path){return new Typeface();}
}
