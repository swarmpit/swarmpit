import { useMemo } from "react";
import CodeMirror, { EditorView } from "@uiw/react-codemirror";
import { yaml } from "@codemirror/lang-yaml";
import { json } from "@codemirror/lang-json";
import { HighlightStyle, syntaxHighlighting } from "@codemirror/language";
import { tags } from "@lezer/highlight";

const editorTheme = EditorView.theme({
  "&": { backgroundColor: "var(--card)", color: "var(--foreground)", fontSize: "0.75rem" },
  "&.cm-focused": { outline: "none" },
  ".cm-scroller": { fontFamily: "ui-monospace, SFMono-Regular, Menlo, monospace", lineHeight: "1.75" },
  ".cm-content": { padding: "12px 0", caretColor: "var(--foreground)" },
  ".cm-line": { padding: "0 12px" },
  ".cm-gutters": { backgroundColor: "var(--muted)", color: "var(--muted-foreground)", borderRightColor: "var(--border)" },
  ".cm-activeLine, .cm-activeLineGutter": { backgroundColor: "var(--muted)" },
  ".cm-cursor, .cm-dropCursor": { borderLeftColor: "var(--foreground)" },
  ".cm-selectionBackground, &.cm-focused .cm-selectionBackground, .cm-content ::selection": { backgroundColor: "var(--accent)" },
  ".cm-selectionMatch": { backgroundColor: "var(--accent)" },
});
const highlighting = syntaxHighlighting(HighlightStyle.define([
  { tag: [tags.propertyName, tags.keyword, tags.tagName], color: "var(--primary)", fontWeight: "500" },
  { tag: [tags.string, tags.attributeValue], color: "var(--success-foreground)" },
  { tag: [tags.number, tags.bool, tags.null], color: "var(--warning-foreground)" },
  { tag: tags.comment, color: "var(--muted-foreground)", fontStyle: "italic" },
  { tag: [tags.punctuation, tags.operator], color: "var(--muted-foreground)" },
]));
const languages = { YAML: yaml(), JSON: json() };
const basicSetup = {
  lineNumbers: true,
  foldGutter: false,
  highlightActiveLine: false,
  highlightActiveLineGutter: false,
  autocompletion: false,
};

export default function ConfigValueEditor({
  id, labelId, hintId, height, value, onChange, syntax, readOnly, disabled, required, onReady,
}: {
  id: string;
  labelId: string;
  hintId?: string;
  height: number;
  value: string;
  onChange?: (value: string) => void;
  syntax: string;
  readOnly: boolean;
  disabled: boolean;
  required: boolean;
  onReady?: (view: EditorView) => void;
}) {
  const extensions = useMemo(() => [
    editorTheme,
    highlighting,
    ...(syntax === "YAML" || syntax === "JSON" ? [languages[syntax]] : []),
    EditorView.contentAttributes.of({
      id,
      "aria-labelledby": labelId,
      ...(hintId ? { "aria-describedby": hintId } : {}),
      "aria-multiline": "true",
      "aria-readonly": String(readOnly),
      "aria-disabled": String(disabled),
      "aria-required": String(required),
      tabindex: disabled ? "-1" : "0",
    }),
  ], [id, labelId, hintId, syntax, readOnly, disabled, required]);
  return (
    <div style={{ height }} className={`min-w-0 overflow-hidden rounded-md border bg-card focus-within:border-ring focus-within:ring-[3px] focus-within:ring-ring/50 ${disabled ? "opacity-50" : ""}`}>
      <CodeMirror
        value={value}
        onChange={readOnly || disabled ? undefined : onChange}
        onCreateEditor={onReady}
        readOnly={readOnly || disabled}
        editable={!readOnly && !disabled}
        extensions={extensions}
        basicSetup={basicSetup}
        indentWithTab={false}
        theme="none"
        height={height - 2 + "px"}
        maxHeight={height - 2 + "px"}
        spellCheck={false}
      />
    </div>
  );
}
