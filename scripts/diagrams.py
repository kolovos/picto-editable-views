"""
Diagram definitions for Kroki.io testing.

Each diagram type maps to a dict of subtypes, where each subtype is a tuple of:
  (source_code, element_name_to_modify)

The element_name is the text AFTER which zero-width characters will be appended.
For traceability purposes, we target label/text content rather than syntax identifiers,
since ZWCs in identifiers break many parsers.

Traceability pattern:
  - Model element properties are traced and appear as text/labels in diagrams
  - ZWCs are appended after the traced text to mark the boundary
  - This should not affect diagram parsing or rendering
"""

# Diagram definitions organized by how they handle text:
# - "label": ZWC goes in a label/text string (safest)
# - "identifier": ZWC goes after a bare identifier (may break some parsers)

DIAGRAMS = {
    # === BLOCK DIAGRAM FAMILY ===
    # These use quoted labels, so ZWC in labels should be safe
    'blockdiag': {
        'default': (
            '''blockdiag {
  blockdiag -> generates -> "block-diagrams";
  blockdiag -> is -> "very easy!";

  blockdiag [label = "BlockDiag"];
  "block-diagrams" [color = "pink"];
  "very easy!" [color = "orange"];
}''',
            'BlockDiag'  # In quoted label attribute
        )
    },
    'seqdiag': {
        'default': (
            '''seqdiag {
  browser -> webserver [label = "GET /index.html"];
  browser <-- webserver [label = "Response"];
  browser -> webserver [label = "POST /blog/comment"];
  browser <-- webserver;
}''',
            'GET /index.html'  # In quoted label
        )
    },
    'actdiag': {
        'default': (
            '''actdiag {
  write -> convert -> image

  lane user {
    label = "User"
    write [label = "Writing reST"];
    image [label = "Get diagram IMAGE"];
  }
  lane Kroki {
    convert [label = "Convert reST to Image"];
  }
}''',
            'Writing reST'  # In quoted label
        )
    },
    'nwdiag': {
        'default': (
            '''nwdiag {
  network dmz {
    address = "210.x.x.x/24"
    web01 [address = "210.x.x.1", label = "Web Server 01"];
    web02 [address = "210.x.x.2"];
  }
  network internal {
    address = "172.x.x.x/24";
    web01 [address = "172.x.x.1"];
    db01;
  }
}''',
            'Web Server 01'  # In quoted label
        )
    },
    'packetdiag': {
        'default': (
            '''packetdiag {
  colwidth = 32;
  node_height = 72;

  0-15: Source Port;
  16-31: Destination Port;
  32-63: Sequence Number;
  64-95: Acknowledgment Number;
}''',
            'Source Port'  # Label text (unquoted but treated as text)
        )
    },
    'rackdiag': {
        'default': (
            '''rackdiag {
  16U;
  1: UPS [2U];
  3: DB Server;
  4: Web Server;
  5: Web Server;
  6: Web Server;
  7: Load Balancer;
  8: L3 Switch;
}''',
            'DB Server'  # Label text
        )
    },

    # === GRAPHVIZ ===
    # Supports ZWC in labels (quoted strings)
    'graphviz': {
        'default': (
            '''digraph G {
    Hello [label="Hello World"]
    Goodbye [label="Goodbye World"]
    Hello -> Goodbye
}''',
            'Hello World'  # In quoted label
        )
    },

    # === PLANTUML FAMILY ===
    # ZWC must be in message text or notes, NOT in participant names
    'plantuml': {
        'usecase': (
            '''@startuml
left to right direction
actor "Customer" as customer
actor "Clerk" as clerk
rectangle checkout {
  usecase "Checkout Process" as checkout_uc
  usecase "Payment" as payment
  usecase "Help" as help
  customer -- checkout_uc
  checkout_uc .> payment : include
  help .> checkout_uc : extends
  checkout_uc -- clerk
}
@enduml''',
            'Checkout Process'  # In quoted usecase name
        ),
        'class': (
            '''@startuml
class Student {
  String name
  int age
}
class Course {
  String title
}
Student "0..*" - "1..*" Course : enrolls in
note right of Student : "Student Entity"
@enduml''',
            'Student Entity'  # In quoted note
        ),
        'sequence': (
            '''@startuml
participant Alice
participant Bob
Alice -> Bob: Authentication Request
Bob --> Alice: Authentication Response
note right: "Successful auth"
@enduml''',
            'Authentication Request'  # In message text (not quoted, but treated as text)
        ),
        'object': (
            '''@startuml
object "First Object" as first
object "Second Object" as second
first : name = "Object One"
second : name = "Object Two"
@enduml''',
            'Object One'  # In quoted attribute value
        ),
        'activity': (
            '''@startuml
start
:Hello world;
:Process the data;
:Save results;
stop
@enduml''',
            'Process the data'  # Activity label (between : and ;)
        ),
        'component': (
            '''@startuml
package "Some Group" {
  [First Component] as fc
  [Another Component] as ac
}
note right of fc : "Main component"
@enduml''',
            'Main component'  # In quoted note
        ),
    },
    'c4plantuml': {
        'default': (
            '''@startuml
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Container.puml

Person(user, "End User", "A user of the system")
Container(webapp, "Web Application", "Java", "Delivers content")
System(backend, "Backend System", "Processes requests")

Rel(user, webapp, "Uses", "HTTPS")
@enduml''',
            'End User'  # In quoted person description
        )
    },

    # === MERMAID ===
    # Supports ZWC in labels (quoted or after identifiers)
    'mermaid': {
        'sequence': (
            '''sequenceDiagram
    participant Alice
    participant John
    Alice->>John: Hello John, how are you?
    John-->>Alice: Great thanks!
    Alice-)John: See you later!''',
            'Hello John, how are you?'  # Message text
        ),
        'gantt': (
            '''gantt
    title A Gantt Diagram
    dateFormat YYYY-MM-DD
    section Section
        First task          :a1, 2014-01-01, 30d
        Another task    :after a1, 20d
    section Another
        Task in Another :2014-01-12, 12d
        Final task    :24d''',
            'First task'  # Task label
        ),
        'flowchart': (
            '''flowchart TD
    A[Start Process] --> B{Decision Point}
    B -->|Yes| C[OK Result]
    B -->|No| D[Cancel Operation]
    C --> E[End Process]
    D --> E''',
            'Start Process'  # Node label in brackets
        ),
    },

    # === D2 ===
    # Modern diagram language, handles unicode well
    'd2': {
        'default': (
            '''direction: right
x: Cloud Service {
  shape: cloud
}
y: Database Storage {
  shape: cylinder
}
x -> y: "Data Transfer"''',
            'Data Transfer'  # In quoted label
        )
    },

    # === NOMNOML ===
    # UML-style, labels in brackets
    'nomnoml': {
        'default': (
            '''[Pirate|eyeCount: Int|raid();pillage()|
  [beard]--[parrot]
  [beard]-:>[foul mouth]
]

[<abstract>Marauder|plpilferage: String]<:--[Pirate]''',
            'pilferage'  # Attribute name
        )
    },

    # === PIKCHR ===
    # PIC-like language, text in quotes
    'pikchr': {
        'default': (
            '''arrow right 200% "Markdown Source" above
box rad 10px "Markdown" "Formatter" "(markdown.c)" fit
arrow right 200% "HTML Output" above
arrow <-> down 70% from last box.s
box same "Pikchr" "Formatter" "(pikchr.c)" fit''',
            'Markdown Source'  # In quoted label
        )
    },

    # === VEGA / VEGALITE ===
    # JSON-based, text in string values
    'vega': {
        'default': (
            '''{
  "$schema": "https://vega.github.io/schema/vega/v5.json",
  "width": 400,
  "height": 200,
  "padding": 5,
  "data": [
    {
      "name": "table",
      "values": [
        {"category": "Alpha Category", "amount": 28},
        {"category": "Beta Category", "amount": 55},
        {"category": "Gamma Category", "amount": 43}
      ]
    }
  ],
  "scales": [
    {
      "name": "xscale",
      "type": "band",
      "domain": {"data": "table", "field": "category"},
      "range": "width",
      "padding": 0.05
    },
    {
      "name": "yscale",
      "domain": {"data": "table", "field": "amount"},
      "nice": true,
      "range": "height"
    }
  ],
  "marks": [
    {
      "type": "rect",
      "from": {"data": "table"},
      "encode": {
        "enter": {
          "x": {"scale": "xscale", "field": "category"},
          "width": {"scale": "xscale", "band": 1},
          "y": {"scale": "yscale", "field": "amount"},
          "y2": {"scale": "yscale", "value": 0}
        }
      }
    }
  ]
}''',
            'Alpha Category'  # In JSON string value
        )
    },
    'vegalite': {
        'default': (
            '''{
  "$schema": "https://vega.github.io/schema/vega-lite/v5.json",
  "description": "A simple bar chart",
  "data": {
    "values": [
      {"category": "Alpha Category", "value": 28},
      {"category": "Beta Category", "value": 55},
      {"category": "Gamma Category", "value": 43}
    ]
  },
  "mark": "bar",
  "encoding": {
    "x": {"field": "category", "type": "nominal"},
    "y": {"field": "value", "type": "quantitative"}
  }
}''',
            'Alpha Category'  # In JSON string value
        )
    },

    # === WAVEDROM ===
    # JSON-based timing diagrams
    'wavedrom': {
        'default': (
            '''{ "signal": [
  { "name": "clock_signal",  "wave": "P......" },
  { "name": "data_bus",  "wave": "x.==.=x", "data": ["head", "body", "tail"] },
  { "name": "wire_out", "wave": "0.1..0." }
]}''',
            'clock_signal'  # Signal name in JSON
        )
    },

    # === ERD ===
    # Entity-relationship, strict identifier parsing
    # ZWC in entity names will likely fail - test anyway
    'erd': {
        'default': (
            '''[Person]
*name
height
weight
+birth_location_id

[Location]
*id
city
state
country

Person *--1 Location''',
            'name'  # Attribute name (may fail with strict parsing)
        )
    },

    # === DBML ===
    # Database markup, strict parsing
    'dbml': {
        'default': (
            '''Table users {
  id integer [primary key, note: "User ID"]
  username varchar [note: "User Name"]
  role varchar
  created_at timestamp
}

Table posts {
  id integer [primary key]
  title varchar [note: "Post Title"]
  body text
  user_id integer
  created_at timestamp
}

Ref: posts.user_id > users.id''',
            'User ID'  # In quoted note
        )
    },

    # === DITAA ===
    # ASCII art diagrams
    'ditaa': {
        'default': (
            '''    +--------+   +-------+    +-------+
    |        +---+ ditaa +----+       |
    |  Text  |   +-------+    |diagram|
    |Document|   |!magic!|    |       |
    |     {d}|   |       |    |       |
    +---+----+   +-------+    +-------+
        :                         ^
        |       Lots of work      |
        +-------------------------+''',
            'Document'  # Text in box (part of Text\nDocument)
        )
    },

    # === SVGBOB ===
    # ASCII art to SVG
    'svgbob': {
        'default': (
            r'''       .---.
      /-o-/--
   .-/ / /->
  ( *  \/
   '-.  \
      \ /
       '
    Bob''',
            'Bob'  # Text label
        )
    },

    # === EXCALIDRAW ===
    # JSON-based hand-drawn style
    'excalidraw': {
        'default': (
            '''{
  "type": "excalidraw",
  "version": 2,
  "elements": [
    {
      "type": "rectangle",
      "x": 100,
      "y": 100,
      "width": 200,
      "height": 100,
      "strokeColor": "#000000",
      "backgroundColor": "#fab005",
      "fillStyle": "solid",
      "strokeWidth": 1,
      "roughness": 1,
      "opacity": 100,
      "id": "rect1"
    },
    {
      "type": "text",
      "x": 150,
      "y": 140,
      "text": "Hello World",
      "fontSize": 20,
      "fontFamily": 1,
      "strokeColor": "#000000",
      "id": "text1"
    }
  ]
}''',
            'Hello World'  # In JSON text field
        )
    },

    # === STRUCTURIZR ===
    # Architecture DSL - use quoted strings
    'structurizr': {
        'default': (
            '''workspace "My Workspace" "Description" {
    model {
        user = person "End User" "A user of the system"
        system = softwareSystem "My System" "Does things"
        user -> system "Uses"
    }
    views {
        systemContext system {
            include *
            autolayout lr
        }
    }
}''',
            'End User'  # In quoted person label
        )
    },

    # === BPMN ===
    # XML-based business process
    'bpmn': {
        'default': (
            '''<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
             xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
             id="definitions"
             targetNamespace="http://example.com">
  <process id="Process_1" isExecutable="false">
    <startEvent id="start" name="Start Event"/>
    <task id="task1" name="Do Something Important"/>
    <endEvent id="end" name="End Event"/>
    <sequenceFlow id="flow1" sourceRef="start" targetRef="task1"/>
    <sequenceFlow id="flow2" sourceRef="task1" targetRef="end"/>
  </process>
  <bpmndi:BPMNDiagram id="diagram">
    <bpmndi:BPMNPlane id="plane" bpmnElement="Process_1">
      <bpmndi:BPMNShape id="start_di" bpmnElement="start">
        <dc:Bounds x="100" y="100" width="36" height="36"/>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="task1_di" bpmnElement="task1">
        <dc:Bounds x="200" y="80" width="100" height="80"/>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="end_di" bpmnElement="end">
        <dc:Bounds x="350" y="100" width="36" height="36"/>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="flow1_di" bpmnElement="flow1">
        <dc:Bounds x="136" y="118" width="64" height="0"/>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="flow2_di" bpmnElement="flow2">
        <dc:Bounds x="300" y="118" width="50" height="0"/>
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</definitions>''',
            'Do Something Important'  # In name attribute
        )
    },

    # === BYTEFIELD ===
    # Clojure-based byte diagrams
    'bytefield': {
        'default': (
            '''(defattrs :bg-green {:fill "#a0ffa0"})
(defattrs :bg-yellow {:fill "#ffffa0"})

(draw-column-headers)
(draw-box "Source Field" {:span 4 :bg :bg-green})
(draw-box "Destination Field" {:span 4 :bg :bg-yellow})''',
            'Source Field'  # In quoted string
        )
    },

    # === SYMBOLATOR ===
    # HDL symbol diagrams
    'symbolator': {
        'default': (
            '''module demo_device #(
    parameter SIZE = 8,
    parameter BITS = 4
)(
    input [SIZE-1:0] data_input,
    input clock,
    output reg [BITS-1:0] result_output
);
endmodule''',
            'data_input'  # Port name (identifier - may have issues)
        )
    },

    # === UMLET ===
    # UML diagrams in XML
    'umlet': {
        'default': (
            '''<?xml version="1.0" encoding="UTF-8"?>
<diagram program="umlet" version="14.3.0">
  <zoom_level>10</zoom_level>
  <element>
    <type>com.baselet.element.old.element.Class</type>
    <coordinates>
      <x>100</x>
      <y>100</y>
      <w>150</w>
      <h>80</h>
    </coordinates>
    <panel_attributes>Customer Entity
--
-name: String
-id: int
--
+getName(): String</panel_attributes>
  </element>
</diagram>''',
            'Customer Entity'  # In panel_attributes text
        )
    },

    # === WIREVIZ ===
    # Wiring diagrams in YAML
    'wireviz': {
        'default': (
            '''connectors:
  X1:
    type: D-Sub
    subtype: female
    pinlabels: [DCD, RX, TX, DTR, GND, DSR, RTS, CTS, RI]
    notes: "Main Connector"
  X2:
    type: Molex KK 254
    subtype: female
    pinlabels: [GND, RX, TX]

cables:
  W1:
    gauge: 0.25 mm2
    length: 0.2
    color_code: DIN
    wirecount: 3
    shield: true

connections:
  -
    - X1: [5,2,3]
    - W1: [1,2,3]
    - X2: [1,3,2]''',
            'Main Connector'  # In quoted notes field
        )
    },
}
