from pathlib import Path
import base64
import zlib

parts = sorted((Path(__file__).parent / "terminal_auxgen_payload").glob("part_*.txt"))
payload = "".join(part.read_text(encoding="utf-8").strip() for part in parts)
source = zlib.decompress(base64.b64decode(payload))
exec(compile(source, "apply_terminal_auxgen_models.py", "exec"))
