# 1.12 dungeon template converter

Converts the Java-serialized 1.12 dungeon templates (`assets/techguns/dungeons/*.ser`) into the JSON files the port reads
(`assets/techguns/dungeons/*.json`). The classes under `techguns/` are stubs with the same names, fields and
serialVersionUIDs as the 1.12 classes, just enough for `ObjectInputStream`.

```
javac -d out $(find . -name "*.java")
java -cp out Convert ../../src/main/resources/assets/techguns/dungeons ../../src/main/resources/assets/techguns/dungeons
```

JSON format per template: `sizeXZ`, `sizeY`, and per segment type: `size` [x, y, z], `palette` (1.12 `registryname@meta`),
`layers` (1.12 placement layer -> flat list of x,y,z,paletteIndex).
