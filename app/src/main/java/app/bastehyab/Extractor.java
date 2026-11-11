package app.bastehyab;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Turns an accessibility tree into package offers (price / volume / duration), operator-agnostic. */
final class Extractor {
    static final class N {
        N parent;
        final List<N> kids = new ArrayList<>();
        String text;
        final Rect b = new Rect();
        List<N> lv = new ArrayList<>(); // text leaves in subtree
        boolean leaf, price, unitOnly, isVol;
        double amt, mul;
        int unit, pc, vc; // unit: 0 none, 1 rial, 2 toman; pc/vc: price / volume leaves below

        int cx() { return b.centerX(); }
        int cy() { return b.centerY(); }
    }

    static final class Result {
        final List<Pkg> items = new ArrayList<>();
        final Set<String> keys = new HashSet<>();
        String dump = "";
        int leaves;
        int nodes;
        int maxDepth;
        int duplicates;
        int prunedHidden;
        boolean incomplete;
        /** True only when the accessibility hierarchy was traversed without depth/node truncation. */
        boolean treeComplete;
        boolean fullTraversal;
        boolean hasLoadMoreIndicator;
        String incompleteReason = "";
    }

    private static final class Cand {
        N leaf;
        long toman;
        int kind; // 0 normal, 1 unit price (monthly / per GB ...), 2 old price, 3 discount / credit amount, 4 labelled "price"
        boolean currencyKnown;
    }

    private static final class Pick {
        long toman;
        long[] alts = new long[0];
        String note = "";
        boolean ambig;
        boolean currencyKnown;
    }

    private static final class BuildState {
        int nodes;
        int visitedNodes;
        int maxDepth;
        int prunedHidden; // hidden subtrees skipped in fast mode
        boolean truncated;
        boolean hasLoadMoreIndicator;
        String reason = "";

        void truncate(String why) {
            truncated = true;
            if (reason.isEmpty()) reason = why;
        }
    }

    private static final int MAX_TREE_DEPTH = 48;
    private static final int MAX_TREE_NODES = 4500;
    private static final int F = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    private static final Pattern VOL = Pattern.compile(
            "(\\d+(?:\\.\\d+)?)\\s*(گیگابایت|گیگا|گیگ|مگابایت|مگا|مگ|ترابایت|ترا|gb|mb|tb)", F);
    private static final Pattern DUR = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(روز|ماه|هفته|ساعت|سال)");
    private static final Pattern DURN = Pattern.compile(
            "(یک|دو|سه|چهار|پنج|شش|هفت|هشت|نه|ده|دوازده|پانزده|بیست|سی|شصت|نود)\\s*(روزه|ماهه|هفته|ساله)");
    private static final Pattern DURW = Pattern.compile("(روزانه|هفتگی|ماهانه|سالانه)");
    private static final Pattern UNIT = Pattern.compile("تومان|تومن|ریال");
    private static final Pattern UNITONLY = Pattern.compile("^\\s*(میلیون|هزار)?\\s*(تومان|تومن|ریال)\\s*$");
    private static final Pattern NUMONLY = Pattern.compile("^\\d{1,4}(?:\\.\\d+)?$");
    private static final Pattern PRICE = Pattern.compile(
            "(\\d{1,3}(?:[,.]\\d{3})+|\\d+(?:\\.\\d+)?)(?:\\s*(میلیون|هزار))?\\s*(تومان|تومن|ریال)?");
    // price written inside a title: "۹۸۹۰۰۰ت", "333000 تومان", "۳۳۳ هزار تومان"
    private static final Pattern TPRICE = Pattern.compile(
            "(?<![\\d.,])(\\d{1,3}(?:,\\d{3})+|\\d+(?:\\.\\d+)?)\\s*(هزار)?\\s*(?:تومان|تومن|ت)(?![\\u0600-\\u06FF])");
    private static final Pattern DURCHIP = Pattern.compile("^\\s*(روزانه|هفتگی|ماهانه|ماهیانه|سالانه)\\s*$");
    private static final Pattern SEP = Pattern.compile("\\d{1,3}(?:[,.]\\d{3})+");
    private static final Pattern R_TIME = Pattern.compile("نیمه\\s?شب|بامداد|صبح|ظهر|عصر|ساعات?\\s*خاص|بسته[ی ]+ساعتی|(?:از\\s*)?ساعت\\s*\\d\\s*(?:تا|الی)\\s*\\d|\\d\\s*(?:تا|الی)\\s*\\d\\s*(?:صبح|بامداد|ظهر|عصر|شب)");
    private static final Pattern R_NIGHT = Pattern.compile("شبانه|نیمه\\s?شب|بامداد|\\d\\s*(?:تا|الی)\\s*\\d\\s*(?:صبح|بامداد)");
    private static final Pattern R_NAZR = Pattern.compile("نذر\\s*آب");
    private static final Pattern R_DAYS = Pattern.compile("(?<![\\p{L}])(?:یک|دو|سه|چهار|پنج|\\d)?\\s?شنبه|جمعه|آخر\\s?هفته|تعطیل");
    private static final Pattern R_USER = Pattern.compile("مشترکین جدید|مشترک جدید|ثبت ?نام|ویژه مشترک|اولین خرید|دعوت");
    private static final Pattern R_NAT = Pattern.compile("آی ?نو|اینترنت ملی|ترافیک ملی|ترافیک ایرانی|سایت[\\s\\u200c]?های? ایرانی");
    private static final Pattern R_BUNDLE = Pattern.compile("پیامک|مکالمه|تماس|sms|دقیقه\\s*(?:مکالمه)?|بسته[ی ]+ترکیبی");
    private static final Pattern R_SUB = Pattern.compile("اشتراک|سرویس دیجیتال|هدیه|کد دیجیتال|فیلیمو|نماوا|فیلم[\\s\\u200c]?نت|فرادرس|طاقچه|لینگانو|تاملند");
    private static final Pattern R_RENEW = Pattern.compile("تمدید خودکار|رزرو");
    private static final Pattern R_UNL = Pattern.compile("نامحدود|مصرف منصفانه|حد[\\s\\u200c]?مصرف");
    private static final Pattern R_APP = Pattern.compile("اپلیکیشن|اینستاگرام|تلگرام|واتس|یوتیوب|آپارات|بازی|روبیکا|(?<![\\p{L}])ایتا(?![\\p{L}])|(?<![\\p{L}])بله(?![\\p{L}])|سروش|تیک ?تاک|فوتبال|ورزش|شبکه های اجتماعی|پیام ?رسان");
    private static final Pattern R_SPECIAL = Pattern.compile("ویژه(?:\\s?ی)?\\s+(?!شما(?![\\p{L}]))\\p{L}{3,}");
    private static final Pattern R_NET = Pattern.compile("ترافیک داخلی|اینترنت داخلی|مخصوص سایت|مخصوص وب.?سایت|شبکه ملی");
    // international and roaming bundles: a separate, non-suspicious class (never a domestic substitute)
    static final Pattern INTL = Pattern.compile("بین[\\s\\u200c]?الملل|رومینگ|خارج\\s?از\\s?کشور");
    private static final Pattern R_CATEGORY = Pattern.compile("شبانه|نذر\\s*آب|بین\\s*الملل|رومینگ|هدیه|ویژه|تعرفه|عمومی|عادی");
    // labels around a price leaf
    // «قیمت پایه» مبلغ اصلی/قابل پرداخت این بسته است، نه قیمت قبلی یا تخفیف‌خورده.
    // حذف آن از K_OLD ضروری است؛ در سامانتل باعث می‌شد مبلغ پایه کنار گذاشته شود.
    private static final Pattern K_OLD = Pattern.compile("قبل از|پیش از|قیمت اصلی|بدون تخفیف|قیمت قبلی|قبلی");
    private static final Pattern K_UNIT = Pattern.compile(
            "ماهانه|ماهیانه|هر ?ماه|در ?ماه|ماهی|/ ?ماه|هر ?گیگ|به ?ازای|روزانه|هر ?روز|در ?روز|هفتگی|هر ?هفته|قسط|اقساط");
    // Explicit base-price labels outrank VAT-inclusive/checkout amounts. The app applies
    // VAT itself, so treating both as competing package prices would incorrectly mark a clear
    // base price as ambiguous.
    private static final Pattern K_BASE = Pattern.compile("قیمت\\s*پایه|مبلغ\\s*پایه");
    private static final Pattern K_MAIN = Pattern.compile(
            "قیمت|مبلغ|با تخفیف|پس از تخفیف|نهایی|قابل ?پرداخت|با مالیات|شامل مالیات");
    private static final Pattern K_DISC = Pattern.compile(
            "تخفیف|هدیه|اعتبار هدیه|پاداش|جایزه|کش ?بک|بازگشت|مالیات|ارزش ?افزوده|صرفه ?جویی");

