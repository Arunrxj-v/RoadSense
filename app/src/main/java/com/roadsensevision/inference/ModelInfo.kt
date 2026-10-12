package com.roadsensevision.inference

/** Verified metadata for the packaged best.onnx (read from the ONNX graph). */
object ModelInfo {
    const val MODEL_FILE = "best.onnx"
    const val INPUT_NAME = "images"
    const val OUTPUT_NAME = "output0"
    const val INPUT_SHAPE = "[1, 3, 640, 640]"
    const val OUTPUT_SHAPE = "[1, 5, 8400]"
    const val IMAGE_SIZE = "640 × 640"
    const val INPUT_DIM = 640             // square letterbox input side, pixels
    const val NUM_ANCHORS = 8400          // decoded anchors (output dim 2)
    const val CLASS_LABEL = "pothole"
    const val CLASS_COUNT = 1
    const val OPSET = 18
    const val IR_VERSION = 8
    const val PRODUCER = "PyTorch 2.14.0 (Ultralytics export)"
    const val STRIDE = 32
    const val TASK = "detect"
    const val NMS_INTERNAL = false          // NMS is applied client-side
    const val DECODE = "cx, cy, w, h + score (single class)"
    const val COLOR_ORDER = "RGB, CHW, normalized 0..1, letterboxed"
    const val PREPROCESSING = "Letterbox to 640×640, pad 114, RGB / 255, NCHW"
    const val INFERENCE_PROVIDER = "CPU (onnxruntime-android)"
    const val SIZE_BYTES = 10604116L         // 10.1 MB

    fun sizeLabel(): String = String.format(java.util.Locale.US, "%.1f MB", SIZE_BYTES / (1024.0 * 1024.0))
}
