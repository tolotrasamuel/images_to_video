#import "ImagesToVideoPlugin.h"
#if __has_include(<images_to_video/images_to_video-Swift.h>)
#import <images_to_video/images_to_video-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "images_to_video-Swift.h"
#endif

@implementation ImagesToVideoPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftImagesToVideoPlugin registerWithRegistrar:registrar];
}
@end
