# =============================================================================
# Blue View Weather — R8 rules (release is minified with R8 FULL MODE, the AGP 8
# default). Full mode is more aggressive than legacy ProGuard: it strips generic
# signatures from classes that are not explicitly kept, obfuscates enum constant
# names, and assumes anything unreferenced is dead. Every rule below exists to
# close one specific hole that opens under those assumptions.
#
# Deliberately absent: Compose and Material3 rules. Both ship consumer rules in
# their AARs, neither uses reflection over app classes, and the Compose compiler
# emits direct calls only — adding keeps for them would just disable shrinking of
# the largest dependency in the app.
# =============================================================================


# -----------------------------------------------------------------------------
# Attributes
# -----------------------------------------------------------------------------

# Signature/InnerClasses/EnclosingMethod: Retrofit reads the *generic* return type
# of each service method (Call<ForecastDto>, or the suspend Continuation) to pick a
# converter. Without Signature it sees a raw Call and throws at interface-creation
# time. InnerClasses+EnclosingMethod keep nested and anonymous types resolvable,
# which Signature entries reference.
-keepattributes Signature, InnerClasses, EnclosingMethod

# Retrofit resolves @GET/@Query/@Path at runtime from these annotation tables, and
# kotlinx-serialization looks up @Serializable/@SerialName the same way.
# AnnotationDefault carries annotation default values (e.g. an omitted @Query name).
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# Play Console crash reports are currently unreadable ("R1.d.g" with no line
# numbers). Keeping these two attributes puts file+line back in every frame; the
# -renamesourcefileattribute below then rewrites the source file name to a constant
# so the original .kt filenames are not leaked in a shipped APK. The mapping.txt
# uploaded to Play deobfuscates the rest.
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile


# -----------------------------------------------------------------------------
# kotlinx.serialization
# -----------------------------------------------------------------------------
# These are the official R8-full-mode rules. Note they are scoped by the
# @Serializable *annotation*, not by a package glob: the previous rules file kept
# only ai.blueview.weather.data.api.dto.**, which silently missed the @Serializable
# types in data/preferences (SavedCity), data/location, data/radar and data/update.
# An annotation predicate covers all of them and cannot go stale when a new DTO
# lands in a new package.

# The compiler plugin puts the entry point — Companion.serializer() — on a
# synthetic Companion object that nothing in app code references directly, so R8
# treats it as dead. `public` is deliberately omitted from the -keepclassmembers
# line: the DTOs in IpGeolocation.kt and RadarRepository.kt are `private` at the
# Kotlin level and compile down to package-private classes, which the stock
# `public class <1>` form would not match.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Serializer lookup for nested/companion serializers reached via the outer class.
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# @Serializable object singletons: the INSTANCE field is how the generated
# serializer materialises the value, and it is only ever read reflectively.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# The generated <Class>$$serializer holds the SerialDescriptor plus the synthetic
# serialize/deserialize/childSerializers members. It is instantiated only from the
# Companion via reflection, so R8 sees zero references and would strip its members.
-if @kotlinx.serialization.Serializable class **
-keep class <1>$$serializer { *; }


# -----------------------------------------------------------------------------
# Enums persisted by constant name
# -----------------------------------------------------------------------------
# CRITICAL. LocationMode (data/preferences/UserPreferences.kt) is written to
# DataStore as `mode.name` and read back with `entries.firstOrNull { it.name == raw }`.
# R8 full mode renames enum constant fields *and* rewrites the name string handed to
# the Enum constructor, so after minification `.name` yields "a"/"b". A user who
# pinned a city would then read a stored "PINNED" that matches nothing and fall
# through to the AUTOMATIC default — a silent data-loss regression on upgrade, with
# no crash to catch it. Keeping <fields> pins the constants themselves; values() and
# valueOf() are kept because they are synthesised and otherwise unreferenced.
-keepclassmembers enum ai.blueview.weather.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}


# -----------------------------------------------------------------------------
# Retrofit
# -----------------------------------------------------------------------------
# Replaces the previous `-keep class retrofit2.** { *; }`, which kept every class
# and member in the library and so disabled shrinking of the whole package. These
# are Retrofit's own published R8 rules.

# R8 full mode strips generic signatures from types that are not kept. Retrofit
# reads Call<T>'s and Response<T>'s type argument to choose a converter, and reads
# Continuation<T> for suspend service methods, so these three need their signatures
# intact even though the classes themselves may still be shrunk and renamed.
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Service interfaces are implemented by a runtime Proxy, so nothing statically
# references their methods. Keep any interface carrying @GET/@Query/etc. and its
# methods (renaming is fine — the HTTP details come from the annotations).
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-keepclasseswithmembers,allowobfuscation,allowshrinking interface * {
    @retrofit2.http.* <methods>;
}

# And keep the generic return type of those methods resolvable for the same reason
# as the Call/Response rule above.
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>


# -----------------------------------------------------------------------------
# -dontwarn: specific missing classes only
# -----------------------------------------------------------------------------
# Replaces the blanket `-dontwarn okhttp3.**` / `-dontwarn retrofit2.**`. A blanket
# dontwarn over a whole package also swallows warnings about classes those libraries
# genuinely need but that are actually absent from this build — exactly the errors
# worth seeing. Each line below names a compile-only or optional-at-runtime
# dependency that is legitimately not on the classpath.

# Animal Sniffer build-tooling annotation, referenced by okhttp/okio/retrofit.
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# JSR-305 nullability annotations; compile-time only.
-dontwarn javax.annotation.**

# Optional TLS providers okhttp probes for reflectively and works fine without.
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn org.bouncycastle.jsse.**

# okhttp/okio guard these behind try/catch on desugared or older runtimes.
-dontwarn java.lang.invoke.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# Dagger's generated code references Error Prone annotations that are compile-only.
-dontwarn com.google.errorprone.annotations.**


# -----------------------------------------------------------------------------
# Hilt / Dagger
# -----------------------------------------------------------------------------
# Hilt's ViewModel factory keys its generated provider map on the ViewModel's
# fully-qualified *name string* (@StringKey literals in the generated module) and
# looks it up with modelClass.getName() at runtime. Renaming the class breaks that
# match. -keepnames (not -keep) is used on purpose: names are pinned, but an unused
# ViewModel can still be shrunk away.
-keepnames @dagger.hilt.android.lifecycle.HiltViewModel class *

# Hilt's generated Hilt_* base classes and @InstallIn modules are wired up by direct
# references from generated code, so they need no keeps — the Hilt Gradle plugin's
# consumer rules cover the rest.


# -----------------------------------------------------------------------------
# androidx.webkit
# -----------------------------------------------------------------------------
# RadarWebView serves the bundled Leaflet radar through WebViewAssetLoader, which
# reaches the system WebView provider across an APK boundary via reflectively
# resolved org.chromium.support_lib_boundary interfaces. Those live in the WebView
# APK, not ours: keep our side of the contract and silence warnings about theirs.
# (There is no @JavascriptInterface bridge in this app, so no rule is needed for one.)
-keep interface org.chromium.support_lib_boundary.** { *; }
-dontwarn org.chromium.support_lib_boundary.**
