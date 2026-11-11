package app.bastehyab;

public final class ScanServiceSmokeTest {
    private static void ok(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static android.view.accessibility.AccessibilityNodeInfo irancellTree(String offerText, int offset, String timerText) {
        android.view.accessibility.AccessibilityNodeInfo root =
                new android.view.accessibility.AccessibilityNodeInfo("root", 0)
                        .bounds(0, 0, 1080, 2400).packageName("com.myirancell");
        android.view.accessibility.AccessibilityNodeInfo home =
                new android.view.accessibility.AccessibilityNodeInfo("home", 0)
                        .bounds(6, 90, 1074, 2385).scrollable(true);
        android.view.accessibility.AccessibilityNodeInfo offers =
                new android.view.accessibility.AccessibilityNodeInfo("offers", 0)
                        .bounds(0, 421, 1080, 2400).scrollable(true);
        android.view.accessibility.AccessibilityNodeInfo card =
                new android.view.accessibility.AccessibilityNodeInfo(offerText, 0)
                        .bounds(20, 500 + offset, 1060, 760 + offset)
                        .viewId("com.myirancell:id/offerPackage");
        android.view.accessibility.AccessibilityNodeInfo price =
                new android.view.accessibility.AccessibilityNodeInfo("۲۵۰٬۰۰۰ ریال", 0)
                        .bounds(30, 650 + offset, 400, 690 + offset)
                        .viewId("com.myirancell:id/package-price");
        android.view.accessibility.AccessibilityNodeInfo timer =
                new android.view.accessibility.AccessibilityNodeInfo(timerText, 0)
                        .bounds(30, 100, 300, 130);
        card.add(price);
        offers.add(card);
        home.add(timer).add(offers);
        root.add(home);
        return root;
    }

    public static void main(String[] args) throws Exception {
        ScanService.startFull("irancell", "com.myirancell");
        ok(ScanService.fullMode && ScanService.isFullTarget("com.myirancell"),
                "Irancell's installed package must be recognized as the exact automatic-scan target");
        ok(!ScanService.isFullTarget("com.myirancell.fake"),
                "Irancell target matching must not accept package-name prefixes");
        ScanService.startFull("rightel", "ir.rightel.myrightel");
        ok(ScanService.fullMode, "auto extraction should start");
        ok(!ScanService.fullStartedScanning, "auto extraction should wait for the actual package page");
        ok(ScanService.fullStartedAt == 0L, "navigation time must not start the five-minute scan timeout");
        ok("rightel".equals(ScanService.fullTargetOpId), "operator target should be retained");
        ok(ScanService.isFullTarget("ir.rightel.myrightel"), "the selected operator package should match");
        ok(!ScanService.isFullTarget("com.android.launcher"), "other apps must not be auto-scrolled");
        ok(!ScanService.shouldStopFullForWindowChange("app.bastehyab"),
                "the launcher activity's transition must not cancel a scan before the operator opens");
        ok(!ScanService.fullConfirmed, "auto scan must wait for the user's confirmation");
        ok(!ScanService.shouldUseConfirmedPageFallback(false, false, 3),
                "a fallback swipe must never run before user confirmation");
        ok(!ScanService.shouldUseConfirmedPageFallback(true, false, 2),
                "page detection should get its bounded retry window first");
        ok(ScanService.shouldUseConfirmedPageFallback(true, false, 3),
                "a manually confirmed page must not be blocked forever by changed accessibility labels");
        ok(!ScanService.shouldUseConfirmedPageFallback(true, true, 3),
                "recognized pages must keep the regular parser-first path");
        ok(ScanService.shouldUseAccessibilityScroll(0),
                "all operators, including Irancell and MCI, should use the same native accessibility action first");
        ok(!ScanService.shouldUseAccessibilityScroll(1),
                "after an unchanged viewport, all operators should escalate to a real touch gesture");
        ok(!ScanService.shouldUseAccessibilityScroll(2),
                "after repeated no-movement observations, all operators should keep using the touch fallback");
        ok(!ScanService.shouldPreferAppViewportSwipe(1),
                "the first fallback should target the detected list region");
        ok(ScanService.shouldPreferAppViewportSwipe(2),
                "repeated no-movement should prefer a gesture across the app content viewport");
        ok(ScanService.fullProgressPercent() == 0, "progress starts at zero");
        ScanService.fullSteps = 60;
        ok(ScanService.fullProgressPercent() == 50, "fallback progress follows completed scan steps");
        ScanService.fullObservedScrollPercent = 72;
        ok(ScanService.fullProgressPercent() == 72, "observed scroll percentage takes precedence over step estimate");
        ScanService.fullObservedScrollPercent = 100;
        ok(ScanService.fullProgressPercent() == 99, "reaching viewport offset 100% is not confirmed list completion");
        ScanService.fullSteps = 0;
        ScanService.fullObservedScrollPercent = -1;
        ScanService.postponePrompt();
        ok(ScanService.promptDismissed && !ScanService.fullConfirmed, "postponing must hide the dialog without confirming or cancelling");
        ok(ScanService.fullMode, "postponing must not cancel the scan");
        ok(!ScanService.shouldPrompt(true, false), "content noise must not re-show a dismissed dialog");
        ok(ScanService.shouldPrompt(true, true), "a screen change must re-show a dismissed dialog");
        ok(ScanService.shouldPrompt(false, false), "an undismissed dialog is shown on any event");
        long eventNow = 10000L;
        ok(ScanService.eventScanDelay(eventNow, eventNow - 500L, true, true, eventNow + 1400L) == 1400L,
                "repeated accessibility scroll events must not shorten the page-settling delay");
        ok(ScanService.eventScanDelay(eventNow, eventNow, false, true, 0L) == 220L,
                "ordinary scroll events must retain their normal debounce interval");
        long firstDue = 10_500L;
        for (int i = 0; i < 100; i++) {
            firstDue = ScanService.coalescedScanDeadline(firstDue, 10_700L + i * 2L);
        }
        ok(firstDue == 10_500L,
                "a burst of content/scroll events must never postpone the first queued scan deadline");
        ok(ScanService.coalescedScanDeadline(10_500L, 10_300L) == 10_300L,
                "a genuinely earlier requested scan may pull the queued scan forward");
        ScanService.confirmFull();
        ok(ScanService.fullConfirmed, "confirmation should unlock the scan");
        ok(ScanService.FULL_CONFIRM_DELAY_MS <= 300L,
                "confirmation must queue the first scan without a long fixed wait");
        ok(ScanService.FULL_SCROLL_SETTLE_MS <= 400L,
                "automatic scan should use a short settle interval after each scroll gesture");
        ok(ScanService.FULL_SETTLE_RETRY_MS <= 200L,
                "unchanged viewport verification should use a short retry interval");
        ok(!ScanService.fullStartedScanning,
                "confirmation must queue the shared scanner but must not claim extraction started before a target root is observed");
        ScanService.pauseFull();
        ok(ScanService.fullMode && ScanService.fullConfirmed && ScanService.fullPaused,
                "pause must keep the extraction session and confirmation state alive");
        ok(!ScanService.shouldStopFullForWindowChange("app.bastehyab"),
                "leaving the operator while paused must not destroy the preserved session");
        ScanService.resumeFull();
        ok(ScanService.fullMode && ScanService.fullConfirmed && !ScanService.fullPaused,
                "resume must continue the same extraction session");
        ScanService.fullTargetObservedOnce = true;
        ok(ScanService.shouldStopFullForWindowChange("app.bastehyab"),
                "leaving the operator after it was opened must stop the targeted scan");
        ScanService.stopFull("اسکرول متوقف شد؛ پایان فهرست یا محدودیت اپ قابل تأیید نیست");
        ok(!ScanService.fullMode, "stop should close the auto-extraction state");
        ok(!ScanService.fullConfirmed, "stop must reset the confirmation");
        ok(!ScanService.fullFinished, "a stationary page must not be falsely reported as a confirmed end-of-list");
        ScanService.startFull("rightel", "ir.rightel.myrightel");
        ScanService.stopFull("فهرست تمام شد");
        ok(!ScanService.fullFinished, "a completion-like reason string without typed evidence must not finish the scan");
        ScanService.startFull("rightel", "ir.rightel.myrightel");
        ScanService.fullEndState = ListEndDetector.State.END_CONFIRMED;
        ScanService.stopFull("پایان فهرست با شواهد معتبر تأیید شد");
        ok(ScanService.fullFinished, "typed end-of-list evidence should report completion");
        ok(ScanService.fullProgressPercent() == 100, "confirmed completion should display one hundred percent");
        ScanService.restartFullFromFinal();
        ok(ScanService.fullMode && ScanService.fullConfirmed,
                "the final-report new-scan action must start a fresh confirmed session");
        ok(!ScanService.fullFinished && ScanService.fullEndState == ListEndDetector.State.UNKNOWN
                        && ScanService.fullSteps == 0 && ScanService.fullSeen.isEmpty(),
                "a new scan must reset the previous completion proof, progress, and collected signatures");
        ScanService.stopFull("test reset");

        String[][] operators = {
                {"irancell", "com.myirancell"},
                {"mci", "ir.mci.ecareapp"},
                {"rightel", "ir.rightel.myrightel"},
                {"samantel", "com.mysamantel"},
                {"shatel", "com.shmapp"}
        };
        for (String[] operator : operators) {
            ScanService.startFull(operator[0], operator[1]);
            ok(ScanService.fullMode && ScanService.isFullTarget(operator[1]),
                    operator[0] + " must enter the same targeted scan session");
            ok(ScanService.shouldUseAccessibilityScroll(0)
                            && !ScanService.shouldUseAccessibilityScroll(1)
                            && !ScanService.shouldUseAccessibilityScroll(2),
                    operator[0] + " must use the exact same MCI scroll escalation order");
            ok(!ScanService.shouldPreferAppViewportSwipe(1)
                            && ScanService.shouldPreferAppViewportSwipe(2),
                    operator[0] + " must use the exact same viewport gesture fallback");
            ScanService.stopFull("reset shared scan-policy test");
        }

        ScanService.startFull("rightel", "");
        ok(!ScanService.fullMode, "empty package target must not enable broad auto-scrolling");
        ok(!ScanService.isFullTarget("com.android.launcher"), "an empty target must never match every app");

        ok(Fmt.parse("NaN", 12.5) == 12.5, "NaN must not enter numeric settings");
        ok(Fmt.parse("Infinity", 7) == 7, "Infinity must not enter numeric settings");

        android.content.Context testContext = new android.content.Context();
        ok("۱۸۸٬۷۱۶ تومان".equals(Cfg.priceLabel(testContext, 188716L)),
                "default price display should remain Toman and retain the rounded base amount");
        Cfg.setDisplayCurrency(testContext, "IRR");
        ok("۱٬۸۸۷٬۱۶۰ ریال".equals(Cfg.priceLabel(testContext, 188716L)),
                "Rial display must multiply the canonical Toman value by ten exactly once");
        Cfg.setDisplayCurrency(testContext, "IRT");
        ok("۱۸۸٬۷۱۶ تومان".equals(Cfg.priceLabel(testContext, 188716L)),
                "switching back to Toman must not mutate the saved canonical amount");
        Cfg.setInt(testContext, "metric", 1);
        ok(Cfg.metric(testContext) == Cfg.PER_GB,
                "one-time metric migration should preserve the default lowest price ranking for existing installs");
        Cfg.setInt(testContext, "metric", Cfg.DURATION);
        ok(Cfg.metric(testContext) == Cfg.DURATION,
                "the duration ranking tab should remain selected after migration");

        Pkg shortValidity = new Pkg();
        shortValidity.key = "short-validity"; shortValidity.gb = 10; shortValidity.days = 30;
        Pkg longValidity = new Pkg();
        longValidity.key = "long-validity"; longValidity.gb = 10; longValidity.days = 365;
        java.util.Map<String, Double> durationValues = new java.util.HashMap<>();
        durationValues.put(shortValidity.key, Compare.value(shortValidity, Cfg.DURATION, 30, 1000));
        durationValues.put(longValidity.key, Compare.value(longValidity, Cfg.DURATION, 30, 1000));
        java.util.List<Pkg> durationRanked = new java.util.ArrayList<>();
        durationRanked.add(shortValidity); durationRanked.add(longValidity);
        Compare.sortRanked(durationRanked, durationValues);
        ok("long-validity".equals(durationRanked.get(0).key),
                "duration sort must put the longer-validity bundle first");
        ok(Cfg.validPackageName("ir.example.operator"), "valid Android package name should pass");
        ok(!Cfg.validPackageName("ir..operator"), "empty package segments must fail");
        ok(!Cfg.validPackageName("operator"), "single-segment package name must fail");

        // Rank badges and visible row order must use the same metric ordering, even when
        // a restricted offer is cheaper than a general-purpose offer.
        Pkg costlyGeneral = new Pkg();
        costlyGeneral.key = "general"; costlyGeneral.op = "rightel"; costlyGeneral.restricted = false;
        Pkg cheapSpecial = new Pkg();
        cheapSpecial.key = "special"; cheapSpecial.op = "rightel"; cheapSpecial.restricted = true;
        Pkg middle = new Pkg();
        middle.key = "middle"; middle.op = "irancell"; middle.restricted = false;
        java.util.List<Pkg> ranked = new java.util.ArrayList<>();
        ranked.add(costlyGeneral); ranked.add(middle); ranked.add(cheapSpecial);
        java.util.Map<String, Double> values = new java.util.HashMap<>();
        values.put("general", 100.0); values.put("middle", 50.0); values.put("special", 10.0);
        Compare.sortRanked(ranked, values);
        ok("special".equals(ranked.get(0).key) && "middle".equals(ranked.get(1).key)
                        && "general".equals(ranked.get(2).key),
                "row order must exactly follow the comparison value, not a separate special-offer sort");

        Pkg intl = new Pkg();
        intl.why = "بین‌الملل و رومینگ";
        intl.restricted = true;
        ok(Compare.passesSpecialFilters(intl, false, true),
                "intl filter should work independently of domestic special bundles");
        ok(!Compare.passesSpecialFilters(intl, true, false), "intl offers must obey the intl toggle");
        Pkg night = new Pkg();
        night.why = "شبانه";
        night.restricted = true;
        ok(!Compare.passesSpecialFilters(night, false, true),
                "domestic night offers must obey the special-bundles toggle");
        ok(Compare.passesSpecialFilters(night, true, false),
                "domestic special offers must not require the intl toggle");

        android.view.accessibility.AccessibilityNodeInfo root =
                new android.view.accessibility.AccessibilityNodeInfo("root", 0).bounds(0, 0, 1080, 2200);
        android.view.accessibility.AccessibilityNodeInfo carousel =
                new android.view.accessibility.AccessibilityNodeInfo("carousel", 300)
                        .bounds(0, 300, 1080, 560).scrollable(true);
        android.view.accessibility.AccessibilityNodeInfo feed =
                new android.view.accessibility.AccessibilityNodeInfo("feed", 600)
                        .bounds(0, 600, 1080, 2100).scrollable(true);
        root.add(carousel).add(feed);
        ok(ScanService.findScrollable(root) == feed,
                "auto scroll should select the main tall list instead of a horizontal carousel");

        // Irancell now follows the same MCI baseline as every operator: choose the largest
        // visible vertical ScrollView, without inspecting Irancell-specific resource IDs.
        ScanService.startFull("irancell", "com.myirancell");
        android.view.accessibility.AccessibilityNodeInfo irancellRoot =
                new android.view.accessibility.AccessibilityNodeInfo("root", 0)
                        .bounds(0, 0, 1080, 2400).packageName("com.myirancell");
        android.view.accessibility.AccessibilityNodeInfo homeScroll =
                new android.view.accessibility.AccessibilityNodeInfo("home", 0)
                        .bounds(6, 90, 1074, 2385).scrollable(true);
        android.view.accessibility.AccessibilityNodeInfo offerScroll =
                new android.view.accessibility.AccessibilityNodeInfo("offers", 0)
                        .bounds(0, 421, 1080, 2400).scrollable(true);
        android.view.accessibility.AccessibilityNodeInfo offerCard =
                new android.view.accessibility.AccessibilityNodeInfo("بسته اینترنت", 0)
                        .viewId("com.myirancell:id/offerPackage");
        android.view.accessibility.AccessibilityNodeInfo price =
                new android.view.accessibility.AccessibilityNodeInfo("۲۵۰٬۰۰۰ ریال", 0)
                        .viewId("com.myirancell:id/package-price");
        offerCard.add(price);
        offerScroll.add(offerCard);
        // Model the real hierarchy: the offer-specific viewport is nested inside a larger root.
        homeScroll.add(offerScroll);
        irancellRoot.add(homeScroll);
        ok(ScanService.findScrollable(irancellRoot) == homeScroll,
                "Irancell must use the same largest-vertical-list choice as the MCI baseline");

        java.lang.reflect.Method pageFingerprint = ScanService.class.getDeclaredMethod(
                "pageFingerprint", android.view.accessibility.AccessibilityNodeInfo.class);
        pageFingerprint.setAccessible(true);
        android.view.accessibility.AccessibilityNodeInfo fingerprintRootA =
                irancellTree("بسته اینترنت ۱۰ گیگابایت ۳۰ روزه", 0, "به‌روزرسانی ۱۲:۴۵");
        android.view.accessibility.AccessibilityNodeInfo fingerprintRootB =
                irancellTree("بسته اینترنت ۱۰ گیگابایت ۳۰ روزه", 144, "به‌روزرسانی ۱۲:۴۶");
        android.view.accessibility.AccessibilityNodeInfo fingerprintRootC =
                irancellTree("بسته اینترنت ۱۰ گیگابایت ۳۰ روزه", 0, "به‌روزرسانی ۱۲:۴۶");
        ScanService fingerprintService = new ScanService();
        String fullRootFingerprintA = (String) pageFingerprint.invoke(fingerprintService, fingerprintRootA);
        String fullRootFingerprintB = (String) pageFingerprint.invoke(fingerprintService, fingerprintRootB);
        String fullRootFingerprintC = (String) pageFingerprint.invoke(fingerprintService, fingerprintRootC);
        ok(!fullRootFingerprintA.equals(fullRootFingerprintB),
                "the shared viewport fingerprint must detect offer movement by screen position");
        ok(fullRootFingerprintA.equals(fullRootFingerprintC),
                "clock/update labels must not make an unchanged offer viewport look like movement");

        ScanService.startFull("rightel", "ir.rightel.myrightel");
        android.view.accessibility.AccessibilityNodeInfo genericRoot =
                new android.view.accessibility.AccessibilityNodeInfo("root", 0)
                        .bounds(0, 0, 1080, 2400).packageName("ir.rightel.myrightel");
        android.view.accessibility.AccessibilityNodeInfo genericSmall =
                new android.view.accessibility.AccessibilityNodeInfo("small", 0)
                        .bounds(0, 420, 1080, 1700).scrollable(true);
        android.view.accessibility.AccessibilityNodeInfo genericLarge =
                new android.view.accessibility.AccessibilityNodeInfo("large", 0)
                        .bounds(0, 100, 1080, 2300).scrollable(true);
        genericRoot.add(genericSmall).add(genericLarge);
        ok(ScanService.findScrollable(genericRoot) == genericLarge,
                "non-Irancell operators should retain the generic largest-vertical-list fallback");

        for (String[] operator : operators) {
            android.view.accessibility.AccessibilityNodeInfo sharedRoot =
                    new android.view.accessibility.AccessibilityNodeInfo("root", 0)
                            .bounds(0, 0, 1080, 2400).packageName(operator[1]);
            android.view.accessibility.AccessibilityNodeInfo horizontalStrip =
                    new android.view.accessibility.AccessibilityNodeInfo("carousel", 0)
                            .bounds(0, 200, 1080, 470).scrollable(true);
            android.view.accessibility.AccessibilityNodeInfo verticalFeed =
                    new android.view.accessibility.AccessibilityNodeInfo("feed", 0)
                            .bounds(0, 430, 1080, 2360).scrollable(true);
            sharedRoot.add(horizontalStrip).add(verticalFeed);
            ok(ScanService.findScrollable(sharedRoot) == verticalFeed,
                    operator[0] + " must select the same main vertical feed as the MCI reference path");
        }
        System.out.println("PASS: targeted auto extraction, operator-neutral MCI scroll order, same vertical-feed selection for all five operators, target validation, generic vertical-list selection and stable offer-viewport fingerprinting, metric-consistent ranking, duration sorting, Toman/Rial display conversion, finite numeric parsing, package validation, and independent special/intl filters.");
    }
}
