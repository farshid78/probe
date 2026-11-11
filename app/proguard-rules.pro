# Manifest-declared entry points and framework callbacks.
-keep class app.bastehyab.ScanService { *; }
-keep class app.bastehyab.MainActivity { *; }
# ViewModel instances are created reflectively by ViewModelProvider.
-keep class * extends androidx.lifecycle.ViewModel { <init>(); }
-keep class * extends androidx.recyclerview.widget.RecyclerView$Adapter { *; }
-keep class app.bastehyab.provider.** { *; }
-keep class app.bastehyab.domain.** { *; }
-keep class app.bastehyab.scan.** { *; }
-keep class app.bastehyab.diagnostics.** { *; }
# No reflection beyond the framework contracts above.
-dontwarn org.json.**
