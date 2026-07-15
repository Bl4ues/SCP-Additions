# Generic SCP-914 recipe fallback

This branch adds an optional behavior layer derived from the crafting recipes loaded by the server. It does not replace or reinterpret any recipe in `914recipes.json` or `914recipes.d`.

## Priority

1. Explicit SCP-914 JSON recipe for the selected setting;
2. generic recipe fallback;
3. no item transformation.

If a non-player entity is present in the intake and no explicit recipe matches it, generic item processing is not attempted. This prevents a crafting fallback from silently ignoring an entity.

## Setting behavior

### Rough

A single crafted item can be broken back into one randomly selected ingredient. There is also a chance that nothing recoverable survives. Only recipes that produce exactly one result are reversible, which prevents common compression and quantity exploits.

### Coarse

A single crafted item can be disassembled into a random incomplete subset of its recipe ingredients. At least one component survives when a reversible recipe exists, but full recovery is not guaranteed.

### 1:1

When the exact intake can form several different crafting results, one is selected randomly. A single item can also be replaced by another craftable item in the same narrow equipment or tool family. Broad generic `Item` and arbitrary block substitutions are deliberately excluded.

### Fine

The intake is treated as an unordered set of crafting ingredients. Shaped and shapeless recipes are both accepted, and multiple compatible recipes are resolved randomly. Every intake item must be used; extra items prevent the generic match. Crafting remainders such as buckets are returned.

### Very Fine

The machine first resolves the same exact crafting result as Fine. It then attempts one additional recipe step only when the entire second recipe can be made from the quantity of that intermediate result. This allows transformations such as a recipe output immediately becoming a more elaborate form without inventing unrelated missing ingredients.

## Safeguards

- Special and dynamic crafting recipes are excluded initially.
- Forward crafting is limited to the normal nine ingredient capacity.
- Reverse processing accepts only recipes with one output item and never returns the original input as a component.
- Explicit SCP-914 configuration always wins.
- Ambiguous valid results use the server random source rather than a fixed recipe order.
- Item and entity inputs are never mixed by the generic fallback.

The behavior is intentionally not a guaranteed upgrade path. SCP-914 documentation and experiment logs consistently portray the machine as setting-dependent, unpredictable and capable of literal, indirect or destructive interpretations.
