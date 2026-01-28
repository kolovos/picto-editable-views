# Development Log: Kroki Zero-Width Character Compatibility Testing

This document captures the development conversation and intent behind the scripts in this directory.

## Purpose

Test whether zero-width characters (ZWCs) used for traceability in Picto cause issues with diagram-as-code tools via the Kroki.io API.

## Background

Picto uses zero-width Unicode characters (`\u2060`, `\u2061`, `\u2064`) as invisible markers appended to traced model element properties. These markers enable the trace toolbar to identify which model elements correspond to rendered text in diagrams.

---

## Conversation Log

### 1. Initial Plan Implementation

**Prompt:** Implement the plan for a Python script to test ZWC compatibility with Kroki.io

**Intent:** Create a test script that:
- Tests 3 zero-width characters: `\u2060` (Word Joiner), `\u2061` (Function Application), `\u2064` (Invisible Plus)
- Tests character counts: 1, 10, 100, 1000
- Tests multiple diagram types via Kroki.io API
- Compares rendered output with/without ZWCs using image similarity

**Result:** Created initial `test_zero_width_chars.py` and `requirements.txt`

---

### 2. Expanding Diagram Coverage

**Prompt:** Does this test all tools supported by Kroki? And are all generated diagrams stored to PNG during a test run and kept for inspection later?

**Intent:** Ensure comprehensive testing and artifact preservation

**Actions taken:**
- Added all 28 Kroki-supported diagram types (was only testing 11)
- Added PNG/SVG file saving to timestamped output directories
- Structure: `test_output/<timestamp>/<diagram_type>/baseline.png`, `U2060_x1.png`, etc.

---

### 3. Code Organization

**Prompt:** Can you just put the diagrams constants in a separate file, to keep this script more readable?

**Intent:** Improve code maintainability

**Result:** Extracted diagram definitions to `diagrams.py`

---

### 4. First Test Run - Many Failures

**Prompt:** Run the tests

**Result:** Many HTTP 400 errors and visual differences

**Key findings:**
- PlantUML: HTTP 400 syntax errors
- blockdiag: Empty responses
- seqdiag/actdiag: 64-71% similarity (image size changes)
- ERD, DBML, WireViz: HTTP 400 errors

---

### 5. Investigating HTTP 400 Errors

**Prompt:** Why the HTTP 400 errors?

**Investigation revealed:**
- ZWCs in **identifiers** break parsers (strict `[a-zA-Z0-9_]` rules)
- ZWCs in **text/label content** work fine
- Example: `Alice⁠ -> Bob` fails, but `Alice -> Bob: Hello⁠` works

**Root cause:** Original test placed ZWCs after syntax identifiers, not in label text

---

### 6. Fixing ZWC Placement

**Prompt:** Update the script so that the ZWC are only appended after identifiers. The reason is that we are trying to use them for traceability using the trace function in Picto

**Intent:** Match real-world Picto usage where traced values appear as labels/text

**Actions taken:**
- Rewrote all diagram definitions in `diagrams.py`
- Changed target elements from identifiers to label/text content
- Added comments explaining the traceability pattern
- Examples:
  - PlantUML: Target message text (`"Authentication Request"`) not participant names
  - GraphViz: Target label attributes (`label="Hello World"`)
  - Mermaid: Target message text and node labels

---

### 7. Removing Problematic Diagram Types

**Prompt:** Remove tikz from the tools to test

**Intent:** TikZ requires LaTeX setup and was consistently failing

**Result:** Removed TikZ from `diagrams.py`

---

### 8. Adding CSV Output

**Prompt:** Add another output file (in CSV format) that contains a table with columns: tool, diagram type, type of ZWC, number of ZWCs, test could be run yes/no, percentage matching

**Intent:** Easier analysis and reporting of results

**Result:** Added `save_csv_results()` function, outputs to `test_results.csv`

---

### 9. Code Review

**Prompt:** Critically inspect the test_zero_width_chars script and report any doubts you have about its correctness

**Issues identified:**

**High Priority:**
1. No validation that target element exists in source - could produce false positives
2. Deprecated PIL `getdata()` API - will break in Pillow 14
3. Baseline failures produced malformed results with `zero_width_char: 'baseline'`

