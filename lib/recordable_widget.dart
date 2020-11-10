import 'package:flutter/material.dart';
import 'package:images_to_video/gesture_detector_layer.dart';
import 'package:images_to_video/screenshot_controller.dart';

class RecordableWidget extends StatefulWidget {
  final Widget child;
  static GlobalKey previewContainer = new GlobalKey();
  final bool isDebug;
  const RecordableWidget({Key key, this.child, this.isDebug = false})
      : super(key: key);
  @override
  _RecordableWidgetState createState() => _RecordableWidgetState();
}

class _RecordableWidgetState extends State<RecordableWidget>
    with WidgetsBindingObserver {
  AppLifecycleState _notification;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    ScreenshotController.instance.setup(widget.isDebug);
    WidgetsBinding.instance.addPostFrameCallback((timeStamp) {
      ScreenshotController.instance.takeScreenshot();
    });
  }

  @override
  void reassemble() {
    ScreenshotController.instance.stop();
    super.reassemble();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    _notification = state;
    print('$logId app state changed to $state');
    if (_notification == AppLifecycleState.resumed ||
        _notification == AppLifecycleState.inactive) {
      ScreenshotController.instance.takeScreenshot();
    } else {
      ScreenshotController.instance.stop();
    }
    setState(() {});
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  String get logId => runtimeType.toString();

  @override
  Widget build(BuildContext context) {
    return RepaintBoundary(
      key: RecordableWidget.previewContainer,
      child: GestureDetectorTracker(
        dragStart: (DragStartDetails event) async {
          print(
              '$logId outter on tap down local ${event.localPosition} global ${event.globalPosition}');
          ScreenshotController.instance.touch(event.localPosition);
          ScreenshotController.instance.takeScreenshot();
        },
        dragUpdate: (event) async {
          print(
              '$logId outter on pan update ${event.localPosition} global ${event.globalPosition}');
          ScreenshotController.instance.touch(event.localPosition);
          ScreenshotController.instance.takeScreenshot();
        },
        child: widget.child,
      ),
    );
  }
}
