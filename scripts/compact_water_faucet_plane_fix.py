import re
import subprocess
from pathlib import Path

PATH = "src/main/resources/assets/scp_additions/models/block/water_faucet.json"
source = subprocess.check_output(
    ["git", "show", f"origin/master:{PATH}"], text=True, encoding="utf-8"
)

pattern = re.compile(
    r'(?P<from_indent>\s*)"from": \[(?P<from>[^\]]+)\],\n'
    r'(?P<to_indent>\s*)"to": \[(?P<to>[^\]]+)\],'
)

changed = 0

def format_number(value: float) -> str:
    text = f"{value:.5f}".rstrip("0").rstrip(".")
    return "0" if text == "-0" else text


def replace(match: re.Match[str]) -> str:
    global changed
    start = [float(value.strip()) for value in match.group("from").split(",")]
    end = [float(value.strip()) for value in match.group("to").split(",")]
    collapsed = [axis for axis in range(3) if abs(start[axis] - end[axis]) < 1.0e-9]
    if not collapsed:
        return match.group(0)
    if len(collapsed) != 1:
        raise RuntimeError(f"Unexpected multi-axis zero-thickness element: {start} -> {end}")
    axis = collapsed[0]
    center = start[axis]
    start[axis] = center - 0.01
    end[axis] = center + 0.01
    changed += 1
    return (
        f'{match.group("from_indent")}"from": '
        f'[{", ".join(format_number(value) for value in start)}],\n'
        f'{match.group("to_indent")}"to": '
        f'[{", ".join(format_number(value) for value in end)}],'
    )

result = pattern.sub(replace, source)
if changed != 10:
    raise RuntimeError(f"Expected 10 zero-thickness elements, changed {changed}")
Path(PATH).write_text(result, encoding="utf-8")