**Medium Priority:**
4. Pixel-exact comparison may be too strict
5. White background assumption when padding different-sized images
6. `repr(char)[1:-1]` is fragile for Unicode representation

---

### 10. Fixing High Priority Issues

**Prompt:** Fix high-prio issues

**Changes made:**

1. **Element validation:**
   ```python
   def inject_zero_width_chars(...) -> tuple[str, bool]:
       if element not in source:
           return source, False
       ...
       return modified, True
   ```

2. **PIL API fix:**
   ```python
   # Old (deprecated)
   pixels1 = list(img1.getdata())

   # New
   bytes1 = img1.tobytes()
   # Compare in 4-byte RGBA chunks
   ```

3. **Baseline error handling:**
   - Now returns one result per ZWC/count combination
   - Each has proper `char_name`, `zero_width_char`, `char_count`
   - Added `is_baseline_error: True` flag

**Bug fix:** `result` variable was used before definition when injection failed

---

### 11. Renaming Main Script

**Prompt:** Rename test_zero_width to something better, because it is not a testcase itself and it's confusing that the other things are testing that script

**Result:** Renamed `test_zero_width_chars.py` → `kroki_zwc_compatibility.py`

Updated imports in `test_kroki_api.py`

---

### 12. Running Tests

**Prompt:** Run the test cases

**Result:** All tests pass
- `test_kroki_standalone.py`: 5/5 passed
- `test_kroki_api.py`: 34/34 passed (pytest)

---

### 13. Running Compatibility Check

**Prompt:** Run the compatibility check

**Results after fixes:**
| Metric | Before | After |
|--------|--------|-------|
| Rendered OK | 217 (52%) | 396 (97%) |
| Visual matches | 163 (39%) | 369 (90%) |
| Errors | 203 | 12 |

**Key improvement:** Placing ZWCs in labels instead of identifiers fixed most issues

---

### 14. Comparing Results

**Prompt:** How do these results compare to the previous test run

**Summary:** Error rate dropped from 48% to 3% after fixing ZWC placement

**Now working:** PlantUML, blockdiag, seqdiag, actdiag, BPMN, DBML, structurizr, wireviz, bytefield

**Still problematic:** erd (strict parser), nomnoml/pikchr/wavedrom (render ZWCs visibly)

---

### 15. Edge Case Discussion

**Prompt:** I just realized a corner case. If an identifier occurs multiple times in the diagram, and is only changed once, then that will be seen as a completely different identifier the next run. Is this ever a problem in any of our cases?

**Analysis:** Checked all diagrams - no duplicates found in current test cases

**Potential real-world issue:**
- If "Alice" appears multiple times, only first gets ZWC
- Could cause identifier mismatch in some tools
- Not a problem for Picto's use case (traces are on rendered labels, usually unique)

---

## File Structure

```
scripts/
├── kroki_zwc_compatibility.py  # Main compatibility checker
├── diagrams.py                  # Diagram definitions (source + target element)
├── test_kroki_api.py           # Pytest tests for API functions
├── test_kroki_standalone.py    # Standalone tests (no deps beyond requests)
├── requirements.txt            # Python dependencies
├── DEVELOPMENT_LOG.md          # This file
└── test_output/                # Generated test artifacts
    └── <timestamp>/
        ├── test_results.json
        ├── test_results.csv
        └── <diagram_type>/
            ├── baseline.png
            ├── baseline.svg
            ├── U2060_x1.png
            └── ...
```

## Key Learnings

1. **ZWC placement matters:** Put ZWCs in text/label content, not syntax identifiers
2. **Parser strictness varies:** Some tools (ERD, old PlantUML identifiers) have strict character rules
3. **Visual rendering varies:** Some tools (nomnoml, pikchr) render ZWCs as visible characters
4. **Test validation is critical:** Must verify injection actually happened to avoid false positives

## Running the Scripts

```bash
# Install dependencies
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt

# Run compatibility check
.venv/bin/python kroki_zwc_compatibility.py

# Run test cases
python3 test_kroki_standalone.py
.venv/bin/pip install pytest
.venv/bin/python -m pytest test_kroki_api.py -v
```
