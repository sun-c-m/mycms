<script setup lang="ts">
import {nextTick, onMounted, reactive, ref, shallowRef} from "vue";
import '@wangeditor/editor/dist/css/style.css'
import {Editor, Toolbar} from '@wangeditor/editor-for-vue'

import {
  ElMessageBox,
  ElMessage,
  type FormInstance,
  type FormRules,
  type UploadFile,
  type UploadFiles,
} from "element-plus";

import type {IDomEditor, IEditorConfig, IToolbarConfig} from "@wangeditor/editor";
import {approveNews, createNewsWithFiles, deleteNews, getNewsPage, rejectNews, updateNewsWithFiles} from "@/http/news.ts";

interface NewsForm {
  title: string
  category: string
  supplier: string
  reviewer: string
  content: string
  attachments: NewsAttachment[]
}

interface NewsAttachment {
  name: string
  url: string
  size?: number
  type?: string
}

interface NewsItem extends NewsForm {
  id: string
  status: string
  createTime?: string
  updateTime?: string
  publishTime?: string
}

const emptyForm = (): NewsForm => ({
  title: '',
  category: '学院新闻',
  supplier: '',
  reviewer: '',
  content: '',
  attachments: [],
})


const rules: FormRules<NewsForm> = {
  title: [{required: true, message: '请输入新闻标题', trigger: 'blur'}],
  category: [{required: true, message: '请选择栏目', trigger: 'change'}],
  content: [{required: true, message: '请输入新闻正文', trigger: 'change'}],
}

const categoryOptions = ['学院新闻', '通知公告', '学术活动', '学工新闻']
const formRef = ref<FormInstance>()
const editorRef = shallowRef<IDomEditor>()
const editingId = ref<string | null>(null)
const newsForm = ref<NewsForm>(emptyForm())
const dialogVisible = ref(false)
const shouldAutoScrollEditor = ref(false)
const removeEditorListeners = ref<(() => void) | null>(null)
let editorMutationObserver: MutationObserver | null = null
const editorWrapperRef = ref<HTMLElement>()
const submitting = ref(false)
const tableLoading = ref(false)
const newsList = ref<NewsItem[]>([])
const attachmentFiles = ref<UploadFile[]>([])
const editingAttachments = ref<NewsAttachment[]>([])
const previewVisible = ref(false)
const previewNews = ref<NewsItem | null>(null)
const auditingIds = ref<string[]>([])
const deletingIds = ref<string[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const queryForm = reactive({
  keyword: '',
  category: '',
  status: '',
})
const maxAttachmentSize = 20 * 1024 * 1024

const toolbarConfig: Partial<IToolbarConfig> = {
  excludeKeys: ['fullScreen'],
}
const editorConfig: Partial<IEditorConfig> = {
  placeholder: '请输入新闻正文内容...',
  MENU_CONF: {
    uploadImage: {
      base64LimitSize: 5 * 1024 * 1024,
    },
  },
}
const openAddDialog = () => openNewsDialog()
const statusTextMap: Record<string, string> = {
  PENDING_REVIEW: '待审核',
  PUBLISHED: '已发布',
  REJECTED: '已驳回',
}
const statusTagMap: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
  PENDING_REVIEW: 'warning',
  PUBLISHED: 'success',
  REJECTED: 'danger',
}

