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
              <el-button type="text" @click="imageResults = []">清空</el-button>
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
                <el-button size="mini" type="primary" icon="el-icon-download" @click="downloadImage(image)"></el-button>
                <el-button size="mini" type="danger" icon="el-icon-delete" @click="removeImage(index)"></el-button>
              </div>
            </div>
          </div>
        </el-card>
        
        <!-- 文案结果 -->
        <el-card v-if="copywritingResults.length > 0" class="result-card">
          <template #header>
            <div class="result-header">
              <span>文案生成结果</span>
              <el-button type="text" @click="copywritingResults = []">清空</el-button>
            </div>
          </template>
          <div class="copywriting-results">
            <div v-for="(item, index) in copywritingResults" :key="index" class="copywriting-item">
              <div class="copywriting-meta">
                <span class="copywriting-time">{{ item.time }}</span>
                <span class="copywriting-prompt" :title="item.prompt">提示词: {{ item.prompt }}</span>
              </div>
              <el-input
                type="textarea"
                v-model="item.content"
                :rows="4"
                readonly
              />
              <div class="copywriting-actions">
                <el-button size="mini" type="primary" @click="copyText(item.content)">
                  复制
                </el-button>
                <el-button size="mini" type="danger" @click="copywritingResults.splice(index, 1)">
                  删除
                </el-button>
              </div>
              <el-divider v-if="index < copywritingResults.length - 1"></el-divider>
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
      copywritingResults: [], // 修改为数组以存储多次生成的结果
      taskId: null,
      lastDescription: '' // 记录上次生成时的描述
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
        // 记录当前生成时的描述
        this.lastDescription = this.form.description;
        
        // 每次点击生成图片都创建一个新任务，以确保能获得不同的结果
        const taskData = {
          taskName: 'AI生图任务-' + new Date().getTime(),
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
            const params = JSON.parse(JSON.stringify(this.form.imageParams));
            delete params.size;
            taskData.imageParams = JSON.stringify(params);
          }
        }
        
        // 调用后端API创建任务
        const taskResponse = await createTask(taskData);
        const currentTaskId = taskResponse.data;
        this.taskId = currentTaskId;
        
        // 调用生成图片API
        await generateImage(currentTaskId);
        
        // 轮询等待任务完成
        let retryCount = 0;
        const maxRetries = 24; // 24次 * 5秒 = 120秒 (匹配后端超时)
        
        while (retryCount < maxRetries) {
          await new Promise(resolve => setTimeout(resolve, 5000));
          const taskDetailResponse = await getTaskDetail(currentTaskId);
          
          if (!taskDetailResponse || !taskDetailResponse.data) continue;
          
          const taskStatus = taskDetailResponse.data.status;
          if (taskStatus === 2) {
            const resultResponse = await getImageResult(currentTaskId);
            if (resultResponse.data && resultResponse.data.length > 0) {
              const newImages = resultResponse.data.map(item => item.resultContent.replace(/`/g, ''));
              // 使用 unshift 将新结果添加到列表最前面，实现“重新显示并保留历史”
              this.imageResults = [...newImages, ...this.imageResults];
              this.loading.generateImage = false;
              this.$message.success('图片生成成功');
              return;
            }
          } else if (taskStatus === 3) {
            throw new Error(taskDetailResponse.data.errorMessage || '生成失败');
          }
          retryCount++;
        }
        throw new Error('生成超时，请稍后在历史记录中查看');
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
        // 如果描述发生了变化，或者当前没有任务ID，则创建新任务
        if (!this.taskId || this.form.description !== this.lastDescription) {
          const taskData = {
            taskName: 'AI文案任务-' + new Date().getTime(),
            description: this.form.description
          };
          const taskResponse = await createTask(taskData);
          this.taskId = taskResponse.data;
          this.lastDescription = this.form.description;
        }
        
        // 调用生成文案API
        await generateCopywriting(this.taskId);
        
        // 获取生成结果
        const resultResponse = await getCopywritingResult(this.taskId);
        if (resultResponse.data && resultResponse.data.length > 0) {
          const newCopy = resultResponse.data[0].resultContent;
          // 将新文案添加到列表最前面
          this.copywritingResults.unshift({
            content: newCopy,
            time: new Date().toLocaleString(),
            prompt: this.form.description
          });
        }
        
        this.loading.generateCopywriting = false;
        this.$message.success('文案生成成功');
      } catch (error) {
        this.loading.generateCopywriting = false;
        this.$message.error('文案生成失败：' + error.message);
      }
    },
    
    // 一键生成（图片+文案）
    async generateAll() {
      if (!this.form.description) {
        this.$message.error('请输入文字描述');
        return;
      }
      
      this.loading.generateAll = true;
      try {
        this.lastDescription = this.form.description;
        
        // 创建新任务
        const taskData = {
          taskName: 'AI一键生成-' + new Date().getTime(),
          description: this.form.description,
          imageParams: JSON.stringify(this.form.imageParams)
        };
        
        // 如果有上传的参考图片
        if (this.fileList && this.fileList.length > 0) {
          const file = this.fileList[0].raw || this.fileList[0];
          const base64Image = await this.convertFileToBase64(file);
          taskData.referenceImage = base64Image;
          
          if (this.form.imageParams.size) {
            const params = JSON.parse(JSON.stringify(this.form.imageParams));
            delete params.size;
            taskData.imageParams = JSON.stringify(params);
          }
        }
        
        // 调用后端API创建任务
        const taskResponse = await createTask(taskData);
        const currentTaskId = taskResponse.data;
        this.taskId = currentTaskId;
        
        // 调用一键生成API
        await generateAll(currentTaskId);
        
        // 轮询等待任务完成
        let retryCount = 0;
        const maxRetries = 24; // 120秒
        
        while (retryCount < maxRetries) {
          await new Promise(resolve => setTimeout(resolve, 5000));
          const taskDetailResponse = await getTaskDetail(currentTaskId);
          
          if (!taskDetailResponse || !taskDetailResponse.data) continue;
          
          const taskStatus = taskDetailResponse.data.status;
          
          if (taskStatus === 2) {
            // 成功，分别获取图片和文案结果
            const imageResultResponse = await getImageResult(currentTaskId);
            if (imageResultResponse.data && imageResultResponse.data.length > 0) {
              const newImages = imageResultResponse.data.map(item => item.resultContent.replace(/`/g, ''));
              this.imageResults = [...newImages, ...this.imageResults];
            }
            
            const copywritingResultResponse = await getCopywritingResult(currentTaskId);
            if (copywritingResultResponse.data && copywritingResultResponse.data.length > 0) {
              const newCopy = copywritingResultResponse.data[0].resultContent;
              this.copywritingResults.unshift({
                content: newCopy,
                time: new Date().toLocaleString(),
                prompt: this.form.description
              });
            }
            
            this.loading.generateAll = false;
            this.$message.success('一键生成成功');
            return;
          } else if (taskStatus === 3) {
            throw new Error(taskDetailResponse.data.errorMessage || '生成失败');
          }
          retryCount++;
        }
        throw new Error('生成超时，请稍后在历史记录中查看');
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
    
    // 复制文字
    copyText(text) {
      const textarea = document.createElement('textarea');
      textarea.value = text;
      document.body.appendChild(textarea);
      textarea.select();
      document.execCommand('copy');
      document.body.removeChild(textarea);
      this.$message.success('复制成功');
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
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
}

.image-item {
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
}

.image-item .el-image {
  width: 100%;
  height: 200px;
  display: block;
}

.image-actions {
  padding: 10px;
  display: flex;
  justify-content: center;
  gap: 10px;
  background: rgba(255,255,255,0.9);
}

.copywriting-item {
  margin-bottom: 20px;
}

.copywriting-meta {
  margin-bottom: 10px;
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #999;
}

.copywriting-prompt {
  max-width: 70%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.copywriting-actions {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
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