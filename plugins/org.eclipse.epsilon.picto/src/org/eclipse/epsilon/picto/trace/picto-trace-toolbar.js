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

            // No traceable element found - hide toolbar
            if (!self.contains(target)) {
                self.hide();
            }
        });
    }

    show(event, target) {
        this.scheduledToHide = false;
        if (target != self.target && !self.contains(event.target)) {
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
        }
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

    #zwc;

    constructor() {
        this.#zwc = getZeroWidthCharacter();
    }

    /**
     * Check if element is traceable.
     * Supports both new span-wrapped approach and legacy suffix approach.
     */
    isTraceable(node) {
        // New: Check for trace-tag attribute on span/tspan
        if (node.nodeType === Node.ELEMENT_NODE &&
            node.hasAttribute && node.hasAttribute("trace-tag")) {
            return true;
        }
        // Fallback: Check for legacy suffix-based traces (backward compat)
        if (node.nodeType === Node.ELEMENT_NODE && node.children.length === 0) {
            return this.#getInvisibleCharactersSuffix(node.textContent).length > 0;
        }
        return false;
    }

    /**
     * Get trace identifier for element.
     * Returns numeric ID string for new approach, ZWC string for legacy.
     */
    getTrace(node) {
        // New: Get trace from attribute
        if (node.nodeType === Node.ELEMENT_NODE &&
            node.hasAttribute && node.hasAttribute("trace-tag")) {
            return node.getAttribute("trace-tag");
        }
        // Fallback: Legacy suffix detection
        return this.#getInvisibleCharactersSuffix(node.textContent);
    }

    #getInvisibleCharactersSuffix(text) {
        if (!text) return "";
        var position = text.length - 1;
        var suffix = "";
        while (position >= 0 && text.charAt(position) === this.#zwc) {
            suffix += text.charAt(position);
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