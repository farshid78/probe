package app.bastehyab;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

final class Op {
    String id, name, cur; // cur: IRR (ریال), IRT (تومان), AUTO
    final List<String> pkgs = new ArrayList<>();
    int color;
    boolean enabled = true, builtin, cmp = true;
    int policy; // 0 auto, 1 largest amount in card, 2 smallest

    Op() {}

    Op(String id, String name, int color, String cur, boolean builtin, String... p) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.cur = cur;
        this.builtin = builtin;
        for (String s : p) pkgs.add(s);
    }

    JSONObject json() throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("name", name);
        o.put("cur", cur);
        o.put("color", color);
        o.put("en", enabled);
        o.put("bi", builtin);
        o.put("cmp", cmp);
        o.put("pp", policy);
        o.put("pk", new JSONArray(pkgs));
        return o;
    }

    static Op from(JSONObject o) throws Exception {
        Op x = new Op();
        x.id = o.getString("id");
        x.name = o.getString("name");
        x.cur = o.optString("cur", "AUTO");
        x.color = o.optInt("color", 0xFF8A97B1);
        x.enabled = o.optBoolean("en", true);
        x.builtin = o.optBoolean("bi", false);
        x.cmp = o.optBoolean("cmp", true);
        x.policy = o.optInt("pp", 0);
        JSONArray a = o.optJSONArray("pk");
        if (a != null) for (int i = 0; i < a.length(); i++) x.pkgs.add(a.getString(i));
        return x;
    }
}
