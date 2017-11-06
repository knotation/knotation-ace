# knotation-editor

_A front-end Knotation editor currently built on CodeMirror_

## Usage

`knotation-editor` emphasizes a minimal interface. It bundles all `js` and `css` needed to run it properly in-browser.

### From ClojureScript

1. Add `[knotation-editor "0.0.10"]` to your `project.clj`
2. Add `[knotation.editor :as ed]` to your `require` statement
3. Call `ed/editor!` with the selector of your desired editor element

Example:

```Clojure
(ns example.editor
  (:require [knotation-editor.editor :as ed]))

(onLoad
 (fn []
   (let [e (ed/editor! "#editor")]
     (add-commands!
      e {"Ctrl-Enter" (fn [ed] (.log js/console "Hello there!"))})
     e)))
```

### From JavaScript

1. Serve the current `resources/knotation_editor.js` file from somewhere
2. Add the appropriate `script` tag to your HTML
3. Call `knotation_editor.editor.fromSelector` with the selector of your desired editor element

Example:

```html
<html>
  <head>
    <script src="../../resources/knotation_editor.js" type="text/javascript"></script>
    <script type="text/javascript">
      function setUp() {
        var ed = knotation_editor.editor.fromSelector('#editor');
        knotation_editor.editor.addCommands(ed, {"Ctrl-Enter": function (ed) { console.log("Hello there!") }});
      }
    </script>
  </head>
  <body onload="setUp()">
    <div id="editor"></div>
  </body>
</html>
```

## Docs

The interface consists of exactly two functions: `editor!` and `add-commands!`. These are also aliased as `addCommands` and `fromSelector` for ease of use from plain JavaScript.

### `editor!`/`fromSelector`

Takes a CSS selector, and optionally `mode`, `theme` and `focus?` optional arguments.

- `mode` determines the highlighting mode for the new editor. It defaults to `"sparql"` _(which is also currently the only supported mode. More will eventually be made available from the `codemirror` mode listings)_
- `theme` determines the CSS theme for the new editor. It defaults to `"default"` _(again, currently the only supported option)_
- `focus?` determines whether the new editor will autmatically be the focus on page load. defaults to `true`

### `add-commands!`/`addCommands`

Takes an editor and a map of commands (`add-commands!` accepts a Clojure map while `addCommands` accepts a JavaScript object). It adds the new commands under the specified bindings.
