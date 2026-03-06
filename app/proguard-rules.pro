# Add project specific ProGuard rules here.

# ── Debugging (keep source/line info in stack traces) ────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Kotlin ────────────────────────────────────────────────────────────────────
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlin.Lazy { *; }

# ── Kotlin Coroutines ─────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# ── Firebase / Firestore ──────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keepattributes *Annotation*
-keepattributes Signature

# ── Wing Zone – Data Models ───────────────────────────────────────────────────
# Keep all data classes and enums in the models package so Firestore
# serialization (hashMapOf) and enum valueOf() lookups keep working.
-keep class wingzone.zenith.data.models.** { *; }
-keepclassmembers enum wingzone.zenith.data.models.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public java.lang.String name();
    <fields>;
}

# ── Wing Zone – Repositories ──────────────────────────────────────────────────
-keep class wingzone.zenith.data.repository.** { *; }

# ── Wing Zone – ViewModels ────────────────────────────────────────────────────
-keep class wingzone.zenith.viewmodel.** { *; }

# ── Wing Zone – Utils (PendingOrderManager etc.) ──────────────────────────────
-keep class wingzone.zenith.utils.** { *; }

# ── WebView JS interface ──────────────────────────────────────────────────────
# (ToyyibPay WebView – keep any @JavascriptInterface methods)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ── Prevent stripping of Serializable classes ─────────────────────────────────
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}