    /**
     * Fast pass skips hidden subtrees (Accessibility reports them as not visible; they carry no
     * readable text in captured screens). Only if that yields no packages do we re-walk the full
     * tree, so wrappers that hide visible children are still handled, at the cost of speed.
     */
    static Result extract(AccessibilityNodeInfo root, Op op, int screenH) {
        return extract(root, op, screenH, false);
    }

    /**
     * Full user-confirmed scans traverse hidden empty branches too, so the end detector receives
     * trustworthy hierarchy-completeness evidence. Ordinary one-page reads keep the faster pass.
     */
    static Result extract(AccessibilityNodeInfo root, Op op, int screenH, boolean requireCompleteTree) {
        if (requireCompleteTree) {
            Result full = extractOnce(root, op, screenH, false);
            full.fullTraversal = true;
            return full;
        }
        Result fast = extractOnce(root, op, screenH, true);
        if (fast.leaves == 0 && fast.prunedHidden > 0) {
            Result full = extractOnce(root, op, screenH, false);
            full.fullTraversal = true;
            return full;
        }
        return fast;
    }

    private static Result extractOnce(AccessibilityNodeInfo root, Op op, int screenH, boolean pruneHidden) {
        Result res = new Result();
        BuildState state = new BuildState();
        N tree = build(root, null, 0, state, pruneHidden);
        res.prunedHidden = state.prunedHidden;
        res.nodes = state.nodes;
        res.maxDepth = state.maxDepth;
        res.incomplete = state.truncated;
        res.treeComplete = tree != null && !state.truncated && state.prunedHidden == 0;
        res.incompleteReason = state.reason;
        res.hasLoadMoreIndicator = state.hasLoadMoreIndicator;
        if (tree == null) {
            res.treeComplete = false;
            res.incomplete = true;
            if (res.incompleteReason.isEmpty()) res.incompleteReason = "درخت صفحه قابل خواندن نبود";
            res.dump = "pkg=" + (root == null ? "unknown" : String.valueOf(root.getPackageName()))
                    + " nodes=0 complete=false reason=" + res.incompleteReason;
            return res;
        }
        mark(tree);
        List<N> leaves = tree.lv;
        res.leaves = leaves.size();

        for (N l : leaves) {
            double[] p = price(l.text);
            if (p != null) {
                l.price = true;
                l.amt = p[0];
                l.unit = (int) p[1];
            } else {
                Matcher um = UNITONLY.matcher(Fmt.norm(l.text));
                if (um.matches()) {
                    l.unitOnly = true;
                    String mu = um.group(1);
                    l.mul = mu == null ? 0 : mu.equals("میلیون") ? 1e6 : 1e3;
                    l.unit = um.group(2).equals("ریال") ? 1 : 2;
                }
            }
        }
        // "120" + "هزار تومان" written as two separate leaves
        for (int i = 0; i < leaves.size(); i++) {
            N l = leaves.get(i);
            if (!l.unitOnly || l.mul == 0) continue;
            for (int d = -1; d <= 1; d += 2) {
                int j = i + d;
                if (j < 0 || j >= leaves.size()) continue;
                N o = leaves.get(j);
                if (o.price || o.unitOnly) continue;
                String n = Fmt.norm(o.text).trim();
                if (NUMONLY.matcher(n).matches()) {
                    o.price = true;
                    o.amt = Double.parseDouble(n) * l.mul;
                    o.unit = l.unit;
                    break;
                }
            }
        }
        for (N l : leaves) l.isVol = !l.price && !l.unitOnly && vol(l.text) != null;
        count(tree);

        List<N> cards = new ArrayList<>();
        findCards(tree, cards, 0);

        Map<String, Pkg> out = new LinkedHashMap<>();
        for (N card : cards) {
            Pkg p = make(card, op);
            if (p != null) {
                if (out.containsKey(p.key)) res.duplicates++;
                else out.put(p.key, p);
            }
        }
        // Always run the geometry pass. Some apps expose an entire RecyclerView/Compose list
        // as one accessibility branch; card-only parsing then assigns the first title to unrelated
        // prices further down the list. make() rejects these aggregate branches, and this pass
        // reconstructs individual offers using title/price position and nearby labels.
        flatPairs(tree, op, out);

        // fallback: container nodes whose own text already holds the whole offer (price leaf scrolled off-screen)
        List<N> all = new ArrayList<>();
        flatten(tree, all);
        for (N x : all) {
            if (x.text == null || x.leaf || x.pc > 0) continue;
            Pkg p = fromText(x, op);
            if (p != null) {
                if (out.containsKey(p.key)) res.duplicates++;
                else out.put(p.key, p);
            }
        }

        res.items.addAll(out.values());
        // A readable tree that yields no packages is a failed extraction, never a successful empty one.
        if (out.isEmpty() && !res.incomplete) {
            res.incomplete = true;
            res.incompleteReason = "هیچ بسته‌ای در درخت صفحه پیدا نشد";
        }
        if (res.incomplete) {
            for (Pkg p : res.items) {
                p.ambig = true;
                String reason = "نتیجه از درخت ناقص صفحه استخراج شده و برای رتبه‌بندی قطعی مناسب نیست";
                p.note = (p.note == null || p.note.isEmpty()) ? reason : p.note + "، " + reason;
            }
        }
        for (Pkg p : res.items) res.keys.add(p.key);
        StringBuilder diagnostic = new StringBuilder();
        diagnostic.append("pkg=").append(root.getPackageName())
                .append(" nodes=").append(res.nodes).append(" depth=").append(res.maxDepth)
                .append(" leaves=").append(res.leaves).append(" cards=").append(cards.size())
                .append(" items=").append(res.items.size()).append(" duplicates=").append(res.duplicates)
                .append(" complete=").append(!res.incomplete);
        if (res.incomplete) diagnostic.append(" reason=").append(res.incompleteReason);
        for (Pkg item : res.items) {
            diagnostic.append("\nitem volumeGB=").append(item.gb)
                    .append(" days=").append(item.days)
                    .append(" currencyKnown=").append(item.currencyKnown)
                    .append(" priceToman=").append(item.currencyKnown ? item.toman : "unknown")
                    .append(" restricted=").append(item.restricted)
                    .append(" ambiguous=").append(item.ambig)
                    .append(" note=").append(item.note == null || item.note.isEmpty() ? "none" : item.note);
        }
        res.dump = diagnostic.toString();
        return res;
    }

