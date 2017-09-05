#include "caffe_mobile.hpp"
#include <stdio.h>

const int CLASS_NUM = 21;
float NMS_THRESH = 0.5;
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
  }

  CaffeMobile::~CaffeMobile() {
    net_.reset();
  }

  bool CaffeMobile::predictImage(const uint8_t* rgba,
                                 int channels,
                                 const std::vector<float> &mean,
                                 float im_info[3],
                                 int ori_img_info[2],
                                 vector<vector<float>> &results) {
    if ((rgba == NULL) || net_.get() == NULL) {
      LOG(ERROR) << "Invalid arguments: rgba=" << rgba
          << ",net_=" << net_.get();
      return false;
    }

    net_->blob_by_name("data")->Reshape(1, 3, (int)im_info[0], (int)im_info[1]);
    LOG(INFO);
    net_->blob_by_name("im_info")->set_cpu_data(im_info);

    Blob<float> *input_layer = net_->input_blobs()[0];
    int input_channels_ = input_layer->channels();

    LOG(INFO)<<"input_channels: "<<input_channels_;
    CHECK(input_channels_ == 3 || input_channels_ == 1)
        << "Input layer should have 1 or 3 channels.";
    int input_width_  = input_layer->width();
    int input_height_ = input_layer->height();

    float *input_data = input_layer->mutable_cpu_data();
    size_t plane_size = input_height_ * input_width_;

    if (channels == 4) {
      for (size_t i = 0; i < plane_size; i++) {
        input_data[i] = static_cast<float>(rgba[i * 4 + 2]);                   // B
        input_data[plane_size + i] = static_cast<float>(rgba[i * 4 + 1]);      // G
        input_data[2 * plane_size + i] = static_cast<float>(rgba[i * 4]);      // R
        // Alpha is discarded
        if (mean.size() == 3) {
          input_data[i] -= mean[0];
          input_data[plane_size + i] -= mean[1];
          input_data[2 * plane_size + i] -= mean[2];
        }
      }
    } else {
      LOG(ERROR) << "image_channels input_channels not match.";
      return false;
    }

    LOG(INFO);
    net_->Forward();

    float *boxes = NULL;
    float *pred = NULL;
    float *pred_per_class = NULL;
    float *sorted_pred_cls = NULL;
    const float* bbox_delt = NULL;
    const float* rois = NULL;
    const float* pred_cls = NULL;
    int num;

    bbox_delt = net_->blob_by_name("bbox_pred")->cpu_data();
  	num = net_->blob_by_name("rois")->num();
    rois = net_->blob_by_name("rois")->cpu_data();
  	pred_cls = net_->blob_by_name("cls_prob")->cpu_data();
    boxes = new float[num * 4];
  	pred = new float[num * 5 * CLASS_NUM];
  	pred_per_class = new float[num * 5];
  	sorted_pred_cls = new float[num * 5];

    for (int n = 0; n < num; n++)
  	{
  		for (int c = 0; c < 4; c++)
  		{
  			boxes[n * 4 + c] = rois[n * 5 + c + 1] / im_info[2];
  		}
  	}

    bbox_transform_inv(num, bbox_delt, pred_cls, boxes, pred, ori_img_info[0], ori_img_info[1]);
    for (int i = 1; i < CLASS_NUM; i++){
      for (int j = 0; j< num; j++)
      {
        for (int k = 0; k < 5; k++)
        {
          pred_per_class[j * 5 + k] = pred[(i*num + j) * 5 + k];
        }
      }

      vector<vector<float>> temp_pred_boxes;
  		vector<float> temp_confidence;
      for (int j = 0; j < num; j++)
      {
        vector<float> tmp_box;
        tmp_box.push_back(pred_per_class[j * 5 + 0]);
        tmp_box.push_back(pred_per_class[j * 5 + 1]);
        tmp_box.push_back(pred_per_class[j * 5 + 2]);
        tmp_box.push_back(pred_per_class[j * 5 + 3]);
        temp_pred_boxes.push_back(tmp_box);
        temp_confidence.push_back(pred_per_class[j * 5 + 4]);
      }

      apply_nms(temp_pred_boxes, temp_confidence);
      checkConfidence(temp_pred_boxes,temp_confidence, results, i);

    }
    delete[]boxes;
    delete[]pred;
    delete[]pred_per_class;
    delete[]sorted_pred_cls;

    LOG(INFO)<<results.size();
    return true;
  }

  void CaffeMobile::checkConfidence(vector<vector<float>> pred_boxes, vector<float> confidence, vector<vector<float>> &results, int cls_num)
  {
    LOG(INFO)<<confidence.size();
    for(int i=0; i<confidence.size();i++){
      LOG(INFO)<<"CLASS_NUM: "<<cls_num<<"   confidence: "<<confidence[i];
  		if (confidence[i] > CONF_THRESH){
        pred_boxes[i].push_back(confidence[i]);
        pred_boxes[i].push_back((float)cls_num);
        results.push_back(pred_boxes[i]);
        LOG(INFO)<<results.size();
      }
  	}
  }

  void CaffeMobile::bbox_transform_inv(int num, const float* box_deltas, const float* pred_cls, float* boxes, float* pred, int img_height, int img_width)
  {
  	float width, height, ctr_x, ctr_y, dx, dy, dw, dh, pred_ctr_x, pred_ctr_y, pred_w, pred_h;
  	for (int i = 0; i< num; i++)
  	{
  		width = boxes[i * 4 + 2] - boxes[i * 4 + 0] + 1.0;
  		height = boxes[i * 4 + 3] - boxes[i * 4 + 1] + 1.0;
  		ctr_x = boxes[i * 4 + 0] + 0.5 * width;
  		ctr_y = boxes[i * 4 + 1] + 0.5 * height;
  		for (int j = 1; j< CLASS_NUM; j++)
  		{
  			dx = box_deltas[(i*CLASS_NUM + j) * 4 + 0];
  			dy = box_deltas[(i*CLASS_NUM + j) * 4 + 1];
  			dw = box_deltas[(i*CLASS_NUM + j) * 4 + 2];
  			dh = box_deltas[(i*CLASS_NUM + j) * 4 + 3];
  			pred_ctr_x = ctr_x + width*dx;
  			pred_ctr_y = ctr_y + height*dy;
  			pred_w = width * exp(dw);
  			pred_h = height * exp(dh);
  			pred[(j*num + i) * 5 + 0] = max<float>(min<float>(pred_ctr_x - 0.5* pred_w, img_width - 1), 0);
  			pred[(j*num + i) * 5 + 1] = max<float>(min<float>(pred_ctr_y - 0.5* pred_h, img_height - 1), 0);
  			pred[(j*num + i) * 5 + 2] = max<float>(min<float>(pred_ctr_x + 0.5* pred_w, img_width - 1), 0);
  			pred[(j*num + i) * 5 + 3] = max<float>(min<float>(pred_ctr_y + 0.5* pred_h, img_height - 1), 0);
  			pred[(j*num + i) * 5 + 4] = pred_cls[i*CLASS_NUM + j];
  		}
  	}
  }

  void CaffeMobile::apply_nms(vector<vector<float> > &pred_boxes, vector<float> &confidence)
  {
  	for (int i = 0; i < pred_boxes.size() - 1; i++)
  	{
  		float s1 = (pred_boxes[i][2] - pred_boxes[i][0] + 1) *(pred_boxes[i][3] - pred_boxes[i][1] + 1);
  		for (int j = i + 1; j < pred_boxes.size(); j++)
  		{
  			float s2 = (pred_boxes[j][2] - pred_boxes[j][0] + 1) *(pred_boxes[j][3] - pred_boxes[j][1] + 1);

  			float x1 = max(pred_boxes[i][0], pred_boxes[j][0]);
  			float y1 = max(pred_boxes[i][1], pred_boxes[j][1]);
  			float x2 = min(pred_boxes[i][2], pred_boxes[j][2]);
  			float y2 = min(pred_boxes[i][3], pred_boxes[j][3]);

  			float width = x2 - x1;
  			float height = y2 - y1;
  			if (width > 0 && height > 0)
  			{
  				float IOU = width * height / (s1 + s2 - width * height);
  				if (IOU > NMS_THRESH)
  				{
  					if (confidence[i] >= confidence[j])
  					{
  						pred_boxes.erase(pred_boxes.begin() + j);
  						confidence.erase(confidence.begin() + j);
  						j--;
  					}
  					else
  					{
  						pred_boxes.erase(pred_boxes.begin() + i);
  						confidence.erase(confidence.begin() + i);
  						i--;
  						break;
  					}
  				}
  			}
  		}
  	}
  }
} // namespace caffe
