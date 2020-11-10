import 'dart:async';
import 'package:flutter/services.dart';
import 'dart:typed_data';
import 'dart:ui';

class ImagesToVideo {
  static const MethodChannel _channel = const MethodChannel('images_to_video');

  static Future<String> setup({
    String path = 'video-out.mp4',
    bool isDebug = false,
  }) async {
    final String version = await _channel.invokeMethod(
      'setup',
      {
        'outputPath': path,
        'isDebug': isDebug,
      },
    );
    return version;
  }

  static Future<void> addToVideo(
    Uint8List byteArray,
    int timestamp,
  ) async {
    await _channel.invokeMethod(
      'addToVideo',
      {
        'frame': byteArray,
        'timestamp': timestamp,
      },
    );
  }

  static Future<void> stop() async {
    await _channel.invokeMethod('stop');
  }

  static Future<void> touch(Offset position) async {
    await _channel.invokeMethod(
      'updateTouchPosition',
      {
        'x': position.dx.round(),
        'y': position.dy.round(),
        'timestamp': DateTime.now().millisecondsSinceEpoch,
      },
    );
  }
}
