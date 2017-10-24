#include "caffe_mobile.hpp"
#include <stdio.h>

const int CLASS_NUM = 21;

float CONF_THRESH = 0.7;

using namespace std;

namespace caffe {

  CaffeMobile *CaffeMobile::caffe_mobile_ = NULL;

  CaffeMobile *CaffeMobile::get() {
    return caffe_mobile_;
  }

  CaffeMobile *CaffeMobile::get(const string &param_file,
                                const string &trained_file) {
    if (!caffe_mobile_) {
      try {
        caffe_mobile_ = new CaffeMobile(param_file, trained_file);
      } catch (std::invalid_argument &e) {
        // TODO
      }
    }
    return caffe_mobile_;
  }

  CaffeMobile::CaffeMobile(const string &param_file, const string &trained_file) {
    // Load Caffe model
    Caffe::set_mode(Caffe::CPU);
    net_.reset(new Net<float>(param_file, caffe::TEST));
    if (net_.get() == NULL) {
      throw std::invalid_argument("Invalid arg: param_file=" + param_file);
    }
    net_->CopyTrainedLayersFrom(trained_file);

    Blob<float> *input_layer = net_->input_blobs()[0];
    input_channels_ = input_layer->channels();
    CHECK(input_channels_ == 3 || input_channels_ == 1)
        << "Input layer should have 1 or 3 channels.";
    input_width_  = input_layer->width();
    input_height_ = input_layer->height();
  }

  CaffeMobile::~CaffeMobile() {
    net_.reset();
  }

  bool CaffeMobile::predictMNSSD(float *inputData,
                               int h, int w,
                               std::vector<std::vector<float> > &detections){
    Blob<float> *input_layer = net_->input_blobs()[0];
    input_layer->Reshape(1, 3, input_height(), input_width());

    net_->Reshape();

    net_->blob_by_name("data")->set_cpu_data(inputData);

    LOG(INFO);
    net_->Forward();

    Blob<float>* result_blob = net_->output_blobs()[0];
    const float* result = result_blob->cpu_data();
    const int num_det = result_blob->height();


    for (int k = 0; k < num_det; ++k) {
      if (result[0] == -1) {
        // Skip invalid detection.
        result += 7;
        continue;
      }

      float temp_confidence;
      vector<float> tmp_box(result + 3, result + 7);

      temp_confidence = *(result + 2);

      checkConfidence(tmp_box, temp_confidence, detections, (int)*(result + 1), h, w);
      result += 7;
    }

    return true;
  }

  void CaffeMobile::checkConfidence(vector<float> pred_boxes,
                       float confidence,
                       vector<vector<float>> &results,
                       int cls_num,
                       int h, int w)
  {
    LOG(INFO)<<"CLASS_NUM: "<<cls_num<<"   confidence: "<<confidence;
		if (confidence > CONF_THRESH){
      LOG(INFO)<<"pred_boxes[0]: "<<pred_boxes[0]<<"   w: "<<w;
      pred_boxes[0] *= w;
      LOG(INFO)<<"pred_boxes[0]: "<<pred_boxes[0]<<"   w: "<<w;
      pred_boxes[1] *= h;
      pred_boxes[2] *= w;
      pred_boxes[3] *= h;
      pred_boxes.push_back(confidence);
      pred_boxes.push_back((float)cls_num);
      results.push_back(pred_boxes);
      LOG(INFO)<<results.size();
    }
  }
} // namespace caffe
