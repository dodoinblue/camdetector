camdetector
===========
This is an experiment application that shows the average color of center 50x50 pixels.
In this demo, following problems are solved:

* Get a frame of CameraPreview/SurfaceView data.
* Convert YUV data to JPG.
* Usage of Bitmap.getPixels.
* Draw text outline.

There are a number issues to be solved:
* Process YUV data directly.
* Solve previewcallback performace and switch to preview callback from oneshot preview callback.
* Convert bytes without saving to filesystem.

Forgive me for the messy code structure.
