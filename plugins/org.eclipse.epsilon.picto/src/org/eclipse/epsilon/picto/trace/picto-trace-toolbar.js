class PictoTraceToolbar {

    toolbar;
    self;
    scheduledToHide = false;
    target;
    mouseIsOver = false;

    constructor() {
        self = this;
        self.traceManager = new PictoTraceManager();
        self.toolbar = self.#createToolbar();
        self.toolbar.onmouseover = function() {
            self.mouseIsOver = true;
        }
        self.toolbar.onmouseout = function() {
            self.mouseIsOver = false;
        }
        addEventListener("mouseover", (event) => {
            var target = event.target;

            // Skip if target is toolbar itself
            if (self.toolbar && self.toolbar.contains(target)) {
                return;
            }

            // Check if target is traceable
            if (self.traceManager.isTraceable(target)) {
                self.show(event, target);
                return;
            }

            // Check parent (for text nodes inside spans)
            if (target.parentElement && self.traceManager.isTraceable(target.parentElement)) {
                self.show(event, target.parentElement);
                return;
            }

            // Check if any ancestor span/tspan has trace-tag
            var ancestor = target.closest ? target.closest("[trace-tag]") : null;
            if (ancestor && self.traceManager.isTraceable(ancestor)) {
                self.show(event, ancestor);
                return;
            }

            // Check for trace-tag-group ancestor (cross-element traces)
            var groupAncestor = target.closest ? target.closest("[trace-tag-group]") : null;
            if (groupAncestor && self.traceManager.isTraceable(groupAncestor)) {
                self.show(event, groupAncestor);
                return;
            }

            // No traceable element found - hide toolbar
            if (!self.contains(target)) {
                self.hide();
            }
        });
    }

    show(event, target) {
        this.scheduledToHide = false;
        if (target != self.target && !self.contains(event.target)) {
            // Clear previous group highlighting
            self.#clearGroupHighlight();

            self.target = target;

            // Get applicable actions for this trace
            var traceTag = self.traceManager.getTrace(target);
            var applicableResult = "";
            if (typeof window.getApplicableTraceActions === "function") {
                applicableResult = window.getApplicableTraceActions(traceTag);
            }
            var applicableIds = applicableResult ? applicableResult.split(",").filter(function(id) {
                return id.length > 0;
            }) : [];

            // If no applicability check available, show all actions
            if (applicableIds.length === 0 && !window.getApplicableTraceActions) {
                applicableIds = (window.pictoTraceActions || []).map(function(a) { return a.id; });
            }

            // Show/hide buttons based on applicability
            var hasVisibleButtons = false;
            self.toolbar.querySelectorAll("button").forEach(function(btn) {
                var isApplicable = applicableIds.includes(btn.dataset.actionId);
                btn.style.display = isApplicable ? "" : "none";
                if (isApplicable) hasVisibleButtons = true;
            });

            // Don't show toolbar if no applicable actions
            if (!hasVisibleButtons) {
                self.toolbar.style.display = "none";
                return;
            }

            // Highlight all elements in the same trace group
            self.#highlightGroup(target);

            self.toolbar.style.position = "absolute";
            self.toolbar.style.left = event.pageX + 5 + "px";
            self.toolbar.style.top = event.pageY + 5 + "px";
            self.toolbar.style.display = "block";
        }
    }

    hide() {
        if (self.toolbar.style.display != "none") {
            self.scheduledToHide = true;
            setTimeout(self.#doHide, 500);
        }
    }

    #doHide() {
        if (self.scheduledToHide && !self.mouseIsOver) {
            self.toolbar.style.display = "none";
            self.target = null;
            self.#clearGroupHighlight();
        }
    }

    #highlightGroup(target) {
        if (target.hasAttribute("trace-tag-group")) {
            var groupId = target.getAttribute("trace-tag-group");
            document.querySelectorAll('[trace-tag-group="' + groupId + '"]').forEach(function(el) {
                el.classList.add("trace-group-hover");
            });
        }
    }

    #clearGroupHighlight() {
        document.querySelectorAll(".trace-group-hover").forEach(function(el) {
            el.classList.remove("trace-group-hover");
        });
    }

    #createToolbar() {
        var toolbar = document.createElement("div");
        document.body.appendChild(toolbar);
        toolbar.classList.add("picto-trace-toolbar");

        // Dynamically create buttons from registered actions
        if (window.pictoTraceActions && window.pictoTraceActions.length > 0) {
            for (var action of window.pictoTraceActions) {
                toolbar.appendChild(self.#createAction(action));
            }
        }

        toolbar.style.display = "none";
        return toolbar;
    }

    #createAction(actionDef) {
        var button = document.createElement("button");
        button.classList.add("picto-trace-toolbar-button");
        button.type = "button";
        button.title = actionDef.tooltip || actionDef.label || actionDef.id;
        button.dataset.actionId = actionDef.id;

        if (actionDef.hasIcon) {
            var image = document.createElement("img");
            image.src = actionDef.id + ".png";
            image.alt = actionDef.label || actionDef.id;
            button.appendChild(image);
        } else {
            button.textContent = actionDef.label || actionDef.id;
        }

        button.onclick = function() {
            var traceTag = self.traceManager.getTrace(self.target);
            window["picto_toolbar_" + actionDef.id](traceTag);
        };
        return button;
    }

    contains(node) {
        return self.toolbar.contains(node);
    }

}

