import { useEffect, useId, useRef, useState } from "react";
import { Choice } from "./common";
import { Label } from "./ui/label";
import { Textarea } from "./ui/textarea";

type CodeEditor = typeof import("./config-value-editor").default;
const syntaxes = ["YAML", "JSON", "Plain text"];

export function CodeField({
  label = "Value",
  value,
  onChange,
  readOnly = false,
  disabled = false,
  required = false,
  hint,
  rows,
  showSyntaxSelector = true,
}: {
  label?: string;
  value: string;
  onChange?: (value: string) => void;
  readOnly?: boolean;
  disabled?: boolean;
  required?: boolean;
  hint?: string;
  rows?: number;
  showSyntaxSelector?: boolean;
}) {
  const id = useId();
  const labelId = id + "-label";
  const hintId = hint ? id + "-hint" : undefined;
  const height = rows === undefined ? 320 : rows * 24 + 18;
  const [syntax, setSyntax] = useState("YAML");
  const [Editor, setEditor] = useState<CodeEditor | null>(null);
  const [loadFailed, setLoadFailed] = useState(false);
  useEffect(() => {
    let mounted = true;
    import("./config-value-editor").then(
      ({ default: editor }) => { if (mounted) setEditor(() => editor); },
      () => { if (mounted) setLoadFailed(true); },
    );
    return () => { mounted = false; };
  }, []);
  const fallbackFocused = useRef(false);
  const fallbackSelection = useRef({ anchor: 0, head: 0 });
  return (
    <div className="min-w-0 space-y-2">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <Label id={labelId} htmlFor={id} onClick={() => {
          if (!disabled) document.getElementById(id)?.focus();
        }}>{label}</Label>
        {showSyntaxSelector && <div className="w-40">
          <Choice label="Syntax" value={syntax} onChange={setSyntax} options={syntaxes} disabled={disabled} />
        </div>}
      </div>
      {!Editor ? (
        <Textarea
          id={id}
          aria-labelledby={labelId}
          aria-describedby={hintId}
          value={value}
          onChange={readOnly || disabled ? undefined : (event) => onChange?.(event.target.value)}
          onFocus={() => { fallbackFocused.current = true; }}
          onBlur={() => { fallbackFocused.current = false; }}
          onSelect={(event) => {
            fallbackSelection.current = { anchor: event.currentTarget.selectionStart, head: event.currentTarget.selectionEnd };
          }}
          required={required}
          readOnly={readOnly}
          disabled={disabled}
          spellCheck={false}
          style={{ height, minHeight: height }}
          className="resize-none font-mono text-xs leading-6"
        />
      ) : (
        <Editor
          id={id}
          labelId={labelId}
          hintId={hintId}
          height={height}
          value={value}
          onChange={onChange}
          syntax={showSyntaxSelector ? syntax : "YAML"}
          required={required}
          readOnly={readOnly}
          disabled={disabled}
          onReady={(view) => {
            if (!fallbackFocused.current || disabled) return;
            fallbackFocused.current = false;
            view.focus();
            view.dispatch({ selection: {
              anchor: Math.min(fallbackSelection.current.anchor, view.state.doc.length),
              head: Math.min(fallbackSelection.current.head, view.state.doc.length),
            }, scrollIntoView: true });
          }}
        />
      )}
      {hint && <p id={hintId} className="text-xs text-muted-foreground">{hint}</p>}
      {loadFailed && <p role="status" className="text-xs text-muted-foreground">Syntax highlighting is unavailable. The value is shown as plain text.</p>}
    </div>
  );
}
