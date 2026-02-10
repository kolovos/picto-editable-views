package org.eclipse.epsilon.picto.trace;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.eclipse.epsilon.common.dt.util.LogUtil;
import org.eclipse.epsilon.picto.transformers.elements.AppendingElementTransformer;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Appends the trace toolbar JavaScript and CSS to the HTML body.
 * Dynamically generates action metadata from registered extensions.
 */
public class TraceToolbarAppender extends AppendingElementTransformer {

	protected Path path;

	@Override
	public String getXPath() {
		return "//body[1]";
	}

	@Override
	protected void append(Element root, Document document) throws DOMException {
		try {
			Path tempDir = Files.createTempDirectory("picto-toolbar");

			// Copy base CSS and JS files
			Files.copy(TraceToolbarAppender.class.getResourceAsStream("picto-trace-toolbar.css"),
					tempDir.resolve("picto-trace-toolbar.css"), StandardCopyOption.REPLACE_EXISTING);
			Files.copy(TraceToolbarAppender.class.getResourceAsStream("picto-trace-toolbar.js"),
					tempDir.resolve("picto-trace-toolbar.js"), StandardCopyOption.REPLACE_EXISTING);

			// Load actions from extension point
			TraceToolbarActionExtensionPointManager manager = new TraceToolbarActionExtensionPointManager();
			List<TraceToolbarActionDescriptor> actions = manager.getExtensions();

			// Generate action metadata JS and copy icons
			StringBuilder actionsJs = new StringBuilder();
			actionsJs.append("window.pictoTraceActions = [\n");

			for (TraceToolbarActionDescriptor descriptor : actions) {
				String id = descriptor.getId();
				String label = descriptor.getLabel();
				String tooltip = descriptor.getTooltip();
				boolean hasIcon = false;
				String iconSvg = descriptor.getIconSvg();

				// If no inline SVG, try copying PNG icon to temp directory
				if (iconSvg == null) {
					try (InputStream iconStream = descriptor.getIconAsStream()) {
						if (iconStream != null) {
							File iconFile = tempDir.resolve(id + ".png").toFile();
							iconFile.deleteOnExit();
							Files.copy(iconStream, iconFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
							hasIcon = true;
						}
					} catch (IOException e) {
						LogUtil.log("Failed to copy icon for action: " + id, e);
					}
				}

				// Add to JS array
				actionsJs.append(String.format(
						"  {id:'%s', label:'%s', tooltip:'%s', hasIcon:%s, iconSvg:%s},\n",
						escapeJs(id), escapeJs(label), escapeJs(tooltip), hasIcon,
						iconSvg != null ? "'" + escapeJs(iconSvg) + "'" : "null"));
			}

			actionsJs.append("];\n");

			// Write generated actions JS
			Path actionsJsPath = tempDir.resolve("picto-trace-actions.js");
			Files.write(actionsJsPath, actionsJs.toString().getBytes());
			actionsJsPath.toFile().deleteOnExit();

			// Add script references to document
			Element actionsScript = document.createElement("script");
			actionsScript.setAttribute("src", actionsJsPath.toString());
			root.appendChild(actionsScript);

			Element mainScript = document.createElement("script");
			mainScript.setAttribute("src", tempDir.toString() + "/picto-trace-toolbar.js");
			root.appendChild(mainScript);

			Element css = document.createElement("link");
			css.setAttribute("rel", "stylesheet");
			css.setAttribute("href", tempDir.toString() + "/picto-trace-toolbar.css");
			root.appendChild(css);

			// Mark temp directory for cleanup
			tempDir.toFile().deleteOnExit();
		}
		catch (IOException ex) {
			LogUtil.log(ex);
		}
	}

	private String escapeJs(String s) {
		if (s == null) return "";
		return s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n");
	}
}
