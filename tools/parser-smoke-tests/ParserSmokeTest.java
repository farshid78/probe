package app.bastehyab;
import android.view.accessibility.AccessibilityNodeInfo;
public class ParserSmokeTest {
 private static void ok(boolean value,String message){if(!value)throw new AssertionError(message);}
 private static Pkg scan(String price,String unit,String opCurrency) {
   AccessibilityNodeInfo root=new AccessibilityNodeInfo(null,0);
   AccessibilityNodeInfo card=new AccessibilityNodeInfo(null,0);
   card.add(new AccessibilityNodeInfo("۳۰ روزه ۵ گیگابایت",30));
   card.add(new AccessibilityNodeInfo(price,60));
   if(unit!=null)card.add(new AccessibilityNodeInfo(unit,60));
   root.add(card);
   Op op=new Op("x","آزمایش",0,"AUTO".equals(opCurrency)?"AUTO":opCurrency,true,"test.operator");
   Extractor.Result r=Extractor.extract(root,op,2000);
   ok(r.items.size()==1,"expected exactly one offer, got="+r.items.size()+" diagnostics="+r.dump);
   return r.items.get(0);
 }
 public static void main(String[] args) throws Exception {
   ok(Fmt.norm("۱۲۳٤٥٦ ك يـ \u200fتومان").equals("123456 ک ی تومان"),"Persian/Arabic digit and letter normalization");
   ok(Fmt.norm(null).isEmpty(),"null normalization");
   ok(Fmt.ago(System.currentTimeMillis() + 60L * 60L * 1000L).contains("جلوتر"),
      "future timestamps should be surfaced as a device-clock warning");
   Pkg rial=scan("۶۲۶٬۶۰۰","ریال","AUTO");
   ok(rial.currencyKnown,"explicit rial unit must be known");
   ok(rial.toman==62660,"rial must convert to toman exactly: "+rial.toman);
   Pkg toman=scan("۶۲۶٬۶۰۰","تومان","AUTO");
   ok(toman.currencyKnown && toman.toman==626600,"explicit toman must not be divided by ten");
   Pkg unknown=scan("۶۲۶٬۶۰۰",null,"AUTO");
   ok(!unknown.currencyKnown && unknown.ambig,"unknown unit must be flagged ambiguous");
   Pkg fixedRial=scan("۶۲۶٬۶۰۰",null,"IRR");
   ok(fixedRial.currencyKnown && fixedRial.toman==62660,"fixed IRR operator profile must convert");
   // A Samantel package may show the base price as a labelled Rial amount next to a smaller
   // unrelated amount. «قیمت پایه» is the actual base amount, not an old/discounted price.
   AccessibilityNodeInfo samantelRoot=new AccessibilityNodeInfo(null,0);
   AccessibilityNodeInfo samantelCard=new AccessibilityNodeInfo(null,0);
   samantelCard.add(new AccessibilityNodeInfo("۱۸ گیگابایت ۹۰ روزه",20));
   samantelCard.add(new AccessibilityNodeInfo("قیمت پایه",60));
   samantelCard.add(new AccessibilityNodeInfo("۱٬۸۸۷٬۱۵۶ ریال",60));
   samantelCard.add(new AccessibilityNodeInfo("قیمت نهایی با ارزش افزوده ۱۰ درصد",75));
   samantelCard.add(new AccessibilityNodeInfo("۲٬۰۷۵٬۸۷۲ ریال",90));
   samantelRoot.add(samantelCard);
   Extractor.Result samantelScan=Extractor.extract(samantelRoot,
       new Op("samantel","سامانتل",0,"IRR",true,"com.mysamantel"),2000);
   ok(samantelScan.items.size()==1,"Samantel base-price test should return one package: "+samantelScan.dump);
   Pkg samantelPkg=samantelScan.items.get(0);
   ok(samantelPkg.toman==188716,"Samantel base price 1,887,156 Rial must convert to 188,716 Toman, not the smaller unrelated amount: "+samantelPkg.toman);
   ok(!samantelPkg.ambig,"one explicit base price must outrank the derived VAT-inclusive price");
   ok(samantelPkg.note.contains("قیمت نهایی/با مالیات"),"Samantel detail should identify the VAT-inclusive alternative");
   AccessibilityNodeInfo mixedRoot=new AccessibilityNodeInfo(null,0);
   AccessibilityNodeInfo mixedCard=new AccessibilityNodeInfo(null,0);
   mixedCard.add(new AccessibilityNodeInfo("۳۰ روزه ۵ گیگابایت",30));
   mixedCard.add(new AccessibilityNodeInfo("۹۰٬۰۰۰ ریال",60));
   mixedCard.add(new AccessibilityNodeInfo("۷۰٬۰۰۰",60));
   mixedRoot.add(mixedCard);
   Extractor.Result mixed=Extractor.extract(mixedRoot,new Op("x","آزمایش",0,"AUTO",true,"test.operator"),2000);
   ok(mixed.items.size()==1,"mixed-currency test package should be detected");
   Pkg mixedPkg=mixed.items.get(0);
   ok(mixedPkg.toman==9000,"unknown raw amount must not win over normalized rial amount: "+mixedPkg.toman);
   ok(mixedPkg.ambig,"competing amount with unknown unit must make package ambiguous");
   AccessibilityNodeInfo privateRoot=new AccessibilityNodeInfo(null,0);
   AccessibilityNodeInfo privateCard=new AccessibilityNodeInfo(null,0);
   privateCard.add(new AccessibilityNodeInfo("۳۰ روزه ۵ گیگابایت",30));
   privateCard.add(new AccessibilityNodeInfo("۹۰٬۰۰۰ ریال",60).editable(true));
   privateCard.add(new AccessibilityNodeInfo("۷۰٬۰۰۰ ریال",80).password(true));
   privateCard.add(new AccessibilityNodeInfo("۸۰٬۰۰۰ ریال",100).visible(false));
   privateRoot.add(privateCard);
   Extractor.Result privateScan=Extractor.extract(privateRoot,new Op("x","آزمایش",0,"IRR",true,"test.operator"),2000);
   ok(privateScan.items.isEmpty(),"editable, password and non-visible content must not produce offers");
   Pkg privacyPkg=new Pkg(); privacyPkg.op="x"; privacyPkg.key="x|1"; privacyPkg.title="۳۰ روزه ۵ گیگابایت";
   privacyPkg.gb=5; privacyPkg.days=30; privacyPkg.toman=9000; privacyPkg.raw="این متن خام نباید ذخیره شود";
   ok(!privacyPkg.json().has("raw"),"raw accessibility-card text must not be persisted in package JSON");
   AccessibilityNodeInfo large=new AccessibilityNodeInfo(null,0);
   for(int i=0;i<8100;i++) large.add(new AccessibilityNodeInfo("",i));
   Extractor.Result limited=Extractor.extract(large,new Op("x","test",0,"AUTO",true,"test.operator"),2000);
   ok(limited.incomplete && limited.dump.contains("complete=false"),"node-cap truncation must be explicit");
   ok(!limited.treeComplete,"a node-capped hierarchy must never be used as complete end-of-list evidence");
   AccessibilityNodeInfo partialRoot=new AccessibilityNodeInfo(null,0);
   AccessibilityNodeInfo partialCard=new AccessibilityNodeInfo(null,0);
   partialCard.add(new AccessibilityNodeInfo("۳۰ روزه ۵ گیگابایت",30));
   partialCard.add(new AccessibilityNodeInfo("۶۲۶٬۶۰۰ ریال",60));
   partialRoot.add(partialCard);
   for(int i=0;i<8100;i++) partialRoot.add(new AccessibilityNodeInfo("",100+i));
   Extractor.Result partial=Extractor.extract(partialRoot,new Op("x","test",0,"AUTO",true,"test.operator"),2000);
   ok(partial.incomplete && !partial.items.isEmpty(),"partial tree should retain valid fragments and report incompleteness");
   ok(partial.items.get(0).ambig,"packages from truncated tree must be marked ambiguous");
   AccessibilityNodeInfo root=new AccessibilityNodeInfo(null,0);
   AccessibilityNodeInfo c1=new AccessibilityNodeInfo(null,0);
   c1.add(new AccessibilityNodeInfo("۳۰ روزه ۵ گیگابایت",30)); c1.add(new AccessibilityNodeInfo("۶۲۶٬۶۰۰",60)); c1.add(new AccessibilityNodeInfo("ریال",60));
   AccessibilityNodeInfo c2=new AccessibilityNodeInfo(null,100);
   c2.add(new AccessibilityNodeInfo("۳۰ روزه ۵ گیگابایت",130)); c2.add(new AccessibilityNodeInfo("۶۲۶٬۶۰۰",160)); c2.add(new AccessibilityNodeInfo("ریال",160));
   root.add(c1).add(c2);
   Extractor.Result duplicate=Extractor.extract(root,new Op("x","test",0,"AUTO",true,"test.operator"),2000);
   ok(duplicate.items.size()==1 && duplicate.duplicates==1,"duplicate cards must be counted and collapsed");
   ok(!duplicate.dump.contains("۳۰ روزه") && !duplicate.dump.contains("۶۲۶"),"diagnostic must not contain raw page text or price data");
   // A genuinely hidden subtree must be ignored even when a visible card is present.
   AccessibilityNodeInfo mixRoot=new AccessibilityNodeInfo(null,0);
   AccessibilityNodeInfo visibleCard=new AccessibilityNodeInfo(null,0);
   visibleCard.add(new AccessibilityNodeInfo("۳۰ روزه ۵ گیگابایت",30));
   visibleCard.add(new AccessibilityNodeInfo("۹۰٬۰۰۰ ریال",60));
   mixRoot.add(visibleCard);
   AccessibilityNodeInfo decoy=new AccessibilityNodeInfo(null,0).visible(false);
   decoy.add(new AccessibilityNodeInfo("۳۰ روزه ۱۰ گیگابایت",90).visible(false));
   decoy.add(new AccessibilityNodeInfo("۱۲۰٬۰۰۰ ریال",100).visible(false));
   mixRoot.add(decoy);
   Op fastOp=new Op("x","آزمایش",0,"IRR",true,"test.operator");
   Extractor.Result mixScan=Extractor.extract(mixRoot,fastOp,2000);
   ok(mixScan.items.size()==1 && !mixScan.fullTraversal && mixScan.prunedHidden>0,"fast pass must prune hidden subtrees without full re-walk when visible offers exist");

   // Hidden wrappers may contain descendants that accessibility still marks visible.
   AccessibilityNodeInfo wrapRoot=new AccessibilityNodeInfo(null,0);
   AccessibilityNodeInfo wrapper=new AccessibilityNodeInfo(null,0).visible(false);
   AccessibilityNodeInfo wrapped=new AccessibilityNodeInfo(null,0);
   wrapped.add(new AccessibilityNodeInfo("۳۰ روزه ۵ گیگابایت",30));
   wrapped.add(new AccessibilityNodeInfo("۹۰٬۰۰۰ ریال",60));
   wrapper.add(wrapped);
   wrapRoot.add(wrapper);
   Extractor.Result wrapScan=Extractor.extract(wrapRoot,fastOp,2000);
   ok(wrapScan.items.size()==1,"visible offers under a hidden wrapper must be recovered");

   // One stale Compose child must not abort extraction of healthy sibling offer cards.
   AccessibilityNodeInfo staleRoot=new AccessibilityNodeInfo(null,0);
   staleRoot.add(new AccessibilityNodeInfo(null,0).failChildCount(true));
   AccessibilityNodeInfo healthyCard=new AccessibilityNodeInfo(null,0);
   healthyCard.add(new AccessibilityNodeInfo("۳۰ روزه ۷ گیگابایت",30));
   healthyCard.add(new AccessibilityNodeInfo("۱۴۰٬۰۰۰ ریال",60));
   staleRoot.add(healthyCard);
   Extractor.Result staleScan=Extractor.extract(staleRoot,new Op("x","آزمایش",0,"IRR",true,"test.operator"),2000);
   ok(staleScan.incomplete && staleScan.items.size()==1,
      "one stale child must mark the result incomplete but preserve valid sibling offers");
   ok(!staleScan.treeComplete,"a stale child must invalidate complete-tree evidence");
   ok(staleScan.items.get(0).ambig,
      "offers from a partially readable tree must remain out of definitive rankings");

   AccessibilityNodeInfo mixedWrapperRoot=new AccessibilityNodeInfo(null,0);
   AccessibilityNodeInfo ordinaryCard=new AccessibilityNodeInfo(null,0);
   ordinaryCard.add(new AccessibilityNodeInfo("۳۰ روزه ۶ گیگابایت",30));
   ordinaryCard.add(new AccessibilityNodeInfo("۹۰٬۰۰۰ ریال",60));
   mixedWrapperRoot.add(ordinaryCard);
   AccessibilityNodeInfo wrapperWithVisibleChild=new AccessibilityNodeInfo(null,0).visible(false);
   AccessibilityNodeInfo wrappedVisibleCard=new AccessibilityNodeInfo(null,0);
   wrappedVisibleCard.add(new AccessibilityNodeInfo("۳۰ روزه ۹ گیگابایت",90));
   wrappedVisibleCard.add(new AccessibilityNodeInfo("۱۲۰٬۰۰۰ ریال",120));
   wrapperWithVisibleChild.add(wrappedVisibleCard);
   mixedWrapperRoot.add(wrapperWithVisibleChild);
   Extractor.Result mixedWrapper=Extractor.extract(mixedWrapperRoot,fastOp,2000);
   ok(mixedWrapper.items.size()==2,"visible descendants under a hidden wrapper must not be silently dropped when other cards are visible");

   // International / roaming bundles are recognised from the card's own text; page tabs never classify a card.
   ok(Extractor.INTL.matcher(Fmt.norm("رومینگ ترکیه 30روزه")).find(),"roaming bundles must be recognised");
   ok(Extractor.INTL.matcher(Fmt.norm("بسته بین الملل ۳۰ روزه")).find(),"international bundles must be recognised");
   ok(!Extractor.INTL.matcher(Fmt.norm("بسته ۳۰ روزه ۳۰ گیگابایت")).find(),"domestic bundles must not be recognised as international");
   ok(!Extractor.whyOf("بسته ۳۰ روزه ۳۰ گیگابایت بسته بین الملل",30,1).equals(Extractor.INTL_WHY),"a tab label must not turn a domestic card into an international one");
   // Megabyte volumes are read correctly (750 MB = 0.732 GB).
   ok(Math.abs(Extractor.vol("۷۵۰ مگابایت")[0] - 750/1024.0) < 1e-9,"750 MB must convert to GB");

   // Flat lists (no card container): title and price are sibling leaves and must still pair.
   AccessibilityNodeInfo flat=new AccessibilityNodeInfo(null,0);
   flat.add(new AccessibilityNodeInfo("۳۰ روزه ۵ گیگابایت",10));
   flat.add(new AccessibilityNodeInfo("۹۰٬۰۰۰ ریال",60));
   Extractor.Result flatScan=Extractor.extract(flat,new Op("x","آزمایش",0,"IRR",true,"test.operator"),2000);
   ok(flatScan.items.size()==1 && flatScan.items.get(0).toman==9000 && flatScan.items.get(0).currencyKnown,"flat title and price siblings must pair into one package");

   AccessibilityNodeInfo emptyRoot=new AccessibilityNodeInfo(null,0);
   emptyRoot.add(new AccessibilityNodeInfo("خرید بسته",30));
   emptyRoot.add(new AccessibilityNodeInfo("بازگشت",30));
   Extractor.Result emptyPkgs=Extractor.extract(emptyRoot,new Op("x","test",0,"AUTO",true,"test.operator"),2000);
   ok(emptyPkgs.items.isEmpty(),"text-only tree must not invent packages");
   ok(emptyPkgs.incomplete && emptyPkgs.dump.contains("complete=false"),"zero packages from a readable tree must be reported as an extraction failure");
   ok(emptyPkgs.treeComplete,"an empty but fully traversed page must still carry complete-tree evidence for end detection");
   AccessibilityNodeInfo hiddenLeafRoot=new AccessibilityNodeInfo(null,0);
   AccessibilityNodeInfo hiddenLeafCard=new AccessibilityNodeInfo(null,0);
   hiddenLeafCard.add(new AccessibilityNodeInfo("۳۰ روزه ۶ گیگابایت",30));
   hiddenLeafCard.add(new AccessibilityNodeInfo("۹۰٬۰۰۰ ریال",60));
   hiddenLeafRoot.add(hiddenLeafCard);
   hiddenLeafRoot.add(new AccessibilityNodeInfo("decorative hidden node",90).visible(false));
   Extractor.Result fastHidden=Extractor.extract(hiddenLeafRoot,new Op("x","test",0,"AUTO",true,"test.operator"),2000);
   Extractor.Result fullHidden=Extractor.extract(hiddenLeafRoot,new Op("x","test",0,"AUTO",true,"test.operator"),2000,true);
   ok(!fastHidden.treeComplete && fullHidden.treeComplete && fullHidden.fullTraversal,
      "confirmed full scans must traverse hidden empty branches before trusting end-of-list evidence");
   AccessibilityNodeInfo loadMoreRoot=new AccessibilityNodeInfo(null,0);
   loadMoreRoot.add(new AccessibilityNodeInfo("نمایش بیشتر بسته‌ها",40));
   Extractor.Result loadMoreResult=Extractor.extract(loadMoreRoot,
       new Op("x","test",0,"AUTO",true,"test.operator"),2000);
   ok(loadMoreResult.hasLoadMoreIndicator,
       "visible load-more controls must be passed from extraction to the full-scan end detector");
   // --- patterns taken from real operator screens (texts only; no personal data) ---
   AccessibilityNodeInfo hourRoot=new AccessibilityNodeInfo(null,0);
   AccessibilityNodeInfo hourCard=new AccessibilityNodeInfo(null,0);
   hourCard.add(new AccessibilityNodeInfo("بسته اینترنت 1گیگابایت 1روزه (6 تا 12ظهر)",30));
   hourCard.add(new AccessibilityNodeInfo("روزانه",60));
   hourCard.add(new AccessibilityNodeInfo("۲۷,۸۰۰  ریال",90));
   hourCard.add(new AccessibilityNodeInfo("از اعتبار",120));
   hourRoot.add(hourCard);
   Extractor.Result hourRes=Extractor.extract(hourRoot,new Op("x","test",0,"AUTO",true,"test.operator"),2000);
   ok(hourRes.items.size()==1,"special-hour card must be read, got="+hourRes.items.size());
   Pkg hourPkg=hourRes.items.get(0);
   ok(hourPkg.toman==2780,"rial price on a card must convert to toman: "+hourPkg.toman);
   ok(hourPkg.restricted && "ساعت خاص".equals(hourPkg.why),"special hours must come from the card text: "+hourPkg.why);

   AccessibilityNodeInfo tabRoot=new AccessibilityNodeInfo(null,0);
   tabRoot.add(new AccessibilityNodeInfo("ساعات خاص",10));
   AccessibilityNodeInfo plainCard=new AccessibilityNodeInfo(null,0);
   plainCard.add(new AccessibilityNodeInfo("بسته اینترنت 30 روزه 6 گیگابایت",30));
   plainCard.add(new AccessibilityNodeInfo("ماهانه",60));
   plainCard.add(new AccessibilityNodeInfo("۷۳۳,۴۰۰  ریال",90));
   plainCard.add(new AccessibilityNodeInfo("از اعتبار",120));
   tabRoot.add(plainCard);
   Extractor.Result tabRes=Extractor.extract(tabRoot,new Op("x","test",0,"AUTO",true,"test.operator"),2000);
   ok(tabRes.items.size()==1 && !tabRes.items.get(0).restricted,"a page tab label must never classify a normal card");
   ok(tabRes.items.get(0).toman==73340,"normal card price: "+tabRes.items.get(0).toman);

   AccessibilityNodeInfo roamRoot=new AccessibilityNodeInfo(null,0);
   AccessibilityNodeInfo roamCard=new AccessibilityNodeInfo(null,0);
   roamCard.add(new AccessibilityNodeInfo("رومینگ ترکیه 30روزه",30));
   roamCard.add(new AccessibilityNodeInfo("10گیگابایت",60));
   roamCard.add(new AccessibilityNodeInfo("۱۴,۱۲۸,۴۴۰ ریال",90));
   roamRoot.add(roamCard);
   Extractor.Result roamRes=Extractor.extract(roamRoot,new Op("x","test",0,"IRR",true,"test.operator"),2000);
   ok(roamRes.items.size()==1 && roamRes.items.get(0).restricted,"roaming must be restricted");
   ok(roamRes.items.get(0).why.equals(Extractor.INTL_WHY),"roaming reason: "+roamRes.items.get(0).why);
   ok(roamRes.items.get(0).toman==1412844,"roaming rial conversion: "+roamRes.items.get(0).toman);

   // A single aggregate Compose/RecyclerView branch must be split into visual offer rows.
   // Each price stays paired with the title and its own «قیمت پایه» label, not with a neighbour.
   AccessibilityNodeInfo flatRoot = new AccessibilityNodeInfo("root",0).bounds(0,0,1000,280);
   AccessibilityNodeInfo flatList = new AccessibilityNodeInfo(null,0).bounds(0,0,1000,280);
   flatList.add(new AccessibilityNodeInfo("۱۸ گیگابایت ۹۰ روزه",10).bounds(620,10,980,32));
   flatList.add(new AccessibilityNodeInfo("قیمت پایه",48).bounds(40,48,300,70));
   flatList.add(new AccessibilityNodeInfo("۱٬۸۸۷٬۱۵۶ ریال",74).bounds(40,74,360,96));
   flatList.add(new AccessibilityNodeInfo("۲۰ گیگابایت ۳۰ روزه",130).bounds(620,130,980,152));
   flatList.add(new AccessibilityNodeInfo("قیمت پایه",168).bounds(40,168,300,190));
   flatList.add(new AccessibilityNodeInfo("۱٬۱۹۰٬۸۳۰ ریال",194).bounds(40,194,360,216));
   flatRoot.add(flatList);
   Extractor.Result flatRes = Extractor.extract(flatRoot,
       new Op("samantel","سامانتل",0,"IRR",true,"com.mysamantel"),2000);
   ok(flatRes.items.size()==2,"aggregate list should produce two independent packages: "+flatRes.dump);
   ok(flatRes.items.get(0).toman==188716 && flatRes.items.get(1).toman==119083,
       "base prices must be paired to their own visual title: "+flatRes.dump);
   ok(!flatRes.items.get(0).ambig && !flatRes.items.get(1).ambig,
       "single labelled base prices must be definitive in separate offer rows");

   // The base-price label can be separated from its amount by decorative/description text.
   AccessibilityNodeInfo spacedRoot = new AccessibilityNodeInfo("root",0).bounds(0,0,1000,180);
   AccessibilityNodeInfo spacedCard = new AccessibilityNodeInfo(null,0).bounds(0,0,1000,180);
   spacedCard.add(new AccessibilityNodeInfo("۱۸ گیگابایت ۹۰ روزه",10).bounds(620,10,980,32));
   spacedCard.add(new AccessibilityNodeInfo("قیمت پایه",48).bounds(520,48,760,70));
   spacedCard.add(new AccessibilityNodeInfo("جزئیات بسته",80).bounds(40,80,360,102));
   spacedCard.add(new AccessibilityNodeInfo("۱٬۸۸۷٬۱۵۶ ریال",48).bounds(40,48,360,70));
   spacedCard.add(new AccessibilityNodeInfo("قیمت نهایی با ارزش افزوده ۱۰ درصد",120).bounds(520,120,900,142));
   spacedCard.add(new AccessibilityNodeInfo("۲٬۰۷۵٬۸۷۲ ریال",120).bounds(40,120,360,142));
   spacedRoot.add(spacedCard);
   Extractor.Result spacedRes = Extractor.extract(spacedRoot,
       new Op("samantel","سامانتل",0,"IRR",true,"com.mysamantel"),2000);
   ok(spacedRes.items.size()==1 && spacedRes.items.get(0).toman==188716
           && !spacedRes.items.get(0).ambig,
       "nearby spatial base-price label must outrank the VAT-inclusive amount despite interstitial nodes: "+spacedRes.dump);

   AccessibilityNodeInfo conflictRoot = new AccessibilityNodeInfo(null,0);
   AccessibilityNodeInfo conflictCard = new AccessibilityNodeInfo(null,0);
   conflictCard.add(new AccessibilityNodeInfo("۱۸ گیگابایت ۹۰ روزه",20));
   conflictCard.add(new AccessibilityNodeInfo("قیمت پایه",50));
   conflictCard.add(new AccessibilityNodeInfo("۱٬۸۸۷٬۱۵۶ ریال",70));
   conflictCard.add(new AccessibilityNodeInfo("قیمت پایه",90));
   conflictCard.add(new AccessibilityNodeInfo("۱٬۹۰۰٬۰۰۰ ریال",110));
   conflictRoot.add(conflictCard);
   Extractor.Result conflict = Extractor.extract(conflictRoot,
       new Op("samantel","سامانتل",0,"IRR",true,"com.mysamantel"),2000);
   ok(conflict.items.size()==1 && conflict.items.get(0).ambig,
       "two competing base-price labels must remain ambiguous instead of guessing: "+conflict.dump);

   // Real-world package titles from the uploaded CSV: retain all detected restrictions, not only the first.
   String giftAndApp = Extractor.whyOf("بسته اینترنت ۳۰روزه ۳۰ گیگابایت به همراه کد دیجیتال اشتراک فیلیمو ویژه روبیکا",30,1);
   ok(giftAndApp.contains("همراه با اشتراک/هدیه") && giftAndApp.contains("ویژه‌ی اپلیکیشن"),
      "gift and app limitations should both be retained: " + giftAndApp);
   String nightLimit = Extractor.whyOf("بسته اینترنت نامحدود سی روزه (۲ بامداد تا ۹ صبح، مصرف منصفانه ۸۰ گیگابایت)",30,1);
   ok(nightLimit.contains("بسته‌ی شبانه") && nightLimit.contains("ساعت خاص") && nightLimit.contains("حد مصرف منصفانه"),
      "time window and fair-use limit should be detected together: " + nightLimit);
   ok(Extractor.whyOf("بسته اینترنت ۳۰روزه ۵۰ گیگابایت",30,1).isEmpty(),
      "ordinary package must stay unclassified");
   ok(!Extractor.whyOf("بسته اینترنت ۳۰روزه ۵ تا ۱۰ گیگابایت",30,1).contains("ساعت خاص"),
      "a numeric volume range must not be mistaken for a time window");
   ok(Extractor.whyOf("بسته اینترنت ۱ گیگ از ساعت ۸ تا ۱۲",1,1).contains("ساعت خاص"),
      "an explicit hour range must be detected as a time restriction");
   ok(Extractor.whyOf("ترافیک ویژه سایت‌های ایرانی",30,1).contains("ترافیک ملی"),
      "Iranian-site traffic should be detected as national traffic");
   ok(!Extractor.whyOf("اشتراک یک سرویس ایرانی",30,1).contains("ترافیک ملی"),
      "a generic Iranian service must not be classified as national traffic");
   String migratedReasons = Extractor.mergeWhy(
      Extractor.whyOf("بسته اینترنت ۳۰روزه ۵ گیگابایت",30,1),
      "روزهای خاص، ترافیک خاص/داخلی");
   ok(migratedReasons.contains("روزهای خاص") && migratedReasons.contains("ترافیک خاص/داخلی"),
      "labels not recoverable from sanitized raw text must survive JSON reload migration");
   ok(Extractor.mergeWhy("ساعت خاص، بسته‌ی شبانه", "بسته‌ی شبانه").equals("ساعت خاص، بسته‌ی شبانه"),
      "reason merge must deduplicate repeated labels");
   Pkg regular = new Pkg(); regular.op="x"; regular.gb=5; regular.days=30; regular.toman=50000; regular.why="";
   Pkg night = new Pkg(); night.op="x"; night.gb=5; night.days=30; night.toman=50000; night.why="بسته‌ی شبانه";
   ok(!regular.sig().equals(night.sig()),"special and general bundles must not collapse into one package identity");

   // --- VAT ---
   ok(Fmt.withVat(100000,10)==110000,"VAT 10%");
   ok(Fmt.withVat(100000,0)==100000,"VAT 0%");
   ok(Fmt.withVat(100000,100)==200000,"VAT 100%");
   ok(Fmt.withVat(12345,10)==13580,"VAT rounds half up");
   boolean badVat=false; try{Fmt.withVat(1000,101);}catch(IllegalArgumentException e){badVat=true;}
   ok(badVat,"VAT above 100 must be rejected");
   badVat=false; try{Fmt.withVat(1000,-1);}catch(IllegalArgumentException e){badVat=true;}
   ok(badVat,"negative VAT must be rejected");

   // --- history: only real changes ---
   Pkg h1=new Pkg(); h1.key="k"; h1.toman=100000; h1.gb=5; h1.days=30; h1.ts=1;
   Pkg h2=new Pkg(); h2.key="k"; h2.toman=100000; h2.gb=5; h2.days=30; h2.ts=2;
   ok(History.diff(h1,h2).isEmpty(),"identical observations must not create history");
   h2.toman=120000;
   java.util.List<History.Change> hc=History.diff(h1,h2);
   ok(hc.size()==1 && hc.get(0).field.equals("price"),"price change must be recorded once");
   ok(Math.abs(hc.get(0).percent()-20.0)<1e-9,"percent change: "+hc.get(0).percent());
   h2.gb=6;
   ok(History.diff(h1,h2).size()==2,"price and volume changes are separate entries");
   h2.ambig=true;
   ok(History.diff(h1,h2).isEmpty(),"ambiguous prices must not create history");
   Pkg h3=new Pkg(); h3.key="other"; h3.toman=1; h3.gb=1; h3.days=1;
   ok(History.diff(h1,h3).isEmpty(),"different keys are never compared");
   System.out.println("PASS: normalization, null handling, currency conversion, unknown-unit rejection, mixed-unit ambiguity, editable/password/invisible-node exclusion, raw-data persistence exclusion, fixed currency, truncation visibility and package ambiguity, duplicate collapse, flat multi-offer pairing, privacy-safe diagnostics.");
 }
}
