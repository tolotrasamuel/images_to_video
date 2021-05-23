import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';

class GestureDetectorTracker extends StatelessWidget {
  final Widget child;

  final Function(DragStartDetails) dragStart;
  final Function(DragUpdateDetails) dragUpdate;
  String get logId => runtimeType.toString();

  const GestureDetectorTracker(
      {Key? key, required this.child, required this.dragStart, required this.dragUpdate})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return RawGestureDetector(
      gestures: {
// commmented because of https://stackoverflow.com/questions/64027432/the-getter-position-was-called-on-null-in-tapgesturerecognizer-handletapdown
        // see onStart below in panGestureRecognizer
//                        AllowMultipleTapGestureRecognizer:
//                            GestureRecognizerFactoryWithHandlers<
//                                AllowMultipleTapGestureRecognizer>(
//                          () => AllowMultipleTapGestureRecognizer(),
//
//                          (AllowMultipleTapGestureRecognizer instance) {
//                            instance.onTapDown = (TapDownDetails event) {
//                              print(
//                                  '$logId outter on tap down kind ${event.kind}, local ${event.localPosition} global ${event.globalPosition}');
////                        this.analyticsCtrl.takeNativeScreenshot();
////                        await takeScreenShot();
//                            };
////
//                          },
//                        ) ,
        AllowMultiplePanGestureRecognizer: GestureRecognizerFactoryWithHandlers<
                AllowMultiplePanGestureRecognizer>(
            () => AllowMultiplePanGestureRecognizer(),
            (AllowMultiplePanGestureRecognizer instance) {
          instance.onStart = dragStart;
          instance.onUpdate = dragUpdate;
          // this is a substitute of onTapDown because of this issue
          // https://stackoverflow.com/questions/64027432/the-getter-position-was-called-on-null-in-tapgesturerecognizer-handletapdown
        })
      },
      behavior: HitTestBehavior.deferToChild,
      child: child,
    );
  }
}

class AllowMultiplePanGestureRecognizer extends PanGestureRecognizer {
  @override
  void rejectGesture(int pointer) {
    acceptGesture(pointer);
  }
}

class AllowMultipleTapGestureRecognizer extends TapGestureRecognizer {
  @override
  void rejectGesture(int pointer) {
    acceptGesture(pointer);
  }
}
