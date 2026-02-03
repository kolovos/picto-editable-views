# Zero-Width Character Compatibility Research

This document summarizes research on ZWC (Zero-Width Character) compatibility with diagram-as-code tools via Kroki.

## Background

The trace() operation in Picto uses ZWCs (U+2060 through U+2064) as invisible markers to track traced values in generated output. For this to work with diagram tools, the ZWCs must survive the diagram rendering pipeline.

## Test Methodology

Testing was performed using `scripts/kroki_zwc_compatibility.py` which:
1. Injects ZWCs into diagram source at specific locations
2. Sends to Kroki API for rendering
3. Checks if ZWCs are preserved in the SVG output
4. Verifies ZWCs appear at the correct location (adjacent to the traced element)

Test results are stored in `scripts/test_results.csv` and `scripts/test_results.json`.

## Results Summary

### ZWCs Preserved (Compatible)

| Tool | Notes |
|------|-------|
| graphviz | Works, ZWCs duplicated in title+text fields |
| blockdiag | Works, ZWCs duplicated |
| seqdiag | Works, ZWCs duplicated |
| actdiag | Works, ZWCs duplicated |
| nwdiag | Works, ZWCs duplicated |
| packetdiag | Works, ZWCs duplicated |
| rackdiag | Works, ZWCs duplicated |
| bytefield | Works, ZWCs duplicated |
| d2 | Works, ZWCs duplicated |
| ditaa | Works, ZWCs duplicated |
| nomnoml | Works, ZWCs duplicated |
| pikchr | Works, ZWCs duplicated |
| svgbob | Works, ZWCs duplicated |
| wavedrom | Works, ZWCs duplicated |
| wireviz | Works, ZWCs duplicated |

### ZWCs Stripped (Incompatible)

| Tool | Notes |
|------|-------|
| **plantuml** | All diagram types fail (activity, class, component, object, sequence, usecase) |
| **c4plantuml** | ZWCs stripped |
| dbml | ZWCs stripped |
| structurizr | ZWCs stripped |
| symbolator | ZWCs stripped |
| umlet | ZWCs stripped |

### Tests Failed to Run

| Tool | Notes |
|------|-------|
| **mermaid** | flowchart, gantt, sequence - all failed to run |
| bpmn | Failed |
| erd | Failed |
| excalidraw | Failed |
| vega | Failed |
| vegalite | Failed |

## Open Questions

### 1. PlantUML Incompatibility
PlantUML is widely used but strips ZWCs. Needs investigation:
- Is there a workaround?
- Can ZWCs be placed in specific locations that survive?
- Alternative approach for PlantUML diagrams?

### 2. Mermaid Test Failures
Mermaid tests failed to run. Needs investigation:
- Was this a Kroki API issue or fundamental incompatibility?
- Can we test Mermaid directly without Kroki?

### 3. ZWC Duplication
All compatible tools show ZWCs duplicated (2x) - appearing in both `<title>` and `<text>` SVG elements. Needs investigation:
- Does the current trace detection code handle this correctly?
- Could duplication cause false positives or other issues?

## Running the Tests

```bash
cd scripts
source .venv/bin/activate
python kroki_zwc_compatibility.py
```

Requires a local Kroki instance or internet access to kroki.io.

## Related Files

- `scripts/kroki_zwc_compatibility.py` - Main test script
- `scripts/diagrams.py` - Diagram definitions for Kroki tools
- `scripts/test_zwc_preservation.py` - Unit tests for ZWC detection
- `scripts/test_results.csv` - Test results (CSV format)
- `scripts/test_results.json` - Test results (JSON format)
