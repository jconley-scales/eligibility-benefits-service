-- btree_gist lets a GiST exclusion constraint compare plain equality columns
-- (such as a member ID) alongside date ranges. The coverage table needs it to
-- reject overlapping Coverages for the same Member.
CREATE EXTENSION IF NOT EXISTS btree_gist;
