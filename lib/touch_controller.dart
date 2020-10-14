import 'dart:ui';

import 'package:flutter/material.dart';

class TouchPosition {
//  doube get  x => offset.dx;
  final Offset offset;
  final int timestamp;

  TouchPosition(this.offset, this.timestamp);

  double updateVisibility(int now, int durationMs) {
    var elapse = now - timestamp;
    var visibility = ((durationMs - elapse).toDouble() / durationMs);
    this._visibility = visibility;
    return visibility;
  }

  double getVisibility() {
// because the touch is added but the frame rendered is in the past
// so we hide the touch of the future momentarily until the frame catches up
    if (_visibility > 1.0) {
      return 0.0;
    }
    return _visibility;
  }

  double _visibility = 1.0;
}

class TouchController {
  var touchPositions = List<TouchPosition>();

  setTouchPosition(offset, int timestamp) {
    touchPositions.add(TouchPosition(offset, timestamp));
  }

  List<TouchPosition> cleanTouchPositions(int now) {
    var newTouchPositions = List<TouchPosition>();
    for (var touch in touchPositions) {
      var visibility = touch.updateVisibility(now, 250);
      if (visibility > 0) {
        newTouchPositions.add(touch);
      }
    }
    return newTouchPositions;
  }

  drawTouch(Canvas canvas, int timestamp, double ratio) {
    touchPositions = cleanTouchPositions(timestamp);
    print(
        "Touch position added now count is before draw ${touchPositions.length}");
    var defaultRadius = 50 * ratio;
    for (var touch in touchPositions) {
      var paint = Paint();

      var opacity = (128 * touch.getVisibility()).toInt();
      paint.color = Colors.yellow.withAlpha(opacity);
      var radius = defaultRadius * touch.getVisibility();
      final scaledOffset = touch.offset * ratio;
      canvas.drawCircle(
          Offset(scaledOffset.dx, scaledOffset.dy), radius, paint);
    }
  }
}
