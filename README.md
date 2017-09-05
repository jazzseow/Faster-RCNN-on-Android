Faster RCNN on Android
==================

Faster RCNN build on Android with the help of these people:
https://github.com/solrex/caffe-mobile
https://github.com/manutdzou/faster-rcnn-pure-c-plus-implement  

# Screenshots


# For Demo

Follow the instructions from https://github.com/solrex/caffe-mobile

## Step 1: Build Caffe-Mobile Lib with cmake

Test passed ANDROID_ABI:

 - [x] arm64-v8a
 - [x] armeabi
 - [x] armeabi-v7a with NEON (not stable)

```bash
$ git clone --recursive https://github.com/solrex/caffe-mobile.git
$ export NDK_HOME=/path/to/your/ndk  # C:/path/to/your/ndk on MinGW64 (/c/path/to/your/ndk not work for OpenBLAS)
$ ./tools/build_android.sh
```

## Step 2: Build Android App: CaffeSimple with Android Studio

VGG16 Faster RCNN model is used for this app. To run the app, you will need to move the net.protobin and weight.caffemodel from '$ROOT/models/' to '/sdcard/ObjectDetection/' in your Android mobile phone. Create the folder if needed.


# To add your custom model/layer


# For MacOSX & Ubuntu

## Step 1: Install dependency

```
$ brew install protobuf # MacOSX
$ sudo apt install libprotobuf-dev protobuf-compiler libatlas-dev # Ubuntu
```

## Step 2: Build Caffe-Mobile Lib with cmake

```
$ git clone --recursive https://github.com/solrex/caffe-mobile.git
$ mkdir build
$ cd ../build
$ cmake ..
$ make -j 4
```

## Step 3: Build Caffe-bin with cmake

```
$ brew install gflags
$ cmake .. -DTOOLS
$ make -j 4
```

# Thanks

 - Based on https://github.com/BVLC/caffe
 - Inspired by https://github.com/chyh1990/caffe-compact
 - Use https://github.com/Yangqing/ios-cmake
 - Use https://github.com/taka-no-me/android-cmake
 - Windows build script inspired by https://github.com/luoyetx/mini-caffe/tree/master/android
