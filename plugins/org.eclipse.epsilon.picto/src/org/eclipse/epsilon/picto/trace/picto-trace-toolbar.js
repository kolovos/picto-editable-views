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
            if (self.traceManager.isTraceable(event.target)) {
                self.show(event, event.target);
            }
            else {
                if (!self.contains(event.target)) {
                    self.hide();
                }
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

    isTraceable(node) {
        if (node.children.length == 0) {
            return this.#getInvisibleCharactersSuffix(node.textContent).length > 0;
        }
        else {
            return false;
        }
    }

    getTrace(node) {
        return this.#getInvisibleCharactersSuffix(node.textContent);
    }

    #getInvisibleCharactersSuffix(text) {
        var position = text.length - 1;
        var suffix = "";
        while (position >= 0 && text.charAt(position) == getZeroWidthCharacter()) {
            suffix += text.charAt(position);
            position --;
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