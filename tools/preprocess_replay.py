from __future__ import annotations

import argparse
import re
import shutil
from dataclasses import dataclass
from pathlib import Path


VERSION_ALIASES = {
    "1.14": 11400,
    "26.1": 260100,
}


def parse_value(value: str) -> int:
    value = value.strip()
    if value in VERSION_ALIASES:
        return VERSION_ALIASES[value]
    if "." in value:
        parts = [int(part) for part in value.split(".")]
        if parts[0] == 1:
            return parts[0] * 10000 + parts[1] * 100 + (parts[2] if len(parts) > 2 else 0)
        return parts[0] * 10000 + parts[1] * 100 + (parts[2] if len(parts) > 2 else 0)
    return int(value)


def eval_condition(expr: str, symbols: dict[str, int]) -> bool:
    expr = expr.strip()

    def repl(match: re.Match[str]) -> str:
        name, op, rhs = match.group(1), match.group(2), match.group(3)
        left = symbols.get(name, 0)
        right = parse_value(rhs)
        return f"({left} {op} {right})"

    converted = re.sub(r"\b([A-Z_]+)\s*(>=|<=|==|!=|>|<)\s*([0-9]+(?:\.[0-9]+)*)", repl, expr)

    def bare(match: re.Match[str]) -> str:
        name = match.group(1)
        if name in {"and", "or", "not", "True", "False"}:
            return name
        return str(bool(symbols.get(name, 0)))

    converted = converted.replace("&&", " and ").replace("||", " or ")
    converted = re.sub(r"(?<![=!<>])!(?!=)", " not ", converted)
    converted = re.sub(r"\b([A-Z_]+)\b", bare, converted)
    return bool(eval(converted, {"__builtins__": {}}, {}))


@dataclass
class Frame:
    parent_active: bool
    active: bool
    matched: bool


DIRECTIVE = re.compile(r"^(\s*)//#(if|elseif|else|endif)\b\s*(.*)$")
UNCOMMENT = re.compile(r"^(\s*)//\$\$ ?(.*)$")


def preprocess_text(text: str, symbols: dict[str, int]) -> str:
    out: list[str] = []
    stack: list[Frame] = []

    def active() -> bool:
        return stack[-1].active if stack else True

    for raw_line in text.splitlines(keepends=True):
        newline = ""
        line = raw_line
        if line.endswith("\r\n"):
            line, newline = line[:-2], "\r\n"
        elif line.endswith("\n"):
            line, newline = line[:-1], "\n"

        directive = DIRECTIVE.match(line)
        if directive:
            kind = directive.group(2)
            expr = directive.group(3)
            if kind == "if":
                parent = active()
                cond = eval_condition(expr, symbols)
                stack.append(Frame(parent, parent and cond, parent and cond))
            elif kind == "elseif":
                frame = stack[-1]
                cond = eval_condition(expr, symbols)
                frame.active = frame.parent_active and not frame.matched and cond
                frame.matched = frame.matched or (frame.parent_active and cond)
            elif kind == "else":
                frame = stack[-1]
                frame.active = frame.parent_active and not frame.matched
                frame.matched = True
            elif kind == "endif":
                stack.pop()
            continue

        if not active():
            continue

        uncomment = UNCOMMENT.match(line)
        if uncomment:
            out.append(f"{uncomment.group(1)}{uncomment.group(2)}{newline}")
        else:
            out.append(raw_line)

    if stack:
        raise ValueError("Unclosed preprocessor block")
    return "".join(out)


def should_process(path: Path) -> bool:
    return path.suffix in {".java", ".json", ".mcmeta", ".cfg", ".vert", ".frag"}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--mc", required=True, type=int)
    parser.add_argument("--fabric", default=1, type=int)
    args = parser.parse_args()

    symbols = {"MC": args.mc, "FABRIC": args.fabric}
    if args.output.exists():
        shutil.rmtree(args.output)
    args.output.mkdir(parents=True)

    for src in args.input.rglob("*"):
        rel = src.relative_to(args.input)
        dst = args.output / rel
        if src.is_dir():
            dst.mkdir(parents=True, exist_ok=True)
            continue
        dst.parent.mkdir(parents=True, exist_ok=True)
        if should_process(src):
            text = src.read_text(encoding="utf-8")
            result = preprocess_text(text, symbols)
            if result.strip():
                dst.write_text(result, encoding="utf-8", newline="")
        else:
            shutil.copy2(src, dst)


if __name__ == "__main__":
    main()
