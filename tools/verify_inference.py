#!/usr/bin/env python3
"""Execute best.onnx with onnxruntime to verify it runs and characterize output0."""
import numpy as np
import onnxruntime as ort

MODEL = "/Users/arunrajv/Documents/projects/PotHole/Model/best.onnx"
opts = ort.SessionOptions()
opts.log_severity_level = 3
sess = ort.InferenceSession(MODEL, opts, providers=["CPUExecutionProvider"])

inp = sess.get_inputs()[0]
out = sess.get_outputs()[0]
print("input:", inp.name, inp.shape, inp.type)
print("output:", out.name, out.shape, out.type)

# Test 1: zeros (letterboxed black image)
zeros = np.zeros((1, 3, 640, 640), dtype=np.float32)
r = sess.run(None, {inp.name: zeros})[0]
print("\nzeros ->", r.shape, "min", r.min(), "max", r.max())

# Test 2: random image, normalized like Ultralytics (0..1, RGB CHW)
rng = np.random.default_rng(0)
rand = rng.random((1, 3, 640, 640), dtype=np.float32)
r2 = sess.run(None, {inp.name: rand})[0]
print("random ->", r2.shape, "min", r2.min(), "max", r2.max())

# Examine structure: [1, 5, 8400] = 4 box + 1 cls (single class, no objectness in v8/v11)
boxes = r2[0, 0:4, :]   # cx, cy, w, h
scores = r2[0, 4, :]
print("\nbox channel stats:")
for i, nm in enumerate(["cx", "cy", "w", "h"]):
    print(f"  {nm}: min={boxes[i].min():.2f} max={boxes[i].max():.2f} mean={boxes[i].mean():.2f}")
print(f"score: min={scores.min():.4f} max={scores.max():.4f} >0.25 count={(scores>0.25).sum()}")

# Determinism check
r3 = sess.run(None, {inp.name: rand})[0]
print("\ndeterministic:", np.array_equal(r2, r3))

# Latency
import time
t = time.time()
N = 50
for _ in range(N):
    sess.run(None, {inp.name: rand})
dt = (time.time() - t) / N * 1000
print(f"CPU latency avg over {N}: {dt:.1f} ms")
