import request from '@/utils/request'

// AI内容生成相关API

// 创建生成任务
export function createTask(task) {
  return request({
    url: '/ai/generate/createTask',
    method: 'post',
    data: task
  })
}

// 生成图片
export function generateImage(taskId) {
  return request({
    url: `/ai/generate/generateImage/${taskId}`,
    method: 'post'
  })
}

// 生成文案
export function generateCopywriting(taskId) {
  return request({
    url: `/ai/generate/generateCopywriting/${taskId}`,
    method: 'post'
  })
}

// 一键生成（图片+文案）
export function generateAll(taskId) {
  return request({
    url: `/ai/generate/generateAll/${taskId}`,
    method: 'post'
  })
}

// 获取任务列表
export function getTaskList(params) {
  return request({
    url: '/ai/generate/taskList',
    method: 'get',
    params
  })
}

// 获取任务详情
export function getTaskDetail(taskId) {
  return request({
    url: `/ai/generate/taskDetail/${taskId}`,
    method: 'get'
  })
}

// 获取生成结果列表
export function getResultList(taskId) {
  return request({
    url: `/ai/generate/resultList/${taskId}`,
    method: 'get'
  })
}

// 获取图片生成结果
export function getImageResult(taskId) {
  return request({
    url: `/ai/generate/imageResult/${taskId}`,
    method: 'get'
  })
}

// 获取文案生成结果
export function getCopywritingResult(taskId) {
  return request({
    url: `/ai/generate/copywritingResult/${taskId}`,
    method: 'get'
  })
}

// 删除任务
export function deleteTask(taskId) {
  return request({
    url: `/ai/generate/deleteTask/${taskId}`,
    method: 'delete'
  })
}