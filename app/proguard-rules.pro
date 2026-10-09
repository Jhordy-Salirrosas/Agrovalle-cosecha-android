# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Navigation Component Safe Args
-keep class com.dapm.android.agrovalle.**Args { *; }
-keep class com.dapm.android.agrovalle.**Directions { *; }