    // ---------------------------------------------------------------- tree

    private static N build(AccessibilityNodeInfo n, N parent, int d, BuildState state, boolean pruneHidden) {
        if (n == null) return null;
        // Count every visited node, including hidden/private leaves, so a very large inaccessible
        // subtree cannot bypass the traversal budget and stall the accessibility worker.
        if (state.visitedNodes >= MAX_TREE_NODES) {
            state.truncate("حد تعداد گره " + MAX_TREE_NODES + " رسید");
            return null;
        }
        state.visitedNodes++;
        // Never read editable fields or password nodes (login, OTP and other user-entered values).
        boolean privateNode;
        try { privateNode = n.isEditable() || n.isPassword(); }
        catch (RuntimeException staleNode) {
            state.truncate("یک گره دسترس‌پذیری هنگام بررسی حریم خصوصی ناپدید شد");
            return null;
        }
        if (privateNode) return null;
        if (d > MAX_TREE_DEPTH) {
            state.truncate("حد عمق درخت " + MAX_TREE_DEPTH + " رسید");
            return null;
        }

        boolean visible = false;
        try { visible = n.isVisibleToUser(); }
        catch (RuntimeException staleNode) {
            state.truncate("وضعیت نمایش یک گره قابل خواندن نبود");
        }
        int childCount = 0;
        try { childCount = Math.max(0, n.getChildCount()); }
        catch (RuntimeException staleNode) {
            state.truncate("تعداد فرزندان یک گره قابل خواندن نبود");
        }
        if (pruneHidden && !visible && childCount == 0) {
            // Hidden leaves cannot contribute visible text. A hidden wrapper may still contain
            // accessibility nodes that report themselves visible, so never prune a whole branch.
            state.prunedHidden++;
            return null;
        }

        state.nodes++;
        state.maxDepth = Math.max(state.maxDepth, d);
        N x = new N();
        x.parent = parent;
        try { n.getBoundsInScreen(x.b); }
        catch (RuntimeException staleNode) { state.truncate("محدوده‌ی دیداری یک گره قابل خواندن نبود"); }
        CharSequence t = null;
        if (visible) {
            try { t = n.getText(); }
            catch (RuntimeException staleNode) { state.truncate("متن یک گره قابل خواندن نبود"); }
            if (t == null || t.length() == 0) {
                try { t = n.getContentDescription(); }
                catch (RuntimeException staleNode) { state.truncate("توضیح یک گره قابل خواندن نبود"); }
            }
        }
        if (t != null) {
            String value = t.toString().replaceAll("[\\u200e\\u200f\\u202a-\\u202e\\u2066-\\u2069]", "")
                    .replace("\n", ", ").trim();
            if (!value.isEmpty()) {
                x.text = value.length() > 300 ? value.substring(0, 300) : value;
                String normalized = Fmt.norm(x.text).toLowerCase(Locale.ROOT);
                if (normalized.contains("بارگذاری بیشتر") || normalized.contains("نمایش بیشتر")
                        || normalized.contains("مشاهده بیشتر") || normalized.contains("ادامه فهرست")
                        || normalized.contains("load more") || normalized.contains("show more")
                        || normalized.contains("next page")) state.hasLoadMoreIndicator = true;
            }
        }
        for (int i = 0; i < childCount; i++) {
            if (state.visitedNodes >= MAX_TREE_NODES) {
                state.truncate("حد تعداد گره " + MAX_TREE_NODES + " رسید");
                break;
            }
            AccessibilityNodeInfo child = null;
            try { child = n.getChild(i); }
            catch (RuntimeException staleChild) {
                state.truncate("یکی از فرزندان صفحه هنگام استخراج ناپدید شد");
                continue;
            }
            if (child == null) continue;
            try {
                N k = build(child, x, d + 1, state, pruneHidden);
                if (k != null) x.kids.add(k);
            } catch (RuntimeException staleChild) {
                state.truncate("استخراج یکی از شاخه‌های صفحه ناقص ماند");
            } finally {
                try { child.recycle(); } catch (RuntimeException ignored) {}
            }
        }
        return x;
    }

    private static boolean mark(N x) {
        boolean any = false;
        for (N k : x.kids) {
            if (mark(k)) any = true;
            x.lv.addAll(k.lv);
        }
        if (x.text != null) {
            if (!any) {
                x.leaf = true;
                x.lv.add(x);
            }
            return true;
        }
        return any;
    }

    private static void count(N x) {
        x.pc = 0;
        x.vc = 0;
        if (x.leaf) {
            if (x.price) x.pc = 1;
            if (x.isVol) x.vc = 1;
        }
        for (N k : x.kids) {
            count(k);
            x.pc += k.pc;
            x.vc += k.vc;
        }
    }

