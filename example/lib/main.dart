import 'dart:async';

import 'package:flutter/material.dart';
import 'package:images_to_video/images_to_video.dart';
import 'package:images_to_video/recordable_widget.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  Timer timer;
  int time = 0;

  @override
  void initState() {
    super.initState();
    timer = Timer.periodic(Duration(milliseconds: 100), (timer) {
      time++;
      setState(() {});
    });

  }

  get timerValue {
    if (timer == null) return '0';
    var fast = time % 10;
    var slow = (time / 10).floor();
    return '$slow:$fast';
  }


  @override
  void dispose() {
    timer.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return RecordableWidget(
      child: MaterialApp(
        home: DefaultTabController(
          length: 3,
          child: Scaffold(
            appBar: AppBar(
              title: const Text('Screen Recorder example apps'),
              bottom: TabBar(
                tabs: [
                  Tab(icon: Icon(Icons.fiber_manual_record)),
                  Tab(icon: Icon(Icons.stop)),
                  Tab(icon: Icon(Icons.pause)),
                ],
              ),
            ),
            body: Column(
              children: [
                Text(
                  '$timerValue',
//                  '',
                  style: TextStyle(fontSize: 25.0),
                ),
                Expanded(
                  child: TabBarView(
                    children: [
                      IconButton(
                        icon: Icon(Icons.fiber_manual_record),
                        onPressed: () {
//                    ScreenRecorder.startRecording(fps: 10);
                        },
                      ),
                      IconButton(
                        icon: Icon(Icons.stop),
                        onPressed: () {
                          ImagesToVideo.stop();
                        },
                      ),
                      IconButton(
                        icon: Icon(Icons.pause),
                        onPressed: () {},
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
