# Wear track (deferred)

Scenarios UE-R01–R03 / ACC-R01 are catalogued but not automated while Wear is
stripped from the 1.0 tree.

When the `:wear` module returns:

1. Restore L1 sync / idempotent quick-log tests.
2. Add L4 tile + quick-log device evidence.
3. Flip catalogue Status from Deferred → Active.
4. Keep CI green until a Wear system image is part of the release gate.