    /**
     * Flat lists: title (volume + duration) and price are sibling text leaves with no card container.
     * Pair each title with the nearest price below it that horizontally overlaps it. Packages already
     * found through cards are not duplicated (same operator + volume + duration + price).
     */
    private static void flatPairs(N tree, Op op, Map<String, Pkg> out) {
        final List<N> titles = new ArrayList<>(), prices = new ArrayList<>(), units = new ArrayList<>();
        final List<N> leaves = new ArrayList<>();
        for (N l : tree.lv) {
            if (l.b.bottom <= l.b.top) continue;
            leaves.add(l);
            if (l.unitOnly) units.add(l);
            else if (l.price) prices.add(l);
            else if (l.isVol) titles.add(l);
        }
        if (titles.isEmpty() || prices.isEmpty()) return;

        // Accessibility traversal order is not guaranteed to match visual order in WebView/Compose.
        java.util.Comparator<N> visualOrder = (a, b) -> {
            int y = Integer.compare(a.cy(), b.cy());
            return y != 0 ? y : Integer.compare(a.cx(), b.cx());
        };
        java.util.Collections.sort(titles, visualOrder);
        java.util.Collections.sort(prices, visualOrder);
        java.util.Collections.sort(leaves, visualOrder);

        final int rootWidth = Math.max(1, tree.b.width());
        final int rootHeight = Math.max(1, tree.b.height());
        final int maxGap = Math.max(180, Math.min(560, Math.round(rootHeight * 0.25f)));
        final Set<N> usedPrices = new HashSet<>();
        for (N titleNode : titles) {
            double[] volume = vol(titleNode.text);
            if (volume == null) continue;

            // A new title at a lower visual row is a hard boundary between two offers.
            int nextTitleTop = Integer.MAX_VALUE;
            int sameRowTitles = 0;
            for (N other : titles) {
                if (other == titleNode) continue;
                int dy = other.cy() - titleNode.cy();
                if (Math.abs(dy) <= Math.max(16, titleNode.b.height())) sameRowTitles++;
                if (dy > Math.max(18, titleNode.b.height())) nextTitleTop = Math.min(nextTitleTop, other.b.top);
            }
            int endY = Math.min(titleNode.cy() + maxGap, nextTitleTop == Integer.MAX_VALUE
                    ? titleNode.cy() + maxGap : nextTitleTop - 1);
            int startY = titleNode.cy() - Math.max(24, titleNode.b.height() * 2);

            List<N> segment = new ArrayList<>();
            for (N l : leaves) {
                if (l.cy() >= startY && l.cy() <= endY) segment.add(l);
            }
            if (segment.isEmpty()) continue;

            List<N> candidatePrices = new ArrayList<>();
            Map<N, Integer> kinds = new java.util.IdentityHashMap<>();
            Map<N, Double> scores = new java.util.IdentityHashMap<>();
            for (N priceNode : prices) {
                if (usedPrices.contains(priceNode)) continue;
                if (priceNode.cy() < startY || priceNode.cy() > endY) continue;
                double dx = Math.abs(priceNode.cx() - titleNode.cx());
                // In one-column cards price and title can be on opposite sides of the row.
                // In a multi-column row, however, x-distance separates adjacent offers.
                if (sameRowTitles > 0 && dx > rootWidth * 0.52) continue;
                candidatePrices.add(priceNode);
                kinds.put(priceNode, kind(segment, priceNode));
                double dy = Math.abs(priceNode.cy() - titleNode.cy());
                scores.put(priceNode, dy + (sameRowTitles > 0 ? dx * 0.35 : dx * 0.04));
            }
            if (candidatePrices.isEmpty()) continue;

            // Prefer a single explicitly labelled base/payable price. Old-price, discount and
            // per-unit leaves must never displace that value.
            List<N> base = new ArrayList<>();
            List<N> main = new ArrayList<>();
            List<N> normal = new ArrayList<>();
            for (N c : candidatePrices) {
                int k = kinds.get(c);
                if (k == 5) base.add(c);
                else if (k == 4) main.add(c);
                else if (k == 0) normal.add(c);
            }
            // «قیمت پایه» is stronger evidence than a VAT-inclusive/final checkout amount.
            // Only multiple base-price candidates remain ambiguous; otherwise choose one base.
            List<N> plausible = !base.isEmpty() ? base
                    : !main.isEmpty() ? main : !normal.isEmpty() ? normal : candidatePrices;
            boolean ambiguous = !base.isEmpty() ? base.size() > 1 : main.size() > 1;
            if (plausible.size() > 1 && main.isEmpty()) {
                List<N> filtered = new ArrayList<>(plausible);
                for (N small : plausible) {
                    for (N large : plausible) {
                        if (large.amt > small.amt * 1.2 && explains(
                                Math.round(toman(small.amt, small.unit, op.cur)),
                                Math.round(toman(large.amt, large.unit, op.cur)), volume[0], dur(titleNode.text))) {
                            filtered.remove(small);
                        }
                    }
                }
                if (!filtered.isEmpty()) plausible = filtered;
                if (plausible.size() > 1) ambiguous = true;
            }
            N chosen = null;
            for (N c : plausible) {
                if (chosen == null || scores.get(c) < scores.get(chosen)) chosen = c;
            }
            if (chosen == null) continue;
            usedPrices.add(chosen);

            // Duration may be in a sibling text node. Only use a nearby duration from this
            // visual offer segment, never the duration of a later card in the same list.
            String durationText = "";
            double days = dur(titleNode.text);
            if (days <= 0) {
                N bestDuration = null;
                double bestDistance = Double.MAX_VALUE;
                for (N l : segment) {
                    if (l == titleNode || l == chosen || l.price || l.unitOnly || l.isVol) continue;
                    double d = dur(l.text);
                    if (d <= 0) continue;
                    double dx = Math.abs(l.cx() - titleNode.cx());
                    double dy = Math.abs(l.cy() - titleNode.cy());
                    double distance = dy + dx * 0.08;
                    if (distance < bestDistance && distance <= maxGap * 0.65) {
                        bestDistance = distance;
                        bestDuration = l;
                    }
                }
                if (bestDuration != null) {
                    days = dur(bestDuration.text);
                    durationText = bestDuration.text;
                }
            }

            int unit = chosen.unit;
            if (unit == 0) {
                N nearest = null;
                double nearestDistance = Double.MAX_VALUE;
                for (N u : units) {
                    double dx = Math.abs(u.cx() - chosen.cx());
                    double dy = Math.abs(u.cy() - chosen.cy());
                    double distance = dy + dx * 0.08;
                    if (distance < 120 && distance < nearestDistance) {
                        nearestDistance = distance;
                        nearest = u;
                    }
                }
                if (nearest != null) unit = nearest.unit;
            }
            if (unit == 0) unit = "IRR".equals(op.cur) ? 1 : "IRT".equals(op.cur) ? 2 : 0;

            long toman = Math.round(toman(chosen.amt, unit, op.cur));
            // A structured card may already have recovered this exact offer under a more
            // meaningful heading (for example «رومینگ ترکیه ۳۰روزه» + a separate 10GB leaf).
            // Do not add a second, weaker flat-row interpretation of the same amount/volume.
            boolean alreadyRecovered = false;
            for (Pkg existing : out.values()) {
                if (Math.abs(existing.gb - volume[0]) < 0.0001
                        && existing.toman == toman
                        && (days <= 0 || existing.days <= 0 || Math.abs(existing.days - days) < 0.01)) {
                    alreadyRecovered = true;
                    break;
                }
            }
            if (alreadyRecovered) continue;

            Pick pk = new Pick();
            pk.toman = toman;
            pk.currencyKnown = unit != 0;
            pk.ambig = ambiguous || unit == 0;
            Set<Long> alternatives = new java.util.LinkedHashSet<>();
            for (N c : candidatePrices) {
                int cu = c.unit;
                if (cu == 0) cu = "IRR".equals(op.cur) ? 1 : "IRT".equals(op.cur) ? 2 : 0;
                if (cu != 0) alternatives.add(Math.round(toman(c.amt, cu, op.cur)));
                if (c != chosen && (kinds.get(c) == 0 || kinds.get(c) == 4) && !main.isEmpty()) {
                    // A non-winning unlabelled value is not a contradiction when one explicit
                    // base/payable label identified the selected candidate.
                    continue;
                }
            }
            if (unit == 0) pk.note = "واحد پول در متن صفحه مشخص نبود؛ نیازمند بررسی دستی";
            if (ambiguous) pk.note = "چند مبلغ هم‌زمان برای این بسته دیده شد؛ قیمت قطعی نیست";
            pk.alts = new long[alternatives.size()];
            int ai = 0;
            for (Long a : alternatives) pk.alts[ai++] = a;

            String title = titleNode.text;
            if (durationText.length() > 0 && dur(title) <= 0) title = title + " " + durationText;
            StringBuilder context = new StringBuilder();
            String intlTitle = "";
            for (N leaf : segment) {
                if (leaf.price || leaf.unitOnly || leaf == titleNode) continue;
                if (context.length() > 0) context.append(' ');
                context.append(leaf.text);
                if (intlTitle.isEmpty() && INTL.matcher(Fmt.norm(leaf.text)).find()) intlTitle = leaf.text;
            }
            boolean international = INTL.matcher(Fmt.norm(context.toString())).find();
            if (international && !intlTitle.isEmpty()) title = intlTitle + " " + title;
            String why = international ? INTL_WHY : whyOf(title + " " + context, days, (int) volume[1]);
            Pkg pkg = mk(op, title, volume[0], days, toman, why, null, pk, title);
            if (pkg == null) continue;
            // A well-formed accessibility card already has stronger structure than the flat
            // geometry fallback. Never overwrite it with a weaker inferred pairing.
            if (!out.containsKey(pkg.key)) out.put(pkg.key, pkg);
        }
    }

