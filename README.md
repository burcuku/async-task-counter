# async-task-counter

Counts the number of asynchronous tasks spawned in an event handler or a task

## Requirements

- Android SDK: http://developer.android.com/sdk/index.html
- Android SDK Tools
- Android SDK Platform-tools
- Android SDK Platform (we use Android 4.4.2 / API 19)
- Android system image for the emulator
- (Optional) Eclipse + ADT plugin

- (Optional) Alternative Android Virtual Devices:
- Genymotion: http://www.genymotion.com

- Java (SE 7)

- Apache Ant (1.9.4)

- Python (2.7.5)


## Usage

Ensure the `ANDROID_HOME` environment variable points to a valid Android
platform SDK containing an Android platform library, e.g.,
`$ANDROID_HOME/platforms/android-19/android.jar`. Also, be sure you have
created and started an Android Virtual Device (AVD).

Instrument your application `.apk` file for testing. For instance, try the
example app in `example/sample_vlillechecker.apk`:

    ant -Dapk=example/sample_vlillechecker.apk -Dandroid.api.version=19 -Dpackage.name=com.vlille.checker

(Note that the original `.apk` file will remain unmodified.)

Then, start an Android device and install the instrumented app into the device:

	adb install build/sample_vlillechecker.apk
	
You can read the number of spawned asynchronous tasks using logcat:

    adb logcat ASYNC:I *:S
