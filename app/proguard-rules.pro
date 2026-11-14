# Hishab release rules. Room/Compose/serialization ship consumer rules.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep enum names used by persisted strings and backup JSON.
-keepclassmembers enum com.hishab.app.** { *; }