    private static void flatten(N x, List<N> out) {
        out.add(x);
        for (N k : x.kids) flatten(k, out);
    }

    /** Cards = repeating sibling branches that each hold a price and a volume (works whatever the number of prices per card). */
    private static void findCards(N x, List<N> out, int depth) {
        if (depth > 70) return;
        List<N> ks = new ArrayList<>();
        for (N k : x.kids) if (k.pc > 0 && k.vc > 0) ks.add(k);
        if (ks.size() >= 2) {
            for (N k : ks) {
                int n = 0;
                for (N g : k.kids) if (g.pc > 0 && g.vc > 0) n++;
                if (n >= 2) findCards(k, out, depth + 1);
                else out.add(k);
            }
        } else if (ks.size() == 1) {
            findCards(ks.get(0), out, depth + 1);
        } else if (x.pc > 0 && x.vc > 0) {
            out.add(x);
        }
    }

    // ---------------------------------------------------------------- parsing

    /** returns {amount, unit} or null. unit: 0 none, 1 rial, 2 toman */
    private static double[] price(String t) { return price(t, false); }

    private static double[] price(String t, boolean allowVol) {
        String s = Fmt.norm(t);
        if (s.contains("%") || s.contains("\u066a") || s.contains("درصد")) return null;
        boolean hasUnit = UNIT.matcher(s).find();
        // a leaf that holds a volume is a TITLE ("38 گیگ 30 روز 333000 تومان"), never the price leaf itself
        if (!allowVol && VOL.matcher(s).find()) return null;
        if (!hasUnit && (VOL.matcher(s).find() || DUR.matcher(s).find())) return null;
        Matcher m = PRICE.matcher(s);
        while (m.find()) {
            String num = m.group(1), mul = m.group(2), un = m.group(3);
            boolean sep = SEP.matcher(num).matches();
            boolean whole = s.trim().equals(m.group().trim());
            if (un == null && mul == null && !sep && !(whole && num.matches("\\d{4,}"))) continue;
            double v = sep ? Double.parseDouble(num.replace(",", "").replace(".", "")) : Double.parseDouble(num);
            if (mul != null) v *= mul.equals("میلیون") ? 1e6 : 1e3;
            if (v < 100) return null;
            int unit = un == null ? 0 : un.equals("ریال") ? 1 : 2;
            return new double[]{v, unit};
        }
        return null;
    }

    /** returns {firstGb, distinctCount} or null */
    static double[] vol(String s) {
        Matcher m = VOL.matcher(Fmt.norm(s));
        double first = -1;
        Set<Double> seen = new HashSet<>();
        while (m.find()) {
            double n = Double.parseDouble(m.group(1));
            String u = m.group(2).toLowerCase(Locale.ROOT);
            double g = (u.startsWith("گیگ") || u.equals("gb")) ? n
                    : (u.startsWith("مگ") || u.equals("mb")) ? n / 1024.0 : n * 1024.0;
            if (first < 0) first = g;
            seen.add(g);
        }
        return first < 0 ? null : new double[]{first, seen.size()};
    }

    /** how many different volumes a title mentions */
    static int volCount(String title) {
        double[] v = vol(title);
        return v == null ? 0 : (int) v[1];
    }

    private static double word(String w) {
        switch (w) {
            case "یک": return 1;
            case "دو": return 2;
            case "سه": return 3;
            case "چهار": return 4;
            case "پنج": return 5;
            case "شش": return 6;
            case "هفت": return 7;
            case "هشت": return 8;
            case "نه": return 9;
            case "ده": return 10;
            case "پانزده": return 15;
            case "بیست": return 20;
            case "سی": return 30;
            case "شصت": return 60;
            case "نود": return 90;
            default: return 12;
        }
    }

    /** days, 0 if unknown */
    private static double dur(String s) {
        String n = Fmt.norm(s);
        Matcher m = DUR.matcher(n);
        if (m.find()) {
            double v = Double.parseDouble(m.group(1));
            String u = m.group(2);
            if (u.equals("روز")) return v;
            if (u.equals("هفته")) return v * 7;
            if (u.equals("ماه")) return v * 30;
            if (u.equals("سال")) return v * 365;
            return v / 24.0; // ساعت
        }
        Matcher w = DURN.matcher(n);
        if (w.find()) {
            double v = word(w.group(1));
            String u = w.group(2);
            return u.startsWith("روز") ? v : u.startsWith("هفته") ? v * 7 : u.startsWith("ماه") ? v * 30 : v * 365;
        }
        Matcher x = DURW.matcher(n);
        if (x.find()) {
            String u = x.group(1);
            return u.equals("روزانه") ? 1 : u.equals("هفتگی") ? 7 : u.equals("ماهانه") ? 30 : 365;
        }
        return 0;
    }

