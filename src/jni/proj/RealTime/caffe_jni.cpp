#include <jni.h>
#include "caffe_mobile.hpp"

#define IMG_H 300
#define IMG_W 300
#define IMG_C 3
#define MAX_DATA_SIZE IMG_H * IMG_W * IMG_C

static float input_data[MAX_DATA_SIZE];

#ifdef __cplusplus
extern "C" {
  #endif

  JNIEXPORT jboolean JNICALL
  Java_com_example_jazz_realtimeobjectdetection_CameraActivity_loadModel(JNIEnv *env, jobject instance, jstring modelPath_, jstring weightPath_) {
    jboolean ret = true;
    const char *modelPath = env->GetStringUTFChars(modelPath_, 0);
    const char *weightPath = env->GetStringUTFChars(weightPath_, 0);

    if (caffe::CaffeMobile::get(modelPath, weightPath) == NULL) {
      ret = false;
    }

    env->ReleaseStringUTFChars(modelPath_, modelPath);
    env->ReleaseStringUTFChars(weightPath_, weightPath);
    return ret;
  }

  JNIEXPORT jobjectArray JNICALL
  Java_com_example_jazz_realtimeobjectdetection_CameraActivity_mnssd(JNIEnv *env,
                                                                jobject instance,
                                                                jfloatArray jmean,
                                                                jint h, jint w, jbyteArray Y, jbyteArray U, jbyteArray V,
                                                                jint rowStride, jint pixelStride)
  {
    std::vector<float> mean;
    if (NULL != jmean) {
      float * mean_arr = (float *)env->GetFloatArrayElements(jmean, 0);
      int mean_size = env->GetArrayLength(jmean);
      mean.assign(mean_arr, mean_arr+mean_size);
    } else {
      LOG(INFO) << "caffe-jni predict(): args: jmean(NULL)";
    }

    jsize Y_len = env->GetArrayLength(Y);
    jbyte * Y_data = env->GetByteArrayElements(Y, 0);
    assert(Y_len <= MAX_DATA_SIZE);
    jsize U_len = env->GetArrayLength(U);
    jbyte * U_data = env->GetByteArrayElements(U, 0);
    assert(U_len <= MAX_DATA_SIZE);
    jsize V_len = env->GetArrayLength(V);
    jbyte * V_data = env->GetByteArrayElements(V, 0);
    assert(V_len <= MAX_DATA_SIZE);

    #define min(a,b) ((a) > (b)) ? (b) : (a)
    #define max(a,b) ((a) > (b)) ? (a) : (b)

    auto h_offset = max(0, (h - IMG_H) / 2);
    auto w_offset = max(0, (w - IMG_W) / 2);

    auto iter_h = IMG_H;
    auto iter_w = IMG_W;
    if (h < IMG_H) {
        iter_h = h;
    }
    if (w < IMG_W) {
        iter_w = w;
    }

    for (auto i = 0; i < iter_h; ++i) {
      jbyte* Y_row = &Y_data[(h_offset + i) * w];
      jbyte* U_row = &U_data[(h_offset + i) / 4 * rowStride];
      jbyte* V_row = &V_data[(h_offset + i) / 4 * rowStride];
      for (auto j = 0; j < iter_w; ++j) {
        // Tested on Pixel and S7.
        char y = Y_row[w_offset + j];
        char u = U_row[pixelStride * ((w_offset+j)/pixelStride)];
        char v = V_row[pixelStride * ((w_offset+j)/pixelStride)];

        auto b_i = 0 * IMG_H * IMG_W + j * IMG_W + i;
        auto g_i = 1 * IMG_H * IMG_W + j * IMG_W + i;
        auto r_i = 2 * IMG_H * IMG_W + j * IMG_W + i;
        /*
        R = Y + 1.402 (V-128)
        G = Y - 0.34414 (U-128) - 0.71414 (V-128)
        B = Y + 1.772 (U-V)
        */
        input_data[r_i] = -mean[0] + (float) ((float) min(255., max(0., (float) (y + 1.402 * (v - 128)))) * 0.007843);
        input_data[g_i] = -mean[1] + (float) ((float) min(255., max(0., (float) (y - 0.34414 * (u - 128) - 0.71414 * (v - 128)))) * 0.007843);
        input_data[b_i] = -mean[2] + (float) ((float) min(255., max(0., (float) (y + 1.772 * (u - v)))) * 0.007843);
      }
    }

    caffe::CaffeMobile *caffe_mobile = caffe::CaffeMobile::get();
    if (NULL == caffe_mobile) {
      LOG(ERROR) << "caffe-jni predict(): CaffeMobile failed to initialize";
      return NULL;  // not initialized
    }

    // float ** results = NULL;
    std::vector<std::vector<float>> results;
    if (!caffe_mobile->predictMNSSD(input_data, h, w, results)) {
      LOG(WARNING) << "caffe-jni predict(): CaffeMobile failed to predict";
      return NULL; // predict error
    }

    jclass floatArrayClass = env->FindClass("[F");

    if (floatArrayClass == NULL)
    {
        LOG(ERROR) << "native-lib detect(): Cant create floatArrayClass";
        return NULL;
    }

    if (results.size() == 0){
      LOG(INFO)<<"Zero return results";
      return NULL;
    }

    jobjectArray returnResults = env->NewObjectArray((jsize)results.size(), floatArrayClass, NULL);

    // Go through the firs dimension and add the second dimension arrays
    for (unsigned int i = 0; i < results.size(); i++)
    {
        jfloatArray floatArray = env->NewFloatArray(6);
        env->SetFloatArrayRegion(floatArray, 0, 6, results[i].data());
        env->SetObjectArrayElement(returnResults, (jsize) i, floatArray);
        env->DeleteLocalRef(floatArray);
    }

    // Return a Java consumable 2D float array
    return returnResults;
  }

  #ifdef __cplusplus
}
#endif
