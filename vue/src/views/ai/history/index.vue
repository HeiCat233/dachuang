<template>
  <div class="ai-history-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>AI生成历史记录</span>
        </div>
      </template>
      
      <!-- 搜索和筛选 -->
      <div class="search-section">
        <el-form :inline="true" :model="searchForm" class="demo-form-inline">
          <el-form-item label="任务名称">
            <el-input v-model="searchForm.taskName" placeholder="请输入任务名称" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="searchForm.status" placeholder="请选择状态">
              <el-option label="全部" value="" />
              <el-option label="待处理" value="0" />
              <el-option label="处理中" value="1" />
              <el-option label="成功" value="2" />
              <el-option label="失败" value="3" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="search">查询</el-button>
            <el-button @click="reset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
      
      <!-- 历史记录列表 -->
      <div class="history-list">
        <el-table :data="historyList" style="width: 100%">
          <el-table-column prop="id" label="任务ID" width="80" />
          <el-table-column prop="taskName" label="任务名称" />
          <el-table-column prop="description" label="文字描述" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="getTagType(scope.row.status)">
                {{ getStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180" />
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="scope">
              <el-button size="small" @click="viewDetails(scope.row.id)">查看详情</el-button>
              <el-button size="small" type="danger" @click="deleteTask(scope.row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        
        <!-- 分页 -->
        <div class="pagination">
          <el-pagination
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
            :current-page="currentPage"
            :page-sizes="[10, 20, 50, 100]"
            :page-size="pageSize"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
          />
        </div>
      </div>
      
      <!-- 详情对话框 -->
      <el-dialog
        v-model="dialogVisible"
        title="任务详情"
        width="80%"
      >
        <div v-if="taskDetails">
          <el-form :model="taskDetails" label-width="100px">
            <el-form-item label="任务名称">
              {{ taskDetails.taskName }}
            </el-form-item>
            <el-form-item label="文字描述">
              {{ taskDetails.description }}
            </el-form-item>
            <el-form-item label="参考图片">
              <el-image
                v-if="taskDetails.referenceImage"
                :src="taskDetails.referenceImage"
                fit="cover"
                style="width: 200px; height: 200px;"
              />
              <span v-else>无</span>
            </el-form-item>
            <el-form-item label="生图参数">
              <pre>{{ formatImageParams(taskDetails.imageParams) }}</pre>
            </el-form-item>
            <el-form-item label="状态">
              <el-tag :type="getTagType(taskDetails.status)">
                {{ getStatusText(taskDetails.status) }}
              </el-tag>
            </el-form-item>
            <el-form-item label="错误信息" v-if="taskDetails.errorMessage">
              {{ taskDetails.errorMessage }}
            </el-form-item>
            <el-form-item label="创建时间">
              {{ taskDetails.createTime }}
            </el-form-item>
          </el-form>
          
          <!-- 生成结果 -->
          <div class="result-details" v-if="taskResults.length > 0">
            <h3>生成结果</h3>
            
            <!-- 图片结果 -->
            <div v-if="imageResults.length > 0" class="result-section">
              <h4>图片结果</h4>
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
                  </div>
                </div>
              </div>
            </div>
            
            <!-- 文案结果 -->
            <div v-if="copywritingResults.length > 0" class="result-section">
              <h4>文案结果</h4>
              <div v-for="(copywriting, index) in copywritingResults" :key="index" class="copywriting-item">
                <el-input
                  type="textarea"
                  :value="copywriting"
                  placeholder="文案内容"
                  :rows="4"
                  readonly
                />
                <div class="copywriting-actions">
                  <el-button size="small" type="primary" @click="copyCopywriting(copywriting)">
                    复制文案
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div v-else>
          加载中...
        </div>
      </el-dialog>
    </el-card>
  </div>
</template>

<script>
import { getTaskList, getTaskDetail, getResultList, deleteTask, getImageResult, getCopywritingResult } from '@/api/ai/generate'

export default {
  name: 'AiHistory',
  data() {
    return {
      searchForm: {
        taskName: '',
        status: ''
      },
      historyList: [],
      currentPage: 1,
      pageSize: 10,
      total: 0,
      dialogVisible: false,
      taskDetails: null,
      taskResults: [],
      imageResults: [],
      copywritingResults: []
    }
  },
  mounted() {
    this.loadHistoryList();
  },
  methods: {
    // 加载历史记录列表
    async loadHistoryList() {
      try {
        // 调用后端API获取历史记录
        const response = await getTaskList({
          page: this.currentPage,
          pageSize: this.pageSize,
          taskName: this.searchForm.taskName,
          status: this.searchForm.status
        });
        this.historyList = response.data.list;
        this.total = response.data.total;
      } catch (error) {
        this.$message.error('获取历史记录失败：' + error.message);
      }
    },
    
    // 搜索
    search() {
      this.currentPage = 1;
      this.loadHistoryList();
    },
    
    // 重置
    reset() {
      this.searchForm = {
        taskName: '',
        status: ''
      };
      this.currentPage = 1;
      this.loadHistoryList();
    },
    
    // 分页大小变化
    handleSizeChange(size) {
      this.pageSize = size;
      this.loadHistoryList();
    },
    
    // 当前页码变化
    handleCurrentChange(current) {
      this.currentPage = current;
      this.loadHistoryList();
    },
    
    // 获取状态文本
    getStatusText(status) {
      const statusMap = {
        0: '待处理',
        1: '处理中',
        2: '成功',
        3: '失败'
      };
      return statusMap[status] || '未知';
    },
    
    // 获取标签类型
    getTagType(status) {
      const typeMap = {
        0: 'info',
        1: 'warning',
        2: 'success',
        3: 'danger'
      };
      return typeMap[status] || 'info';
    },
    
    // 查看详情
    async viewDetails(taskId) {
      try {
        // 调用后端API获取任务详情
        const taskResponse = await getTaskDetail(taskId);
        this.taskDetails = taskResponse.data;
        
        // 调用后端API获取图片生成结果
        const imageResultResponse = await getImageResult(taskId);
        this.imageResults = imageResultResponse.data.map(item => item.resultContent);
        
        // 调用后端API获取文案生成结果
        const copywritingResultResponse = await getCopywritingResult(taskId);
        this.copywritingResults = copywritingResultResponse.data.map(item => item.resultContent);
        
        this.dialogVisible = true;
      } catch (error) {
        this.$message.error('获取任务详情失败：' + error.message);
      }
    },
    
    // 删除任务
    async deleteTask(taskId) {
      this.$confirm('确定要删除这个任务吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          // 调用后端API删除任务
          await deleteTask(taskId);
          
          this.$message.success('删除成功');
          this.loadHistoryList();
        } catch (error) {
          this.$message.error('删除失败：' + error.message);
        }
      }).catch(() => {
        // 取消删除
      });
    },
    
    // 格式化图片参数
    formatImageParams(params) {
      if (!params) return '';
      try {
        const obj = JSON.parse(params);
        return JSON.stringify(obj, null, 2);
      } catch (e) {
        return params;
      }
    },
    
    // 下载图片
    downloadImage(imageUrl) {
      const link = document.createElement('a');
      link.href = imageUrl;
      link.download = 'ai-generated-image.jpg';
      link.click();
    },
    
    // 复制文案
    copyCopywriting(copywriting) {
      const textarea = document.createElement('textarea');
      textarea.value = copywriting;
      document.body.appendChild(textarea);
      textarea.select();
      document.execCommand('copy');
      document.body.removeChild(textarea);
      this.$message.success('文案复制成功');
    }
  }
}
</script>

<style scoped>
.ai-history-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-section {
  margin-bottom: 20px;
}

.history-list {
  margin-top: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.result-details {
  margin-top: 30px;
}

.result-section {
  margin-top: 20px;
  padding: 10px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}

.image-results {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-top: 10px;
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
}

.copywriting-item {
  margin-top: 10px;
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
}
</style>