    /** price (toman) written inside the title itself, or -1 */
    static long titlePrice(String title) {
        Matcher m = TPRICE.matcher(Fmt.norm(title));
        long last = -1;
        while (m.find()) {
            double v = Double.parseDouble(m.group(1).replace(",", ""));
            if (m.group(2) != null) v *= 1000;
            if (v >= 1000) last = Math.round(v);
        }
        return last;
    }

    /** stable identity of a package: never contains its price, and splits same-title cards that differ in kind */
    static String keyOf(String opId, String title, String code, String why) {
        if (code != null && !code.isEmpty()) return opId + "|" + code;
        String t = Fmt.norm(title);
        t = TPRICE.matcher(t).replaceAll("").replace(" ", "");
        return opId + "|" + t + (why == null || why.isEmpty() ? "" : "#" + why);
    }

    private static double toman(double amt, int unit, String cur) {
        int resolved = unit;
        if (resolved == 0) resolved = "IRR".equals(cur) ? 1 : "IRT".equals(cur) ? 2 : 0;
        if (resolved == 1) return amt / 10.0;
        if (resolved == 2) return amt;
        // Unknown currency is deliberately not guessed; the package is marked non-comparable.
        return amt;
    }

    static String restr(String s) { return whyOf(s, 0, 1); }

    /** why a package must not be compared with ordinary ones ("" = ordinary). */
    static final String INTL_WHY = "بین‌الملل/رومینگ";

    /** Merge newly parsed labels with previously persisted labels that may no longer be present in raw text. */
    static String mergeWhy(String first, String second) {
        List<String> reasons = new ArrayList<>();
        String[] sources = {first == null ? "" : first, second == null ? "" : second};
        for (String source : sources) {
            for (String part : source.split("،")) {
                String reason = part.trim();
                if (!reason.isEmpty() && !reasons.contains(reason)) reasons.add(reason);
            }
        }
        StringBuilder out = new StringBuilder();
        for (String reason : reasons) {
            if (out.length() > 0) out.append("، ");
            out.append(reason);
        }
        return out.toString();
    }

    static String whyOf(String s, double days, int volCount) {
        String n = Fmt.norm(s);
        List<String> reasons = new ArrayList<>();
        if (R_NAZR.matcher(n).find()) reasons.add("بسته‌ی نذر آب/طرح ویژه");
        if (R_NIGHT.matcher(n).find()) reasons.add("بسته‌ی شبانه");
        if (R_TIME.matcher(n).find()) reasons.add("ساعت خاص");
        if (R_DAYS.matcher(n).find()) reasons.add("روزهای خاص");
        if (R_USER.matcher(n).find()) reasons.add("ویژه‌ی مشترکین خاص");
        if (R_NAT.matcher(n).find()) reasons.add("ترافیک ملی");
        if (R_BUNDLE.matcher(n).find()) reasons.add("بسته‌ی ترکیبی");
        if (R_SUB.matcher(n).find()) reasons.add("همراه با اشتراک/هدیه");
        if (R_RENEW.matcher(n).find()) reasons.add("تمدید خودکار/رزرو");
        if (R_UNL.matcher(n).find()) reasons.add("نامحدود/دارای حد مصرف منصفانه");
        if (R_APP.matcher(n).find()) reasons.add("ویژه‌ی اپلیکیشن");
        if (R_SPECIAL.matcher(n).find()) reasons.add("ویژه‌ی سرویس/مشترک خاص");
        if (R_NET.matcher(n).find()) reasons.add("ترافیک خاص/داخلی");
        if (days > 0 && days < 1 && !reasons.contains("ساعت خاص")) reasons.add("بسته‌ی ساعتی");
        if (volCount > 1) reasons.add("حجم ترکیبی");
        if (reasons.isEmpty()) return "";
        StringBuilder out = new StringBuilder();
        for (String reason : reasons) {
            if (out.indexOf(reason) >= 0) continue;
            if (out.length() > 0) out.append("، ");
            out.append(reason);
        }
        return out.toString();
    }

    // ---------------------------------------------------------------- price choice

    private static String near(List<N> lv, int i, int d) {
        N me = lv.get(i);
        for (int j = i + d; j >= 0 && j < lv.size(); j += d) {
            N o = lv.get(j);
            // A neighbouring amount is a boundary: a label belongs to the closest amount only.
            // Skipping it made «قیمت پایه» attach to every price after it in long operator screens.
            if (o.price) return "";
            if (o.unitOnly) continue;
            String t = Fmt.norm(o.text);
            if (t.length() > 18) return "";
            // a label only qualifies the price if it is physically next to it (same row / directly above or below)
            int gap = Math.max(0, Math.max(me.b.top, o.b.top) - Math.min(me.b.bottom, o.b.bottom));
            if (gap > 0.6 * Math.max(1, me.b.height())) return "";
            // a bare duration chip ("روزانه") is the package type, not a per-day price label
            if (DURCHIP.matcher(t).matches()) return "";
            return t;
        }
        return "";
    }

    private static int kind(List<N> lv, N leaf) {
        // A label embedded in the amount's own text is unambiguous.
        int own = labelKind(leaf.text == null ? "" : leaf.text);
        if (own != 0) return own;

        // Prefer visual geometry over accessibility traversal order. In Compose/WebView trees,
        // the node immediately before an amount may belong to the next row, while the correct
        // label is a sibling on the same visual line. Use neighbour order only when the app
        // supplied no usable screen bounds.
        if (leaf.b.width() <= 0 || leaf.b.height() <= 0) {
            int i = lv.indexOf(leaf);
            String adjacent = (near(lv, i, -1) + " " + near(lv, i, 1))
                    .replace("پرداخت مستقیم", " ").replace("از اعتبار", " ").replace("خرید", " ");
            return labelKind(adjacent);
        }

        int bestKind = 0;
        double bestDistance = Double.MAX_VALUE;
        for (N other : lv) {
            if (other == leaf || other.price || other.unitOnly || other.text == null) continue;
            String label = Fmt.norm(other.text);
            if (label.length() > 64) continue;
            int k = labelKind(label);
            if (k == 0) continue;
            int dx = Math.max(0, Math.max(leaf.b.left, other.b.left)
                    - Math.min(leaf.b.right, other.b.right));
            int dy = Math.max(0, Math.max(leaf.b.top, other.b.top)
                    - Math.min(leaf.b.bottom, other.b.bottom));
            double distance = dy + dx * 0.22;
            double limit = Math.max(110.0, leaf.b.height() * 4.0);
            if (distance > limit || distance >= bestDistance) continue;

            // One label can describe only one amount. Do not inherit «قیمت پایه» across
            // a second, physically closer amount in the same package card.
            boolean closerPriceExists = false;
            for (N otherPrice : lv) {
                if (otherPrice == leaf || !otherPrice.price) continue;
                int odx = Math.max(0, Math.max(other.b.left, otherPrice.b.left)
                        - Math.min(other.b.right, otherPrice.b.right));
                int ody = Math.max(0, Math.max(other.b.top, otherPrice.b.top)
                        - Math.min(other.b.bottom, otherPrice.b.bottom));
                double otherDistance = ody + odx * 0.22;
                if (otherDistance + 0.5 < distance) {
                    closerPriceExists = true;
                    break;
                }
            }
            if (!closerPriceExists) {
                bestDistance = distance;
                bestKind = k;
            }
        }
        return bestKind;
    }

