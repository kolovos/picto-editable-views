# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Eclipse Epsilon is a family of scripting languages and tools for model-based software engineering. This repository focuses on **Picto editable views** - enabling users to edit model properties directly from Picto visualizations.

### Project Goal

Implement a `trace()` operation in EGL templates that:
1. Marks generated output with invisible traceability markers
2. Shows a toolbar on hover allowing users to edit the traced value
3. Propagates changes back to the source model with undo/redo support

## Current Progress

### Completed (PRs #9-16)

| PR | Branch | Description |
|----|--------|-------------|
| #9 | `pr/foundation` | Basic trace toolbar infrastructure, renamed watermark→trace |
| #10 | `pr/issue-7` | Issue #7: Research on diagram-to-source navigation (Kroki ZWC compatibility) |
| #11 | `pr/issue-3` | Issue #3: Support multiple trace() calls within same HTML/SVG element |
| #12 | `pr/issue-5` | Issue #5: Base-5 ZWC encoding (efficiency: ID 100 = 3 chars instead of 100) |
| #13 | `pr/issue-2` | Issue #2: Extension point for custom toolbar actions |
| #14 | `pr/issue-8` | Architectural: Java callback for trace detection (simplified architecture) |
| #16 | `pr/issue-1` | Issue #8: Multi-line string tracing (recursive text collection, parent search)

**Note:** PR #14 branch is named `pr/issue-8` but the work is architectural cleanup, not issue #8 (multi-line strings).

### Recently Implemented (not yet in PR)

- **Issue #1**: Multiple versions of trace() - `trace(value)`, `trace(value, actions)`, `trace(value, element, actions)`

### Remaining Work

| Issue | Title | Status |
|-------|-------|--------|
| #1 | Multiple versions of trace() | Implemented, needs PR |
| #6 | Test ZWC with more diagram tools | **In Progress**: Initial research complete, open questions remain |

### Issue #6: ZWC Compatibility Research

**See:** [`docs/zwc-compatibility-research.md`](docs/zwc-compatibility-research.md) for full details.

**Initial testing complete** via `scripts/kroki_zwc_compatibility.py`. Key findings:
- **Works:** Graphviz, all *diag tools, d2, ditaa, nomnoml, pikchr, svgbob, wavedrom, wireviz
- **Doesn't work:** PlantUML (all types), c4plantuml, dbml, structurizr, symbolator, umlet
- **Failed to run:** Mermaid, bpmn, erd, excalidraw, vega, vegalite

**Open questions requiring investigation:**
1. **PlantUML incompatibility** - Is there a workaround? (Needs investigation by maintainer)
2. **Mermaid test failures** - API issue or fundamental incompatibility? (Needs investigation)
3. **ZWC duplication** - All compatible tools duplicate ZWCs in title+text fields. Does current code handle this? (Needs code investigation)

## Build Commands

```bash
# Full build (requires the uber-JAR to be installed first)
mvn -B -N -f pom-plain.xml install
mvn -B -pl releng/org.eclipse.epsilon.jena.uberjar -f pom-plain.xml clean install
mvn -B clean install

# Build only the Picto plugin
mvn -B -pl plugins/org.eclipse.epsilon.picto clean install

# Run Picto tests
mvn -B -f tests/org.eclipse.epsilon.picto.test verify
```

## Architecture

### How trace() Works

1. **EGL Template** calls `trace()` with one of three signatures:
   - `trace(c.name)` - trace value, all toolbar actions
   - `trace(c.name, Sequence{"show", "edit"})` - trace value, specific actions only
   - `trace(a.type.name, a.type, Sequence{"show"})` - trace value from one element, actions target different element
2. **TraceOperationFactory** records the (element, property) pair and returns the value with invisible ZWC markers encoded in base-5 (`\u2060`-`\u2064`)
3. **TracedTextWrapperTransformer** (optional) can wrap traced content in span/tspan elements
4. **TraceToolbarAppender** injects JS/CSS for the toolbar
5. **Browser JS** detects ZWC on hover via `GetTraceFromTextFunction` callback
6. **Toolbar actions** call Java functions to edit values or navigate to source

### Key Files

**Trace subsystem** (`plugins/org.eclipse.epsilon.picto/src/org/eclipse/epsilon/picto/trace/`):
- `TraceManager.java` - Maps trace IDs to (element, property) pairs
- `TraceOperationFactory.java` - Creates the `trace()` EOL operation
- `TraceToolbarAction.java` - Base class for toolbar actions
- `TraceToolbarActionExtensionPointManager.java` - Loads actions from extension point
- `GetTraceFromTextFunction.java` - Extracts trace ID from text with ZWC markers
- `picto-trace-toolbar.js` - Browser-side toolbar logic

**Extension point** (`plugin.xml`):
- `traceToolbarAction` - Register custom toolbar actions (schema: `schema/traceToolbarAction.exsd`)

**Tests** (`tests/org.eclipse.epsilon.picto.test/`):
- `TraceManagerTests.java` - Base-5 encoding/decoding
- `TraceToolbarActionTests.java` - Extension point loading
- `GetTraceFromTextFunctionTests.java` - ZWC detection

## Development Notes

- Java 17 required
- EMF editing framework used for model changes (requires `IEditingDomainProvider`)
- Stacked PR workflow: `pr/` branch prefix, merge in dependency order, rebase downstream after each merge