const getStatusText = (status: string) => statusTextMap[status] || status || '-'
const getStatusTagType = (status: string) => statusTagMap[status] || 'info'
const formatDateTime = (value?: string) => {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}
const stripHtml = (html: string) => {
  return html
      .replace(/<[^>]+>/g, '')
      .replace(/&nbsp;/g, ' ')
      .replace(/\s+/g, ' ')
      .trim()
}
const getContentSummary = (html: string, maxLength = 60) => {
  const text = stripHtml(html)
  if (!text) return '-'
  return text.length > maxLength ? `${text.slice(0, maxLength)}...` : text
}
const getAttachmentUrl = (url: string) => {
  if (!url) return '#'
  if (/^https?:\/\//i.test(url)) return url

  const baseUrl = import.meta.env.VITE_API_BASE_URL || ''
  return `${baseUrl}${url.startsWith('/') ? url : `/${url}`}`
}
const getAttachmentExtension = (attachment: NewsAttachment) => {
  const source = attachment.name || attachment.url || ''
  const cleanSource = source.split('?')[0]?.split('#')[0] || ''
  const dotIndex = cleanSource.lastIndexOf('.')
  return dotIndex === -1 ? '' : cleanSource.slice(dotIndex + 1).toLowerCase()
}
const isImageAttachment = (attachment: NewsAttachment) => {
  const extension = getAttachmentExtension(attachment)
  return Boolean(attachment.type?.startsWith('image/')) || ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'svg'].includes(extension)
}
const isPdfAttachment = (attachment: NewsAttachment) => {
  return attachment.type === 'application/pdf' || getAttachmentExtension(attachment) === 'pdf'
}
const isPreviewableAttachment = (attachment: NewsAttachment) => {
  return isImageAttachment(attachment) || isPdfAttachment(attachment)
}
const getAttachmentTypeText = (attachment: NewsAttachment) => {
  const extension = getAttachmentExtension(attachment)
  if (isImageAttachment(attachment)) return '图片'
  if (isPdfAttachment(attachment)) return 'PDF'
  if (['doc', 'docx'].includes(extension)) return 'Word'
  if (['xls', 'xlsx'].includes(extension)) return 'Excel'
  if (['ppt', 'pptx'].includes(extension)) return 'PPT'
  return extension ? extension.toUpperCase() : '附件'
}
const formatFileSize = (size?: number) => {
  if (!size) return ''
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}
const toAttachmentFiles = (attachments: NewsAttachment[]) => {
  return attachments.map((attachment, index) => ({
    name: attachment.name,
    url: attachment.url,
    size: attachment.size,
    status: 'success',
    uid: Date.now() + index,
  })) as UploadFile[]
}
const openAttachmentPreview = (attachment: NewsAttachment) => {
  window.open(getAttachmentUrl(attachment.url), '_blank', 'noopener,noreferrer')
}
const getNewsList = async () => {
  tableLoading.value = true
  try {
    const result: any = await getNewsPage({
      currentPage: currentPage.value,
      pageSize: pageSize.value,
      params: [
        {name: 'keyword', value: queryForm.keyword},
        {name: 'category', value: queryForm.category},
        {name: 'status', value: queryForm.status},
      ],
    })

    newsList.value = (result.data?.records || []).map((item: NewsItem) => ({
      ...item,
      attachments: Array.isArray(item.attachments) ? item.attachments : [],
    }))
    total.value = Number(result.data?.total || 0)
  } catch (error: any) {
    showRequestError(error, '新闻列表加载失败')
  } finally {
    tableLoading.value = false
  }
}
const handleSearch = () => {
  currentPage.value = 1
  getNewsList()
}
const resetSearch = () => {
  queryForm.keyword = ''
  queryForm.category = ''
  queryForm.status = ''
  handleSearch()
}
const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  getNewsList()
}
const handleCurrentChange = (page: number) => {
  currentPage.value = page
  getNewsList()
}
const syncAttachments = (files: UploadFiles) => {
  attachmentFiles.value = [...files]
  newsForm.value.attachments = files
      .filter(file => !file.raw && file.url)
      .map(file => {
        const savedAttachment = editingAttachments.value.find(attachment => attachment.url === file.url)
        return {
          name: file.name,
          url: savedAttachment?.url || file.url || '',
          size: file.size ?? savedAttachment?.size,
          type: savedAttachment?.type,
        }
      })
}
const handleAttachmentChange = (file: UploadFile, files: UploadFiles) => {
  const size = file.raw?.size ?? file.size ?? 0
  if (size > maxAttachmentSize) {
    ElMessage.warning(`${file.name} 超过 20MB，不能上传`)
    syncAttachments(files.filter(item => item.uid !== file.uid) as UploadFiles)
    return
  }

  syncAttachments(files)
}
const handleAttachmentRemove = (_file: UploadFile, files: UploadFiles) => {
  syncAttachments(files)
}
const buildNewsFormData = () => {
  const formData = new FormData()
  formData.append('news', new Blob([JSON.stringify({
    ...newsForm.value,
    attachments: [...newsForm.value.attachments],
  })], {type: 'application/json'}))
  attachmentFiles.value.forEach(file => {
    if (file.raw) {
      formData.append('files', file.raw)
    }
  })
  return formData
}
const openPreview = (row: NewsItem) => {
  previewNews.value = row
  previewVisible.value = true
}
const isAuditing = (id: string) => auditingIds.value.includes(id)
const isDeleting = (id: string) => deletingIds.value.includes(id)
const canAudit = (row: NewsItem) => row.status === 'PENDING_REVIEW'
const getAuditDisabledReason = (row: NewsItem) => canAudit(row) ? '' : `${getStatusText(row.status)}的新闻不能重复审核`
const handleDeleteNews = async (row: NewsItem) => {
  try {
    await ElMessageBox.confirm(`确认删除“${row.title}”吗？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  deletingIds.value.push(row.id)
  try {
    const result: any = await deleteNews(row.id)
    ElMessage.success(result.message || '新闻已删除')

    if (newsList.value.length === 1 && currentPage.value > 1) {
      currentPage.value -= 1
    }
    await getNewsList()
  } catch (error: any) {
    showRequestError(error, '新闻删除失败')
  } finally {
    deletingIds.value = deletingIds.value.filter(id => id !== row.id)
  }
}
const auditNews = async (row: NewsItem, action: 'approve' | 'reject') => {
  const actionText = action === 'approve' ? '通过' : '驳回'
  try {
    await ElMessageBox.confirm(`确认${actionText}这条新闻吗？`, '审核确认', {
      confirmButtonText: actionText,
      cancelButtonText: '取消',
      type: action === 'approve' ? 'success' : 'warning',
    })
  } catch {
    return
  }

  auditingIds.value.push(row.id)
  try {
    const result: any = action === 'approve'
        ? await approveNews(row.id)
        : await rejectNews(row.id)

    ElMessage.success(result.message || `新闻已${actionText}`)
    await getNewsList()
  } catch (error: any) {
    showRequestError(error, `新闻${actionText}失败`)
  } finally {
    auditingIds.value = auditingIds.value.filter(id => id !== row.id)
  }
}

const openNewsDialog = (row?: NewsItem) => {
  // 有 row 表示编辑，没有 row 表示新增；编辑时记录当前新闻 id。
  editingId.value = row?.id || null
  // 打开弹窗时先关闭正文自动滚动，避免编辑旧内容时光标/视图跳动。
  shouldAutoScrollEditor.value = false
  // 编辑时回填当前行数据；新增时恢复为空表单。
  Object.assign(newsForm.value, row ? {
    title: row.title,
    category: row.category,
    supplier: row.supplier || '',
    reviewer: row.reviewer || '',
    content: row.content,
    attachments: row.attachments || [],
  } : emptyForm())
  editingAttachments.value = row?.attachments || []
  attachmentFiles.value = row ? toAttachmentFiles(editingAttachments.value) : []
  // 清掉上一次打开弹窗残留的表单校验提示。
  formRef.value?.clearValidate()
  // 最后显示弹窗。
  dialogVisible.value = true
}

const getEditorScrollContainer = () => {
  return editorWrapperRef.value?.querySelector<HTMLElement>('.w-e-scroll')
}
const bindEditorAutoScroll = async () => {
  removeEditorListeners.value?.()
  editorMutationObserver?.disconnect()

  await nextTick()

  const scrollContainer = getEditorScrollContainer()
  const editable = editorWrapperRef.value?.querySelector<HTMLElement>('[data-slate-editor]')

  if (!scrollContainer || !editable) return

  const handleInput = () => {
    if (!shouldAutoScrollEditor.value) return

    requestAnimationFrame(() => {
      scrollContainer.scrollTop = scrollContainer.scrollHeight
    })
  }

  const enableAutoScroll = () => {
    shouldAutoScrollEditor.value = true
  }

  editable.addEventListener('compositionstart', enableAutoScroll)
  editable.addEventListener('beforeinput', enableAutoScroll)
  editable.addEventListener('input', handleInput)
  editable.addEventListener('keyup', handleInput)
  editable.addEventListener('paste', handleInput)
  editorMutationObserver = new MutationObserver(handleInput)
  editorMutationObserver.observe(editable, {
    childList: true,
    characterData: true,
    subtree: true,
  })

  removeEditorListeners.value = () => {
    editable.removeEventListener('compositionstart', enableAutoScroll)
    editable.removeEventListener('beforeinput', enableAutoScroll)
    editable.removeEventListener('input', handleInput)
    editable.removeEventListener('keyup', handleInput)
    editable.removeEventListener('paste', handleInput)
    editorMutationObserver?.disconnect()
    editorMutationObserver = null
  }
}
const handleEditorCreated = (editor: IDomEditor) => {
  editorRef.value = editor
  bindEditorAutoScroll()
}
const scrollEditorToBottom = async () => {
  if (!shouldAutoScrollEditor.value) return

  await nextTick()

  const scrollToBottom = () => {
    const scrollContainer = getEditorScrollContainer()
    if (!scrollContainer) return

    scrollContainer.scrollTop = scrollContainer.scrollHeight
  }

  requestAnimationFrame(scrollToBottom)
  setTimeout(scrollToBottom, 0)
}
const showRequestError = (error: any, fallback: string) => {
  ElMessage.error(error?.response?.data?.message || error?.message || fallback)
}
const submitNews = async () => {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const request = editingId.value
        ? updateNewsWithFiles(editingId.value, buildNewsFormData())
        : createNewsWithFiles(buildNewsFormData())
    const result: any = await request

    if (result.code !== 200) {
      ElMessage.error(result.message || '新闻保存失败')
      return
    }

    dialogVisible.value = false
    ElMessage.success(result.message || (editingId.value ? '新闻已更新并提交审核' : '新闻已提交审核'))
    await getNewsList()
  } catch (error: any) {
    showRequestError(error, '新闻保存失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  getNewsList()
})
</script>

<template>
  <div class="news-page">
    <div class="page-toolbar">
      <div>
        <h2>新闻管理</h2>
        <p>参照学院新闻页面结构维护标题、栏目、审核信息和正文内容。</p>
      </div>

      <el-button type="primary" @click="openAddDialog">添加新闻</el-button>
    </div>

    <div class="list-panel">
      <el-form :model="queryForm" inline class="query-form">
        <el-form-item label="关键词">
          <el-input
              v-model="queryForm.keyword"
              clearable
              placeholder="标题、供稿、审稿"
              @clear="handleSearch"
              @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="栏目">
          <el-select v-model="queryForm.category" clearable placeholder="全部栏目" style="width: 150px" @clear="handleSearch">
            <el-option
                v-for="item in categoryOptions"
                :key="item"
                :label="item"
                :value="item"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" clearable placeholder="全部状态" style="width: 130px" @clear="handleSearch">
            <el-option label="待审核" value="PENDING_REVIEW"/>
            <el-option label="已发布" value="PUBLISHED"/>
            <el-option label="已驳回" value="REJECTED"/>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table
          v-loading="tableLoading"
          :data="newsList"
          border
          class="news-table"
          height="100%"
      >
        <el-table-column prop="title" label="新闻标题" min-width="220" show-overflow-tooltip/>
        <el-table-column prop="category" label="栏目" width="110"/>
        <el-table-column label="正文摘要" min-width="240">
          <template #default="{ row }">
            {{ getContentSummary(row.content) }}
          </template>
        </el-table-column>
        <el-table-column prop="supplier" label="供稿" width="110" show-overflow-tooltip>
          <template #default="{ row }">{{ row.supplier || '-' }}</template>
        </el-table-column>
        <el-table-column prop="reviewer" label="审稿" width="110" show-overflow-tooltip>
          <template #default="{ row }">{{ row.reviewer || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.publishTime) }}</template>
        </el-table-column>
        <el-table-column label="更新时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.updateTime || row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="openNewsDialog(row)">编辑</el-button>
            <el-button
                type="danger"
                size="small"
                :loading="isDeleting(row.id)"
                @click="handleDeleteNews(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
        <el-table-column label="审核" width="210" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openPreview(row)">预览</el-button>
            <el-tooltip
                :content="getAuditDisabledReason(row)"
                :disabled="canAudit(row)"
                placement="top"
            >
              <span>
                <el-button
                    type="success"
                    size="small"
                    :disabled="!canAudit(row)"
                    :loading="isAuditing(row.id)"
                    @click="auditNews(row, 'approve')"
                >
                  通过
                </el-button>
              </span>
            </el-tooltip>
            <el-tooltip
                :content="getAuditDisabledReason(row)"
                :disabled="canAudit(row)"
                placement="top"
            >
              <span>
                <el-button
                    type="danger"
                    size="small"
                    :disabled="!canAudit(row)"
                    :loading="isAuditing(row.id)"
                    @click="auditNews(row, 'reject')"
                >
                  驳回
                </el-button>
              </span>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50]"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <el-dialog
        v-model="dialogVisible"
        :title="editingId ? '编辑新闻' : '添加新闻'"
        width="1040px"
        destroy-on-close
        align-center
        class="news-dialog"
    >
      <el-form ref="formRef" :model="newsForm" :rules="rules" label-width="86px" class="news-form">
        <div class="form-basic">
          <el-form-item label="新闻标题" prop="title">
            <el-input v-model="newsForm.title" maxlength="80" show-word-limit placeholder="请输入新闻标题"/>
          </el-form-item>

          <el-row :gutter="18">
            <el-col :span="8">
              <el-form-item label="栏目" prop="category">
                <el-select v-model="newsForm.category" placeholder="请选择栏目" style="width: 100%">
                  <el-option
                      v-for="item in categoryOptions"
                      :key="item"
                      :label="item"
                      :value="item"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="18">
            <el-col :span="12">
              <el-form-item label="供稿">
                <el-input v-model="newsForm.supplier" placeholder="请输入供稿人"/>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="审稿">
                <el-input v-model="newsForm.reviewer" placeholder="请输入审稿人"/>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="附件">
            <el-upload
                v-model:file-list="attachmentFiles"
                class="attachment-upload"
                action="#"
                multiple
                :auto-upload="false"
                :on-change="handleAttachmentChange"
                :on-remove="handleAttachmentRemove"
            >
              <el-button>选择附件</el-button>
              <template #tip>
                <div class="upload-tip">
                  {{ editingId ? '可删除已有附件，也可继续选择新附件；保存修改时一起提交' : '支持 PDF、Word、Excel 等文件，单个附件不超过 20MB，可一次选择多个' }}
                </div>
              </template>
            </el-upload>
          </el-form-item>

        </div>

        <el-form-item label="正文内容" prop="content" class="content-form-item">
          <div ref="editorWrapperRef" class="editor-wrapper">
            <Toolbar
                :editor="editorRef"
                :default-config="toolbarConfig"
                mode="default"
                class="editor-toolbar"
            />
            <Editor
                v-model="newsForm.content"
                :default-config="editorConfig"
                mode="default"
                class="editor-content"
                @on-created="handleEditorCreated"
                @on-change="scrollEditorToBottom"
                @keyup="scrollEditorToBottom"
                @input="scrollEditorToBottom"
            />
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <div class="editor-footer-border"></div>
          <div class="dialog-footer-actions">
            <el-button @click="dialogVisible = false">取消</el-button>
            <el-button type="primary" :loading="submitting" @click="submitNews">
              {{ editingId ? '保存修改' : '保存新闻' }}
            </el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <el-dialog
        v-model="previewVisible"
        width="900px"
        destroy-on-close
        align-center
        class="preview-dialog"
    >
      <article v-if="previewNews" class="preview-content">
        <h1>{{ previewNews.title }}</h1>
        <div class="preview-meta">
          <span>{{ previewNews.category }}</span>
          <span>供稿：{{ previewNews.supplier || '-' }}</span>
          <span>审稿：{{ previewNews.reviewer || '-' }}</span>
          <span>{{ formatDateTime(previewNews.updateTime || previewNews.createTime) }}</span>
          <el-tag :type="getStatusTagType(previewNews.status)" size="small">
            {{ getStatusText(previewNews.status) }}
          </el-tag>
        </div>
        <div class="preview-body" v-html="previewNews.content"></div>
        <section v-if="previewNews.attachments?.length" class="preview-attachments">
          <h3>附件</h3>
          <div
              v-for="attachment in previewNews.attachments"
              :key="attachment.url || attachment.name"
              class="attachment-row"
          >
            <div class="attachment-info">
              <el-tag size="small" effect="plain">{{ getAttachmentTypeText(attachment) }}</el-tag>
              <span class="attachment-name">{{ attachment.name }}</span>
              <small>{{ formatFileSize(attachment.size) }}</small>
            </div>
            <div class="attachment-actions">
              <el-button
                  v-if="isPreviewableAttachment(attachment)"
                  type="primary"
                  link
                  @click="openAttachmentPreview(attachment)"
              >
                预览
              </el-button>
              <a
                  class="attachment-download"
                  :href="getAttachmentUrl(attachment.url)"
                  :download="attachment.name"
                  target="_blank"
                  rel="noopener"
              >
                下载
              </a>
            </div>
          </div>
        </section>
      </article>
    </el-dialog>
  </div>
</template>

<style scoped>
.news-page {
  height: 100%;
  padding: 22px;
  background: #f5f7fb;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.page-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
  padding: 18px 20px;
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
}

.page-toolbar h2 {
  margin: 0 0 6px;
  color: #1f2d3d;
  font-size: 22px;
  font-weight: 600;
}

.page-toolbar p {
  margin: 0;
  color: #7a8499;
  font-size: 14px;
}

.list-panel {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  gap: 12px;
  padding: 16px;
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
}

.query-form {
  display: flex;
  flex-wrap: wrap;
  gap: 0 8px;
}

.query-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.news-table {
  min-height: 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
}

:global(.news-dialog.el-dialog),
:global(.news-dialog .el-dialog) {
  width: min(1040px, calc(100vw - 48px));
  height: min(840px, calc(100vh - 48px));
  display: flex;
  flex-direction: column;
  margin: 0 auto;
}

:global(.news-dialog.el-dialog .el-dialog__header),
:global(.news-dialog .el-dialog__header),
:global(.news-dialog.el-dialog .el-dialog__footer),
:global(.news-dialog .el-dialog__footer) {
  flex-shrink: 0;
}

:global(.news-dialog.el-dialog .el-dialog__body),
:global(.news-dialog .el-dialog__body) {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  padding-bottom: 0;
}

.news-form {
  height: 100%;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  overflow: hidden;
}

.form-basic {
  min-height: 0;
  padding-right: 8px;
}

.attachment-upload {
  width: 100%;
}

.upload-tip {
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
}

.edit-attachment-list {
  display: grid;
  gap: 8px;
  width: 100%;
}

.edit-attachment-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border: 1px solid #e5e6eb;
  border-radius: 6px;
  color: #303133;
  text-decoration: none;
  background: #fff;
}

.edit-attachment-item:hover {
  border-color: #409eff;
  color: #409eff;
}

.edit-attachment-type {
  padding: 2px 6px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  color: #606266;
  font-size: 12px;
  line-height: 1.4;
  background: #f5f7fb;
}

.edit-attachment-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.edit-attachment-item small,
.edit-attachment-empty {
  color: #909399;
}

.content-form-item {
  min-height: 0;
  margin-bottom: 0;
}

.content-form-item :deep(.el-form-item__content) {
  height: 100%;
  min-height: 0;
  display: flex;
  align-items: stretch;
}

.editor-wrapper {
  width: 100%;
  height: 100%;
  min-height: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}

.editor-toolbar {
  position: relative;
  z-index: 2;
  min-height: 40px;
  background: #fff;
  border-bottom: 1px solid #dcdfe6;
}

.editor-content {
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.editor-content :deep(> div) {
  height: 100% !important;
}

:deep(.editor-content .w-e-text-container) {
  height: 100% !important;
  overflow: hidden !important;
}

:deep(.editor-content .w-e-scroll) {
  height: 100% !important;
  overflow-y: auto !important;
  scroll-behavior: auto;
}

:deep(.editor-content [data-slate-editor]) {
  min-height: 100%;
}

:global(.news-dialog.el-dialog .el-dialog__footer),
:global(.news-dialog .el-dialog__footer) {
  padding-top: 0;
}

.dialog-footer {
  text-align: right;
}

.editor-footer-border {
  height: 1px;
  margin: 0 0 16px 86px;
  background: #dcdfe6;
}

.dialog-footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

:global(.preview-dialog.el-dialog),
:global(.preview-dialog .el-dialog) {
  width: min(900px, calc(100vw - 48px));
  max-height: min(820px, calc(100vh - 48px));
  display: flex;
  flex-direction: column;
}

:global(.preview-dialog.el-dialog .el-dialog__body),
:global(.preview-dialog .el-dialog__body) {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 22px 32px 32px;
}

.preview-content {
  color: #303133;
}

.preview-content h1 {
  margin: 0 0 14px;
  color: #1f2d3d;
  font-size: 24px;
  font-weight: 600;
  line-height: 1.45;
  text-align: center;
}

.preview-meta {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px 16px;
  margin-bottom: 22px;
  color: #7a8499;
  font-size: 14px;
}

.preview-body {
  color: #303133;
  font-size: 16px;
  line-height: 1.85;
  word-break: break-word;
}

.preview-body :deep(img) {
  max-width: 100%;
  height: auto;
}

.preview-body :deep(p) {
  margin: 0 0 12px;
}

.preview-attachments {
  margin-top: 28px;
  padding-top: 18px;
  border-top: 1px solid #e5e6eb;
}

.preview-attachments h3 {
  margin: 0 0 10px;
  color: #1f2d3d;
  font-size: 18px;
  font-weight: 600;
}

.attachment-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 0;
  border-bottom: 1px solid #eef0f4;
}

.attachment-row:last-child {
  border-bottom: 0;
}

.attachment-info {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.attachment-name {
  min-width: 0;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attachment-info small {
  flex-shrink: 0;
  color: #909399;
}

.attachment-actions {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.attachment-download {
  color: #409eff;
  font-size: 14px;
  line-height: 1;
  text-decoration: underline;
}

</style>
