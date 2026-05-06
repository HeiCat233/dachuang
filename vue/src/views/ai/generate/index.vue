<template>
  <div class="ai-generate-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>AI内容生成</span>
        </div>
      </template>
      
      <!-- 输入区域 -->
      <div class="input-section">
        <el-form :model="form" label-width="100px">
          <!-- 文字描述输入 -->
          <el-form-item label="文字描述">
            <el-input
              type="textarea"
              v-model="form.description"
              placeholder="请输入产品特性描述，例如：一款现代简约风格的LED台灯，白色外壳，可调亮度"
              :rows="4"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
          
          <!-- 图片上传区域 -->
          <el-form-item label="参考图片">
            <el-upload
              class="upload-demo"
              action="#"
              :auto-upload="false"
              :on-change="handleImageChange"
              :on-remove="handleImageRemove"
              :file-list="fileList"
              drag
              multiple
            >
              <i class="el-icon-upload"></i>
              <div class="el-upload__text">
                拖拽文件到此处或 <em>点击上传</em>
              </div>
              <template #tip>
                <div class="el-upload__tip">
                  支持JPG、PNG格式，单个文件不超过5MB
                </div>
              </template>
            </el-upload>
          </el-form-item>
          
          <!-- 生图参数设置 -->
          <el-form-item label="生图参数">
            <el-row :gutter="20">
              <el-col :span="8">
                <el-form-item label="图片尺寸">
                  <el-select v-model="form.imageParams.size" placeholder="请选择">
                    <el-option label="512x512" value="512x512" />
                    <el-option label="1024x1024" value="1024x1024" />
                    <el-option label="1024x1536" value="1024x1536" />
                    <el-option label="1536x1024" value="1536x1024" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="风格选择">
                  <el-select v-model="form.imageParams.style" placeholder="请选择">
                    <el-option label="写实风格" value="realistic" />
                    <el-option label="卡通风格" value="cartoon" />
                    <el-option label="油画风格" value="oil_painting" />
                    <el-option label="水彩风格" value="watercolor" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="生成数量">
                  <el-select v-model="form.imageParams.count" placeholder="请选择">
                    <el-option label="1张" value="1" />
                    <el-option label="2张" value="2" />
                    <el-option label="4张" value="4" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
          </el-form-item>
        </el-form>
      </div>
      
      <!-- 操作按钮 -->
      <div class="action-section">
        <el-button type="primary" @click="generateImage" :loading="loading.generateImage">
          生成图片
        </el-button>
        <el-button type="success" @click="generateCopywriting" :loading="loading.generateCopywriting">
          生成文案
        </el-button>
        <el-button type="warning" @click="generateAll" :loading="loading.generateAll">
          一键生成
        </el-button>
      </div>
      
      <!-- 结果展示区域 -->
      <div class="result-section">
        <!-- 图片结果 -->
        <el-card v-if="imageResults.length > 0" class="result-card">
          <template #header>
            <div class="result-header">
              <span>图片生成结果</span>
            </div>
          </template>
          <div class="image-results">
            <div v-for="(image, index) in imageResults" :key="index" class="image-item">
              <el-image
                :src="image"
                fit="cover"
                :preview-src-list="imageResults"
              />
              <div class="image-actions">
                <el-button size="small" type="primary" @click="downloadImage(image)">
                  下载
                </el-button>
                <el-button size="small" type="danger" @click="removeImage(index)">
                  删除
                </el-button>
              </div>
            </div>
          </div>
        </el-card>
        
        <!-- 文案结果 -->
        <el-card v-if="copywritingResult" class="result-card">
          <template #header>
            <div class="result-header">
              <span>文案生成结果</span>
            </div>
          </template>
          <div class="copywriting-result">
            <el-input
              type="textarea"
              v-model="copywritingResult"
              placeholder="文案生成结果将显示在这里"
              :rows="6"
              readonly
            />
            <div class="copywriting-actions">
              <el-button type="primary" @click="copyCopywriting">
                复制文案
              </el-button>
            </div>
          </div>
        </el-card>
      </div>
    </el-card>
  </div>
</template>

<script>
import { createTask, generateImage, generateCopywriting, generateAll, getImageResult, getCopywritingResult, getTaskDetail } from '@/api/ai/generate'

