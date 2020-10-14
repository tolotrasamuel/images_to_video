import 'dart:async';
import 'package:flutter/rendering.dart';
import 'package:images_to_video/images_to_video.dart';
import 'dart:typed_data';
import 'dart:ui' as ui;

import 'package:images_to_video/recordable_widget.dart';
import 'package:timing_logger/timing_logger.dart';
import 'package:images_to_video/touch_controller.dart';

class ScreenshotController {
  ScreenshotController._privateConstructor();
  final touchController = new TouchController();
  static final ScreenshotController _instance =
      ScreenshotController._privateConstructor();

  static ScreenshotController get instance => _instance;

  static const FPS = 10;

  static get frameDuration => Duration(milliseconds: (1000 / FPS).round());
  Timer _debounceTimer;
  Timer _frameTick;
  bool paused = false;
  final throttleLimit = new ThrottleFilter(frameDuration);

  String get logId => runtimeType.toString();

  takeScreenshot() async {
    paused = false;
    await _takeSnapshot();
    await _continueForTrailingSeconds();
  }
  pause() async {
    paused = true;
    await _stopTrailingRecord();
  }

  Future<ui.Image> _takeFlutterScreenShoot(double pxRatio) async {
    print('$logId getting flutter screenshot');
    final timelogger = new TimingLogger("Screenshot", "Taking screenshot");
    RenderRepaintBoundary boundary =
        RecordableWidget.previewContainer.currentContext.findRenderObject();
    ui.Image image = await boundary.toImage(pixelRatio: pxRatio);
    timelogger.addSplit("converting boundary to Image done pxRatio: $pxRatio");
    timelogger.dumpToLog();
    return image;

//    return pngBytes;
  }

  _continueForTrailingSeconds() async {
    _startTrailingRecord();
    _debounceTimer?.cancel();
    _debounceTimer = new Timer(Duration(seconds: 5), () {
//      _stopTrailingRecord();
    });
  }

  _stopTrailingRecord() {
    print('$logId stopping trailing record');
    _frameTick?.cancel();
  }

  _startTrailingRecord() {
    if (_frameTick?.isActive == true) return;
    print('$logId starting trailing record');
    _frameTick = Timer.periodic(frameDuration, (_) {
      if(paused) return;
      this._takeSnapshot();
    });
  }

//
  _takeSnapshot() async {
    final canCall = this.throttleLimit.call();
    if (!canCall) return;
    int timestamp = DateTime.now().millisecondsSinceEpoch;
    Uint8List byteArray = await _getScreenshotBytes(timestamp);
    print('$logId Screenshoot as byte ${byteArray.length}');
//    await _saveSnapshot(byteArray);
    await _addToVideo(byteArray, timestamp);
//    await _saveSnapshot(byteArray);
  }

  Future<Uint8List> _getScreenshotBytes(int timestamp) async {
//    return await _getScreenshotNavite();
    double pxRatio = 0.5;
    final image = await _takeFlutterScreenShoot(pxRatio);
    final timelogger = new TimingLogger("Screenshot", "Drawing touches");
    ui.PictureRecorder recorder = new ui.PictureRecorder();
    Canvas c = new Canvas(recorder);
    c.drawImage(image, Offset.zero, new Paint());
    touchController.drawTouch(c, timestamp, pxRatio);
    timelogger.addSplit("drawing circle on canvas");
    final picture = recorder.endRecording();
    final imageAll = await picture.toImage(image.width, image.height);
    timelogger.addSplit("image all to image");
    ByteData byteDataAll = await imageAll.toByteData(format: ui.ImageByteFormat.png);
    timelogger.addSplit("compressing image all");
    timelogger.dumpToLog();
    return byteDataAll.buffer.asUint8List();
  }

  void touch(Offset localPosition) {
    final timestamp = DateTime.now().millisecondsSinceEpoch;
    touchController.setTouchPosition(localPosition, timestamp);
//    ImagesToVideo.touch(localPosition);
  }

  _addToVideo(Uint8List byteArray, int timestamp) {
    ImagesToVideo.addToVideo(byteArray, timestamp);
  }

  void setup() {
    ImagesToVideo.setup(path: 'video-out.mp4');
  }

  void stop() {
    print("$logId stopping recording");
    paused = true;
    _stopTrailingRecord();
    ImagesToVideo.stop();

  }
}

class ThrottleFilter<T> {
  DateTime lastEventDateTime;
  final Duration duration;

  ThrottleFilter(this.duration);

  bool call() {
    final now = new DateTime.now();
    if (lastEventDateTime == null ||
        now.difference(lastEventDateTime) > duration) {
      lastEventDateTime = now;
      return true;
    }
    return false;
  }
}
