package app.bastehyab;
import android.view.accessibility.AccessibilityNodeInfo;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

// Replays captured accessibility-tree dumps through the real Extractor and prints offer summaries.
public class DumpHarness {
  static final Pattern LINE = Pattern.compile("^(\\s*)#(\\d+) (\\S+)(.*)$");
  static final Pattern ATTR = Pattern.compile("(\\w+)=\"((?:[^\"\\\\]|\\\\.)*)\"");
  static final Pattern BOUNDS = Pattern.compile("b=(-?\\d+),(-?\\d+),(-?\\d+),(-?\\d+)");

  static AccessibilityNodeInfo parse(List<String> lines) {
    // find the frame's root window subtree(s): treat each "##### WINDOW" block as a root sibling
    AccessibilityNodeInfo top = new AccessibilityNodeInfo(null,null,null,0,0,0,0,true);
    Deque<Object[]> stack = new ArrayDeque<>();
    int lastIndent = -1;
    for (String ln : lines) {
      if (ln.startsWith("#####")) { stack.clear(); lastIndent = -1; continue; }
      Matcher m = LINE.matcher(ln);
      if (!m.matches()) continue;
      int indent = m.group(1).length();
      String rest = m.group(4);
      String id=null,text=null,desc=null; int l=0,t=0,r=0,b=0; boolean vis=true;
      Matcher a = ATTR.matcher(rest);
      while (a.find()) {
        switch (a.group(1)) {
          case "id": id = a.group(2); break;
          case "text": text = a.group(2); break;
          case "desc": desc = a.group(2); break;
        }
      }
      Matcher bm = BOUNDS.matcher(rest); if (bm.find()) { l=Integer.parseInt(bm.group(1)); t=Integer.parseInt(bm.group(2)); r=Integer.parseInt(bm.group(3)); b=Integer.parseInt(bm.group(4)); }
      vis = !rest.contains("vis=0");
      AccessibilityNodeInfo node = new AccessibilityNodeInfo(id,text,desc,l,t,r,b,vis);
      while (!stack.isEmpty() && (int) stack.peek()[0] >= indent) stack.pop();
      if (stack.isEmpty()) top.kids.add(node); else ((AccessibilityNodeInfo) stack.peek()[1]).kids.add(node);
      stack.push(new Object[]{indent, node});
    }
    return top;
  }

  public static void main(String[] args) throws Exception {
    String dir = args[0], pkg = args[1], cur = args[2];
    Op op = new Op("x", "test", 0, cur, true, pkg);
    List<String> files = new ArrayList<>();
    try (var s = Files.list(Paths.get(dir))) { s.filter(p -> p.toString().endsWith("_tree.txt")).sorted().forEach(p -> files.add(p.toString())); }
    Map<String,Integer> seen = new TreeMap<>();
    for (String f : files) {
      List<String> lines = Files.readAllLines(Paths.get(f));
      AccessibilityNodeInfo root = parse(lines);
      // treat the first window subtree that belongs to the operator package as the app root
      AccessibilityNodeInfo app = root;
      Extractor.Result res = Extractor.extract(app, op, 2320);
      System.out.println(Paths.get(f).getFileName() + " items=" + res.items.size() + " dup=" + res.duplicates
        + " incomplete=" + res.incomplete + " fullTraversal=" + res.fullTraversal + " prunedHidden=" + res.prunedHidden + " nodes=" + res.nodes);
      for (Pkg p : res.items) seen.merge(p.gb + "GB/" + p.days + "d/" + (p.currencyKnown ? p.toman + "T" : "unitUnknown") + (p.ambig ? "/AMBIG" : ""), 1, Integer::sum);
    }
    System.out.println("--- distinct offers (volume/days/price) across frames:");
    seen.forEach((k,v) -> System.out.println("  " + k + "  x" + v));
  }
}