class PictoTraceManager {

    #zwcChars;

    constructor() {
        this.#zwcChars = getZeroWidthCharacter();
    }

    /**
     * Check if element is traceable.
     * Supports trace-tag attribute, trace-tag-group attribute (for cross-element traces),
     * and legacy suffix approach.
     */
    isTraceable(node) {
        // Check for trace-tag attribute on span/tspan
        if (node.nodeType === Node.ELEMENT_NODE &&
            node.hasAttribute && node.hasAttribute("trace-tag")) {
            return true;
        }
        // Check for trace-tag-group attribute (cross-element traces)
        if (node.nodeType === Node.ELEMENT_NODE &&
            node.hasAttribute && node.hasAttribute("trace-tag-group")) {
            return true;
        }
        // Fallback: Check for legacy suffix-based traces (backward compat)
        if (node.nodeType === Node.ELEMENT_NODE && node.children.length === 0) {
            var suffix = this.#getInvisibleCharactersSuffix(node.textContent);
            return suffix !== null && suffix.length > 0;
        }
        return false;
    }

    /**
     * Get trace identifier for element.
     * Returns numeric ID string for trace-tag, trace-tag-group, and legacy approaches.
     */
    getTrace(node) {
        // Get trace from trace-tag attribute
        if (node.nodeType === Node.ELEMENT_NODE &&
            node.hasAttribute && node.hasAttribute("trace-tag")) {
            return node.getAttribute("trace-tag");
        }
        // For grouped traces, find trace-tag from any group member
        if (node.nodeType === Node.ELEMENT_NODE &&
            node.hasAttribute && node.hasAttribute("trace-tag-group")) {
            var groupId = node.getAttribute("trace-tag-group");
            var member = document.querySelector('[trace-tag-group="' + groupId + '"][trace-tag]');
            if (member) {
                return member.getAttribute("trace-tag");
            }
        }
        // Fallback: Legacy suffix detection - decode and return as numeric string
        var suffix = this.#getInvisibleCharactersSuffix(node.textContent);
        if (suffix && suffix.length > 0) {
            return String(this.#decodeZwcSequence(suffix));
        }
        return "";
    }

    #isZwc(char) {
        return this.#zwcChars.indexOf(char) >= 0;
    }

    #zwcToDigit(char) {
        return this.#zwcChars.indexOf(char);
    }

    #decodeZwcSequence(sequence) {
        var id = 0;
        var base = this.#zwcChars.length;
        for (var i = 0; i < sequence.length; i++) {
            var digit = this.#zwcToDigit(sequence.charAt(i));
            if (digit < 0) return -1;
            id = id * base + digit;
        }
        return id;
    }

    #getInvisibleCharactersSuffix(text) {
        if (!text) return "";
        var position = text.length - 1;
        var suffix = "";
        while (position >= 0 && this.#isZwc(text.charAt(position))) {
            suffix = text.charAt(position) + suffix;
            position--;
        }
        return suffix;
    }
}

/*
function edit(suffix) {
    window.alert("edit " + suffix);
}

function del(suffix) {
    window.alert("delete " + suffix);
}*/

new PictoTraceToolbar();