    private static int labelKind(String context) {
        String s = Fmt.norm(context).replace("پرداخت مستقیم", " ")
                .replace("از اعتبار", " ").replace("خرید", " ");
        if (K_BASE.matcher(s).find()) return 5;
        if (K_OLD.matcher(s).find()) return 2;
        if (K_UNIT.matcher(s).find()) return 1;
        if (K_MAIN.matcher(s).find()) return 4;
        if (K_DISC.matcher(s).find()) return 3;
        return 0;
    }

    /** is `small` just `large` split per month / week / day / GB ? */
    private static boolean explains(long small, long large, double gb, double days) {
        if (small <= 0) return false;
        double r = (double) large / small;
        double[] ds = {days / 30.0, days / 7.0, days, gb};
        for (double d : ds) if (d >= 1.8 && Math.abs(r - d) / d <= 0.07) return true;
        return false;
    }

    private static String kindName(int k) {
        switch (k) {
            case 5: return "قیمت پایه";
            case 4: return "قیمت نهایی/با مالیات";
            case 1: return "مبلغ ماهانه/واحدی";
            case 2: return "قیمت قبلی";
            case 3: return "تخفیف/اعتبار";
            default: return "مبلغ دیگر";
        }
    }

    private static Pick pick(N card, List<N> pl, int cardUnit, Op op, double gb, double days) {
        List<Cand> cs = new ArrayList<>();
        boolean hasKnownCurrencyCandidate = false;
        boolean hasUnknownCurrencyCandidate = false;
        for (N l : pl) {
            Cand c = new Cand();
            c.leaf = l;
            int resolvedUnit = l.unit != 0 ? l.unit : cardUnit;
            if (resolvedUnit == 0) resolvedUnit = "IRR".equals(op.cur) ? 1 : "IRT".equals(op.cur) ? 2 : 0;
            c.currencyKnown = resolvedUnit != 0;
            hasKnownCurrencyCandidate |= c.currencyKnown;
            hasUnknownCurrencyCandidate |= !c.currencyKnown;
            c.toman = Math.round(toman(l.amt, resolvedUnit, op.cur));
            c.kind = kind(card.lv, l);
            cs.add(c);
        }
        // Never compare an amount with an unknown unit against normalized Toman values.
        // Prefer evidence-backed candidates, but keep the package ambiguous if an unknown
        // competing amount exists on the same card.
        List<Cand> comparable = new ArrayList<>();
        for (Cand c : cs) if (!hasKnownCurrencyCandidate || c.currencyKnown) comparable.add(c);
        Cand best = null;
        if (op.policy == 1 || op.policy == 2) {
            for (Cand c : comparable) {
                if (best == null || (op.policy == 1 ? c.toman > best.toman : c.toman < best.toman)) best = c;
            }
        } else {
            List<Cand> base = new ArrayList<>();
            List<Cand> main = new ArrayList<>();
            List<Cand> f = new ArrayList<>();
            for (Cand c : comparable) {
                if (c.kind == 5) base.add(c);
                else if (c.kind == 4) main.add(c);
                else if (c.kind == 0) f.add(c);
            }
            if (!base.isEmpty()) f = base;
            else if (!main.isEmpty()) f = main;
            if (f.isEmpty()) f.addAll(comparable);
            if (f.size() > 1) {
                List<Cand> g = new ArrayList<>(f);
                for (Cand s : f) {
                    for (Cand big : f) if (big.toman > s.toman * 1.2 && explains(s.toman, big.toman, gb, days)) g.remove(s);
                }
                if (!g.isEmpty()) f = g;
            }
            if (f.size() > 1) {
                int maxH = 0;
                for (Cand c : f) maxH = Math.max(maxH, c.leaf.b.height());
                List<Cand> g = new ArrayList<>();
                for (Cand c : f) if (c.leaf.b.height() >= 0.88 * maxH) g.add(c);
                if (!g.isEmpty()) f = g;
            }
            if (f.size() > 1) {
                List<Cand> g = new ArrayList<>();
                for (Cand c : f) if (c.kind == 4) g.add(c);
                if (!g.isEmpty() && g.size() < f.size()) f = g;
            }
            for (Cand c : f) if (best == null || c.toman < best.toman) best = c;
        }
        Pick pk = new Pick();
        pk.toman = best.toman;
        pk.currencyKnown = best.currencyKnown;
        Set<Long> distinct = new HashSet<>();
        for (Cand c : comparable) distinct.add(c.toman);
        pk.alts = new long[distinct.size()];
        int i = 0;
        for (long v : distinct) pk.alts[i++] = v;
        StringBuilder note = new StringBuilder();
        boolean other = false;
        for (Cand c : comparable) {
            if (c.toman == best.toman) continue;
            other = true;
            String nm = explains(Math.min(c.toman, best.toman), Math.max(c.toman, best.toman), gb, days)
                    ? "مبلغ ماهانه/واحدی" : kindName(c.kind);
            if (note.length() > 0) note.append("، ");
            note.append(Fmt.num(c.toman)).append(" (").append(nm).append(')');
        }
        if (other) pk.note = "مبالغ دیگر در همین کارت: " + note;
        // ambiguous when the decision was not backed by a label / unit-price rule
        boolean unexplained = false;
        int baseCount = 0, mainCount = 0;
        for (Cand c : comparable) if (c.kind == 5) baseCount++;
        for (Cand c : comparable) if (c.kind == 4) mainCount++;
        for (Cand c : comparable) {
            if (c.toman == best.toman) continue;
            // One explicit base price can safely outrank a derived VAT-inclusive total.
            // Multiple base labels, or multiple final-price labels without a base label,
            // are genuinely conflicting and must stay out of the definitive ranking.
            if ((c.kind == 5 && best.kind != 5)
                    || (baseCount == 0 && c.kind == 4)
                    || (baseCount == 0 && mainCount <= 1 && best.kind != 4 && c.kind == 0
                        && !explains(Math.min(c.toman, best.toman), Math.max(c.toman, best.toman), gb, days))) {
                unexplained = true;
            }
        }
        if ((baseCount > 1) || (baseCount == 0 && mainCount > 1)) unexplained = true;
        pk.ambig = !pk.currencyKnown || hasUnknownCurrencyCandidate || (unexplained && op.policy == 0);
        if (hasUnknownCurrencyCandidate && hasKnownCurrencyCandidate) {
            pk.note = (pk.note.isEmpty() ? "" : pk.note + "، ")
                    + "یک مبلغ رقیب در همین کارت واحد پول مشخصی نداشت؛ انتخاب قیمت نیازمند بررسی است";
        }
        if (!pk.currencyKnown) {
            pk.note = (pk.note.isEmpty() ? "" : pk.note + "، ")
                    + "واحد پول در متن صفحه مشخص نبود؛ این مبلغ تا زمان تأیید واحد، قابل مقایسه نیست";
        }
        return pk;
    }

