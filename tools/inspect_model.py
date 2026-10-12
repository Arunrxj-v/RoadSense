#!/usr/bin/env python3
"""Inspect the best.onnx pothole detection model: I/O tensors, classes, metadata."""
import sys
import onnx
from onnx import numpy_helper

MODEL = "/Users/arunrajv/Documents/projects/PotHole/Model/best.onnx"

model = onnx.load(MODEL)
onnx.checker.check_model(model)
print("=== MODEL ===")
print("ir_version:", model.ir_version)
print("producer:", model.producer_name, model.producer_version)
print("opset:", [(o.domain or "ai.onnx", o.version) for o in model.opset_import])
print("graph name:", model.graph.name)

print("\n=== INPUTS ===")
for inp in model.graph.input:
    t = inp.type.tensor_type
    shape = [d.dim_value if d.dim_value else d.dim_param for d in t.shape.dim]
    print(f"  {inp.name}: shape={shape} dtype={t.elem_type} ({onnx.TensorProto.DataType.Name(t.elem_type)})")

print("\n=== OUTPUTS ===")
for out in model.graph.output:
    t = out.type.tensor_type
    shape = [d.dim_value if d.dim_value else d.dim_param for d in t.shape.dim]
    print(f"  {out.name}: shape={shape} dtype={t.elem_type} ({onnx.TensorProto.DataType.Name(t.elem_type)})")

print("\n=== INITIALIZERS (first 10) ===")
for init in model.graph.initializer[:10]:
    print("  ", init.name, numpy_helper.to_array(init).shape)

print("\n=== NODE OP TYPES ===")
ops = {}
for n in model.graph.node:
    ops[n.op_type] = ops.get(n.op_type, 0) + 1
for k, v in sorted(ops.items(), key=lambda x: -x[1]):
    print(f"  {k}: {v}")

print("\n=== METADATA PROPS ===")
for p in model.metadata_props:
    val = p.value
    if len(val) > 4000:
        val = val[:4000] + "...[truncated]"
    print(f"  {p.key} = {val}")

print("\n=== STRINGS in raw metadata (labels etc.) ===")
import re
raw = open(MODEL, "rb").read()
for m in set(re.findall(rb"[ -~]{6,}", raw)):
    s = m.decode()
    low = s.lower()
    if any(k in low for k in ("pothole", "class", "label", "names", "imgsz", "stride", "conf")):
        print("  ", s[:300])
