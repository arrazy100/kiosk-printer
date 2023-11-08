# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature,*Annotation*,EnclosingMethod

-keep public class com.lunapos.kioskprinter.dtos.PrinterBody, com.lunapos.kioskprinter.dtos.PrinterBody.**,
    com.lunapos.kioskprinter.singletons.PrinterData, com.lunapos.kioskprinter.singletons.PrinterData.** {
    private <fields>;
    public void set*(***);
    public *** get*();
}

#to preserve Jackson annotations like @JsonIgnore
-keepclassmembers class * {
    @com.fasterxml.jackson.annotation.* *;
}

#dont throw warnings from here
-dontwarn com.fasterxml.jackson.databind.**