    // ---------------------------------------------------------------- building packages

    private static N nearestDur(N card, N from) {
        N best = null;
        double bd = Double.MAX_VALUE;
        for (N l : card.lv) {
            if (l.price || l.unitOnly || dur(l.text) <= 0) continue;
            double d = Math.hypot(l.cx() - from.cx(), l.cy() - from.cy());
            if (d < bd) {
                bd = d;
                best = l;
            }
        }
        return best;
    }

    private static String rawOf(N card) {
        StringBuilder sb = new StringBuilder();
        for (N l : card.lv) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(l.text);
            if (sb.length() > 420) break;
        }
        return sb.length() > 420 ? sb.substring(0, 420) : sb.toString();
    }

    /** Collect short category headings preceding this card in ancestor branches. */
    private static String categoryContext(N card) {
        StringBuilder out = new StringBuilder();
        N branch = card;
        int depth = 0;
        while (branch.parent != null && depth++ < 12) {
            N parent = branch.parent;
            for (N sibling : parent.kids) {
                if (sibling == branch) break;
                if (sibling.pc > 0 || sibling.vc > 0) continue;
                for (N leaf : sibling.lv) {
                    String t = Fmt.norm(leaf.text);
                    if (t.length() <= 48 && R_CATEGORY.matcher(t).find()) {
                        if (out.length() > 0) out.append(' ');
                        out.append(t);
                    }
                }
            }
            branch = parent;
        }
        return out.toString();
    }

    private static Pkg make(N card, Op op) {
        int separateTitles = 0;
        for (N l : card.lv) if (l.isVol) separateTitles++;
        // A real offer card normally has one title leaf. If a parent branch contains several
        // volume-bearing leaves, it is a list/group, not a single offer; parsing it as one card
        // is what previously attached one package title to another package's price.
        if (separateTitles > 1) return null;
        List<N> pl = new ArrayList<>();
        int cardUnit = 0;
        for (N l : card.lv) {
            if (l.price) pl.add(l);
            else if (l.unitOnly) cardUnit = l.unit;
        }
        if (pl.isEmpty()) return null;
        N tl = null;
        boolean tlDur = false;
        for (N l : card.lv) {
            if (!l.isVol) continue;
            boolean d = dur(l.text) > 0;
            if (tl == null || (d && !tlDur)) {
                tl = l;
                tlDur = d;
            }
        }
        if (tl == null) return null;
        double[] v = vol(tl.text);
        double days = dur(tl.text);
        if (days <= 0) {
            N dl = nearestDur(card, tl);
            if (dl != null) days = dur(dl.text);
        }
        Pick pk = pick(card, pl, cardUnit, op, v[0], days);
        long tp = titlePrice(tl.text);
        if (tp > 0 && tp != pk.toman) {
            boolean inAlts = false;
            for (long a : pk.alts) if (a == tp) inAlts = true;
            if (inAlts) {
                pk.toman = tp;
                pk.currencyKnown = true; // titlePrice() only accepts explicit toman markers
                pk.ambig = false;
            } else {
                pk.ambig = true;
                pk.note = (pk.note.isEmpty() ? "" : pk.note + "، ") + "قیمت داخل عنوان: " + Fmt.num(tp) + " تومان";
            }
        }
        StringBuilder all = new StringBuilder();
        for (N l : card.lv) if (!l.price && !l.unitOnly) all.append(l.text).append(' ');
        boolean intl = INTL.matcher(Fmt.norm(all.toString())).find();
        String why = intl ? INTL_WHY : whyOf(all.toString(), days, (int) v[1]); // own text only: page tabs (e.g. "ساعات خاص") must not tag every card
        String shownTitle = tl.text;
        if (intl) for (N l : card.lv) if (!l.price && !l.unitOnly && INTL.matcher(Fmt.norm(l.text)).find()) { shownTitle = l.text; break; }
        String raw = rawOf(card);
        return mk(op, shownTitle, v[0], days, pk.toman, why, "", pk, raw);
    }

    private static Pkg fromText(N x, Op op) {
        String s = x.text;
        if (s.length() > 250 || x.lv.size() > 16) return null;
        double[] v = vol(s);
        if (v == null || v[1] > 1) return null;
        double[] pr = price(s, true);
        if (pr == null) return null;
        String n = Fmt.norm(s);
        int sepCount = 0;
        Matcher m = SEP.matcher(n);
        while (m.find()) sepCount++;
        if (sepCount > 2) return null;
        int unit = (int) pr[1];
        if (unit == 0) unit = n.contains("ریال") ? 1 : (n.contains("تومان") || n.contains("تومن")) ? 2 : 0;
        String title = null;
        for (N l : x.lv) {
            if (vol(l.text) != null) {
                title = l.text;
                break;
            }
        }
        if (title == null) title = s.split(", ")[0];
        double days = dur(title);
        if (days <= 0) days = dur(s);
        double[] tv = vol(title);
        if (tv == null) return null;
        if (unit == 0) unit = "IRR".equals(op.cur) ? 1 : "IRT".equals(op.cur) ? 2 : 0;
        long tom = Math.round(toman(pr[0], unit, op.cur));
        String fwhy = INTL.matcher(Fmt.norm(s)).find() ? INTL_WHY : whyOf(s, days, (int) tv[1]);
        Pkg p = mk(op, title, tv[0], days, tom, fwhy, "", null, s);
        p.currencyKnown = unit != 0;
        if (!p.currencyKnown) {
            p.ambig = true;
            p.note = "واحد پول در متن صفحه مشخص نبود؛ این مبلغ تا زمان تأیید واحد، قابل مقایسه نیست";
        }
        return p;
    }

    private static Pkg mk(Op op, String title, double gb, double days, long toman, String why, String code, Pick pk, String raw) {
        if (gb < 0.05 || gb > 5000 || toman < 100 || toman > 2000000000L) return null;
        if (Fmt.norm(title).trim().startsWith("از ")) return null; // "starting from ..." tile, not a real package
        Pkg p = new Pkg();
        p.op = op.id;
        p.title = title;
        p.gb = gb;
        p.days = days > 1000 ? 0 : days;
        p.toman = toman;
        p.why = why;
        p.restricted = !why.isEmpty();
        p.code = code;
        p.raw = raw;
        if (pk != null) {
            p.note = pk.note;
            p.ambig = pk.ambig;
            p.currencyKnown = pk.currencyKnown;
            p.alts = pk.alts;
        } else {
            p.currencyKnown = true;
            p.alts = new long[]{toman};
        }
        p.key = keyOf(op.id, title, code, why);
        return p;
    }
}
