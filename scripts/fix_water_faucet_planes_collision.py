import json
from pathlib import Path

MODEL_PATH = Path("src/main/resources/assets/scp_additions/models/block/water_faucet.json")
JAVA_PATH = Path("src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java")

model = json.loads(MODEL_PATH.read_text(encoding="utf-8"))
expanded = 0
for element in model["elements"]:
    start = element["from"]
    end = element["to"]
    collapsed_axes = [axis for axis in range(3) if abs(start[axis] - end[axis]) < 1.0e-9]
    if not collapsed_axes:
        continue
    if len(collapsed_axes) != 1:
        raise RuntimeError(f"Unexpected multi-axis zero-thickness element: {element}")
    axis = collapsed_axes[0]
    center = start[axis]
    start[axis] = round(center - 0.01, 5)
    end[axis] = round(center + 0.01, 5)
    expanded += 1

if expanded != 10:
    raise RuntimeError(f"Expected 10 zero-thickness faucet elements, found {expanded}")

MODEL_PATH.write_text(json.dumps(model, indent="\t", ensure_ascii=False) + "\n", encoding="utf-8")

java = JAVA_PATH.read_text(encoding="utf-8")
old_shape = """        private static final VoxelShape NORTH_SHAPE = Shapes.or(
                box(4.75, 4.0, 12.8, 11.25, 7.0, 15.8),
                box(3.9, 2.0, 11.7, 7.1, 8.8, 16.5),
                box(8.9, 2.0, 11.7, 12.1, 8.8, 16.5));"""
new_shape = """        private static final VoxelShape NORTH_SHAPE =
                box(4.0, 2.0, 12.0, 12.0, 8.8, 16.0);"""
if old_shape not in java:
    raise RuntimeError("Water Faucet collision block did not match the expected source")
java = java.replace(old_shape, new_shape, 1)
JAVA_PATH.write_text(java, encoding="utf-8")
