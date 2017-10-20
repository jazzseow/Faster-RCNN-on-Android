#include <jni.h>
#include "caffe_mobile.hpp"

#ifdef __cplusplus
extern "C" {
  #endif

  JNIEXPORT jboolean JNICALL
  Java_com_example_jazz_objectdetection_Utils_CaffeInterface_loadModel(JNIEnv *env, jobject instance, jstring modelPath_, jstring weightPath_) {
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
  Java_com_example_jazz_objectdetection_Utils_CaffeInterface_fasterRCNN(JNIEnv *env,
                                                                jobject instance,
                                                                jbyteArray jrgba,
                                                                jint jchannels,
                                                                jfloatArray jmean,
                                                                jfloatArray jim_info,
                                                                jintArray jori_img_info){

    uint8_t *rgba = NULL;
    // Get matrix pointer
    if (NULL != jrgba) {
      rgba = (uint8_t *)env->GetByteArrayElements(jrgba, 0);
    } else {
      LOG(ERROR) << "caffe-jni predict(): invalid args: jrgba(NULL)";
      return NULL;
    }

    std::vector<float> mean;
    if (NULL != jmean) {
      float * mean_arr = (float *)env->GetFloatArrayElements(jmean, 0);
      int mean_size = env->GetArrayLength(jmean);
      mean.assign(mean_arr, mean_arr+mean_size);
    } else {
      LOG(INFO) << "caffe-jni predict(): args: jmean(NULL)";
    }

    caffe::CaffeMobile *caffe_mobile = caffe::CaffeMobile::get();
    if (NULL == caffe_mobile) {
      LOG(ERROR) << "caffe-jni predict(): CaffeMobile failed to initialize";
      return NULL;  // not initialized
    }

    float *im_info;
    if (env->GetArrayLength(jim_info) == 3){
      im_info = (float *)env->GetFloatArrayElements(jim_info, 0);
    } else{
      LOG(ERROR) << "native-lib detect():  im_info size must be 3";
      return NULL;
    }

    int *ori_img_info;
    if (env->GetArrayLength(jori_img_info) == 2){
      ori_img_info = (int *)env->GetIntArrayElements(jori_img_info, 0);
    } else{
      LOG(ERROR) << "native-lib detect():  ori_img_info size must be 2";
      return NULL;
    }

    // float ** results = NULL;
    std::vector<std::vector<float>> results;
    if (!caffe_mobile->predictFasterRCNN(rgba, jchannels, mean, im_info, ori_img_info, results)) {
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

  JNIEXPORT jobjectArray JNICALL
  Java_com_example_jazz_objectdetection_Utils_CaffeInterface_ssd(JNIEnv *env,
                                                                jobject instance,
                                                                jbyteArray jrgba,
                                                                jfloatArray jmean,
                                                                jintArray jori_img_info)
  {

    uint8_t *rgba = NULL;
    // Get matrix pointer
    if (NULL != jrgba) {
      rgba = (uint8_t *)env->GetByteArrayElements(jrgba, 0);
    } else {
      LOG(ERROR) << "caffe-jni predict(): invalid args: jrgba(NULL)";
      return NULL;
    }

    std::vector<float> mean;
    if (NULL != jmean) {
      float * mean_arr = (float *)env->GetFloatArrayElements(jmean, 0);
      int mean_size = env->GetArrayLength(jmean);
      mean.assign(mean_arr, mean_arr+mean_size);
    } else {
      LOG(INFO) << "caffe-jni predict(): args: jmean(NULL)";
    }

    int *ori_img_info;
    if (env->GetArrayLength(jori_img_info) == 2){
      ori_img_info = (int *)env->GetIntArrayElements(jori_img_info, 0);
    } else{
      LOG(ERROR) << "native-lib detect():  ori_img_info size must be 2";
      return NULL;
    }

    caffe::CaffeMobile *caffe_mobile = caffe::CaffeMobile::get();
    if (NULL == caffe_mobile) {
      LOG(ERROR) << "caffe-jni predict(): CaffeMobile failed to initialize";
      return NULL;  // not initialized
    }


    // float ** results = NULL;
    std::vector<std::vector<float>> results;
    if (!caffe_mobile->predictSSD(rgba, mean, ori_img_info, results)) {
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

  JNIEXPORT jobjectArray JNICALL
  Java_com_example_jazz_objectdetection_Utils_CaffeInterface_mnssd(JNIEnv *env,
                                                                jobject instance,
                                                                jbyteArray jrgba,
                                                                jfloatArray jmean,
                                                                jintArray jori_img_info)
  {

    uint8_t *rgba = NULL;
    // Get matrix pointer
    if (NULL != jrgba) {
      rgba = (uint8_t *)env->GetByteArrayElements(jrgba, 0);
    } else {
      LOG(ERROR) << "caffe-jni predict(): invalid args: jrgba(NULL)";
      return NULL;
    }

    std::vector<float> mean;
    if (NULL != jmean) {
      float * mean_arr = (float *)env->GetFloatArrayElements(jmean, 0);
      int mean_size = env->GetArrayLength(jmean);
      mean.assign(mean_arr, mean_arr+mean_size);
    } else {
      LOG(INFO) << "caffe-jni predict(): args: jmean(NULL)";
    }

    int *ori_img_info;
    if (env->GetArrayLength(jori_img_info) == 2){
      ori_img_info = (int *)env->GetIntArrayElements(jori_img_info, 0);
    } else{
      LOG(ERROR) << "native-lib detect():  ori_img_info size must be 2";
      return NULL;
    }

    caffe::CaffeMobile *caffe_mobile = caffe::CaffeMobile::get();
    if (NULL == caffe_mobile) {
      LOG(ERROR) << "caffe-jni predict(): CaffeMobile failed to initialize";
      return NULL;  // not initialized
    }

    // float ** results = NULL;
    std::vector<std::vector<float>> results;
    if (!caffe_mobile->predictMNSSD(rgba, mean, ori_img_info, results)) {
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