export default {
  name: 'AiGenerate',
  data() {
    return {
      form: {
        description: '',
        imageParams: {
          size: '1024x1024',
          style: 'realistic',
          count: '1'
        }
      },
      fileList: [],
      loading: {
        generateImage: false,
        generateCopywriting: false,
        generateAll: false
      },
      imageResults: [],
      copywritingResult: '',
      taskId: null
    }
  },
  methods: {
    // 处理图片上传
    handleImageChange(file, fileList) {
      this.fileList = fileList;
    },
    
    // 处理图片删除
    handleImageRemove(file, fileList) {
      this.fileList = fileList;
    },
    
    // 生成图片
    async generateImage() {
      if (!this.form.description) {
        this.$message.error('请输入文字描述');
        return;
      }
      
      this.loading.generateImage = true;
      try {
        // 创建任务
        const taskData = {
          taskName: 'AI生图任务',
          description: this.form.description,
          imageParams: JSON.stringify(this.form.imageParams)
        };
        
        // 如果有上传的参考图片，转换为Base64并添加到任务数据中
        if (this.fileList && this.fileList.length > 0) {
          const file = this.fileList[0].raw || this.fileList[0];
          const base64Image = await this.convertFileToBase64(file);
          taskData.referenceImage = base64Image;
          
          // 图生图模式下，移除 size 参数（因为输出尺寸由输入图片决定）
          if (this.form.imageParams.size) {
            delete this.form.imageParams.size;
            taskData.imageParams = JSON.stringify(this.form.imageParams);
          }
        }
        
        // 调用后端API创建任务
        const taskResponse = await createTask(taskData);
        console.log('[AI生成] 创建任务响应:', taskResponse);
        
        // 检查响应是否成功
        if (!taskResponse || !taskResponse.data) {
          throw new Error('创建任务失败：响应数据异常');
        }
        
        this.taskId = taskResponse.data;
        console.log('[AI生成] 任务ID:', this.taskId);
        
        if (!this.taskId) {
          throw new Error('创建任务失败：未获取到任务ID');
        }
        
        // 调用生成图片API
        await generateImage(this.taskId);
        
        // 轮询等待任务完成（最多等待60秒）
        let retryCount = 0;
        const maxRetries = 12; // 12次 * 5秒 = 60秒
        
        while (retryCount < maxRetries) {
          // 等待5秒
          await new Promise(resolve => setTimeout(resolve, 5000));
          
          // 查询任务状态
          const taskDetailResponse = await getTaskDetail(this.taskId);
          
          // 检查响应数据是否存在
          if (!taskDetailResponse || !taskDetailResponse.data) {
            console.error('任务详情响应数据异常:', taskDetailResponse);
            continue;
          }
          
          const taskStatus = taskDetailResponse.data.status;
          
          if (taskStatus === 2) {
            // 成功，获取结果
            const resultResponse = await getImageResult(this.taskId);
            if (resultResponse.data && resultResponse.data.length > 0) {
              // 处理图片URL，移除可能的反引号
              this.imageResults = resultResponse.data.map(item => item.resultContent.replace(/`/g, ''));
              this.loading.generateImage = false;
              this.$message.success('图片生成成功');
              return;
            }
          } else if (taskStatus === 3) {
            // 失败
            const errorMsg = taskDetailResponse.data.errorMessage || '生成失败';
            this.loading.generateImage = false;
            this.$message.error('图片生成失败：' + errorMsg);
            return;
          }
          
          retryCount++;
        }
        
        // 超时
        this.loading.generateImage = false;
        this.$message.warning('生成超时，请稍后在历史记录中查看');
      } catch (error) {
        this.loading.generateImage = false;
        this.$message.error('图片生成失败：' + error.message);
      }
    },
    
    // 生成文案
    async generateCopywriting() {
      if (!this.form.description) {
        this.$message.error('请输入文字描述');
        return;
      }
      
      this.loading.generateCopywriting = true;
      try {
        // 如果没有任务ID，先创建任务
        if (!this.taskId) {
          const taskData = {
            taskName: 'AI文案任务',
            description: this.form.description
          };
          
          // 调用后端API创建任务
          const taskResponse = await createTask(taskData);
          console.log('[AI生成] 创建任务响应:', taskResponse);
          
          // 检查响应是否成功
          if (!taskResponse || !taskResponse.data) {
            throw new Error('创建任务失败：响应数据异常');
          }
          
          this.taskId = taskResponse.data;
          console.log('[AI生成] 任务ID:', this.taskId);
          
          if (!this.taskId) {
            throw new Error('创建任务失败：未获取到任务ID');
          }
        }
        
        // 调用生成文案API
        await generateCopywriting(this.taskId);
        
        // 获取生成结果
        const resultResponse = await getCopywritingResult(this.taskId);
        if (resultResponse.data.length > 0) {
          this.copywritingResult = resultResponse.data[0].resultContent;
        }
        
        this.loading.generateCopywriting = false;
        this.$message.success('文案生成成功');
      } catch (error) {
        this.loading.generateCopywriting = false;
        this.$message.error('文案生成失败：' + error.message);
      }
    },
    
    // 一键生成
    async generateAll() {
      if (!this.form.description) {
        this.$message.error('请输入文字描述');
        return;
      }
      
      this.loading.generateAll = true;
      try {
        // 创建任务
        const taskData = {
          taskName: 'AI一键生成任务',
          description: this.form.description,
          imageParams: JSON.stringify(this.form.imageParams)
        };
        
        // 如果有上传的参考图片，转换为Base64并添加到任务数据中
        if (this.fileList && this.fileList.length > 0) {
          const file = this.fileList[0].raw || this.fileList[0];
          const base64Image = await this.convertFileToBase64(file);
          taskData.referenceImage = base64Image;
          
          // 图生图模式下，移除 size 参数
          if (this.form.imageParams.size) {
            delete this.form.imageParams.size;
            taskData.imageParams = JSON.stringify(this.form.imageParams);
          }
        }
        
        // 调用后端API创建任务
        const taskResponse = await createTask(taskData);
        console.log('[AI生成] 创建任务响应:', taskResponse);
        
        // 检查响应是否成功
        if (!taskResponse || !taskResponse.data) {
          throw new Error('创建任务失败：响应数据异常');
        }
        
        this.taskId = taskResponse.data;
        console.log('[AI生成] 任务ID:', this.taskId);
        
        if (!this.taskId) {
          throw new Error('创建任务失败：未获取到任务ID');
        }
        
        // 调用一键生成API
        await generateAll(this.taskId);
        
        // 轮询等待任务完成（最多等待60秒）
        let retryCount = 0;
        const maxRetries = 12;
        
        while (retryCount < maxRetries) {
          await new Promise(resolve => setTimeout(resolve, 5000));
          
          const taskDetailResponse = await getTaskDetail(this.taskId);
          
          // 检查响应数据是否存在
          if (!taskDetailResponse || !taskDetailResponse.data) {
            console.error('任务详情响应数据异常:', taskDetailResponse);
            continue;
          }
          
          const taskStatus = taskDetailResponse.data.status;
          
          if (taskStatus === 2) {
            // 成功，获取结果
            const imageResultResponse = await getImageResult(this.taskId);
            if (imageResultResponse.data && imageResultResponse.data.length > 0) {
              // 处理图片URL，移除可能的反引号
              this.imageResults = imageResultResponse.data.map(item => item.resultContent.replace(/`/g, ''));
            }
            
            const copywritingResultResponse = await getCopywritingResult(this.taskId);
            if (copywritingResultResponse.data && copywritingResultResponse.data.length > 0) {
              this.copywritingResult = copywritingResultResponse.data[0].resultContent;
            }
            
            this.loading.generateAll = false;
            this.$message.success('一键生成成功');
            return;
          } else if (taskStatus === 3) {
            const errorMsg = taskDetailResponse.data.errorMessage || '生成失败';
            this.loading.generateAll = false;
            this.$message.error('生成失败：' + errorMsg);
            return;
          }
          
          retryCount++;
        }
        
        this.loading.generateAll = false;
        this.$message.warning('生成超时，请稍后在历史记录中查看');
      } catch (error) {
        this.loading.generateAll = false;
        this.$message.error('一键生成失败：' + error.message);
      }
    },
    
    // 下载图片
    downloadImage(imageUrl) {
      const link = document.createElement('a');
      link.href = imageUrl;
      link.download = 'ai-generated-image.jpg';
      link.click();
    },
    
    // 删除图片
    removeImage(index) {
      this.imageResults.splice(index, 1);
    },
    
    // 复制文案
    copyCopywriting() {
      const textarea = document.createElement('textarea');
      textarea.value = this.copywritingResult;
      document.body.appendChild(textarea);
      textarea.select();
      document.execCommand('copy');
      document.body.removeChild(textarea);
      this.$message.success('文案复制成功');
    },
    
    // 将文件转换为Base64
    convertFileToBase64(file) {
      return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.readAsDataURL(file);
        reader.onload = () => resolve(reader.result);
        reader.onerror = error => reject(error);
      });
    }
  }
}
</script>

<style scoped>
.ai-generate-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.input-section {
  margin-bottom: 30px;
}

.action-section {
  margin-bottom: 30px;
  display: flex;
  gap: 10px;
}

.result-section {
  margin-top: 30px;
}

.result-card {
  margin-bottom: 20px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.image-results {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-top: 20px;
}

.image-item {
  width: 200px;
  text-align: center;
}

.image-item img {
  width: 100%;
  height: 200px;
  object-fit: cover;
  border-radius: 4px;
}

.image-actions {
  margin-top: 10px;
  display: flex;
  justify-content: center;
  gap: 10px;
}

.copywriting-result {
  margin-top: 20px;
}

.copywriting-actions {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .image-results {
    justify-content: center;
  }
  
  .action-section {
    flex-direction: column;
  }
}
</style>