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

    show(event, text) {
        this.scheduledToHide = false;
        if (event.target != self.target && !self.contains(event.target)) {
            self.target = event.target;
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
		// TODO: Instead of hard-coding these, pull them from extensions
		// of the TraceActionExtensionPoint (TBD)
        toolbar.appendChild(self.#createAction("show"));
        toolbar.appendChild(self.#createAction("edit"));
        toolbar.appendChild(self.#createAction("delete"));
        toolbar.style.display = "none";
        return toolbar;
    }

    #createAction(action) {
        var button = document.createElement("button");
        button.classList.add("picto-trace-toolbar-button");
        button.type = "button";
        var image = document.createElement("img");
        image.classList.add("picto-trace-toolbar-button-" + action);
        button.appendChild(image);
        button.onclick = function() {
            window["picto_toolbar_" + action](self.traceManager.getTrace(self.target));
        }
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
