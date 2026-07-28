Editable SCP Sign Support assets

Base canvas: scp_sign_base.png (1024x640)
Hazard overlays: <stable_id>.png (256x256, transparent)
Empty hazard slot: none.png

The editor and world renderer use these display rectangles in the 1024x640 canvas:
- Clearance: x=783..840, y=83..123
- SCP number: x=64..419, y=273..321
- Containment class: x=65..419, y=351..378
- Anomaly type: x=589..824, y=298..313
- Hazard slot 1: x=473..640, y=375..539
- Hazard slot 2: x=622..788, y=375..539
- Hazard slot 3: x=771..938, y=375..539

The stable IDs intentionally match the supplied filenames, including hyphens in:
- hive-mind_organisms.png
- self-evolving_system.png
- self-replicating_object.png

Every slot renders either its selected hazard PNG or none.png. A missing selected asset falls back to none.png.

Final selectable hazard files (31 hazards, plus none.png):
- adaptive_object.png
- antimemetic_hazard.png
- auditory_hazard.png
- autonomous_object.png
- biohazard.png
- bodily_harm_hazard.png
- cognitohazard.png
- corrosive_hazard.png
- ectoentropic.png
- electrical_hazard.png
- existential_threat.png
- extradimensional.png
- fire_hazard.png
- hive-mind_organisms.png
- indirect_injury_hazard.png
- infohazard.png
- invisible_object.png
- liquid.png
- medical_application.png
- memetic_hazard.png
- mutagen_hazard.png
- nonstandard_spacetime.png
- radioactive_hazard.png
- reality_manipulation.png
- self-evolving_system.png
- self-replicating_object.png
- sentient_and_violent.png
- sentient_object.png
- teleporting_object.png
- thermal.png
- toxic_hazard.png
