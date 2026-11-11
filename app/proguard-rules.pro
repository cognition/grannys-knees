# Project-specific R8 rules for release shrinking.
# Library consumer rules (Compose, Room, Navigation, etc.) are applied automatically.

# Keep line numbers for clearer Play Console stack traces when mapping is present.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
