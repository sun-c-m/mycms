<template>
  <div class="permission-page">
    <div class="page-toolbar">
      <div>
        <h2>权限管理</h2>
        <p>维护目录、菜单和按钮权限，供角色授权和动态菜单使用。</p>
      </div>
      <el-button type="primary" @click="openPermissionDialog()">新增权限</el-button>
    </div>

    <div class="list-panel">
      <el-form :model="queryForm" inline class="query-form">
        <el-form-item label="权限名称">
          <el-input
              v-model="queryForm.name"
              clearable
              placeholder="请输入权限名称"
              @clear="handleSearch"
              @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="权限标识">
          <el-input
              v-model="queryForm.code"
              clearable
              placeholder="请输入权限标识"
              @clear="handleSearch"
              @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="类型">
          <el-select
              v-model="queryForm.menuType"
              clearable
              placeholder="全部类型"
              style="width: 150px"
              @clear="handleSearch"
              @change="handleSearch"
          >
            <el-option label="目录" value="DIRECTORY"/>
            <el-option label="菜单" value="MENU"/>
            <el-option label="按钮" value="BUTTON"/>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table
          v-loading="tableLoading"
          :data="permissionList"
          border
          class="permission-table"
          height="100%"
      >
        <el-table-column prop="id" label="权限ID" width="90"/>
        <el-table-column prop="name" label="权限名称" min-width="150" show-overflow-tooltip/>
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag :type="permissionTypeTagMap[normalizeMenuType(row.menuType)] || 'info'" size="small">
              {{ permissionTypeTextMap[normalizeMenuType(row.menuType)] || row.menuType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="code" label="权限标识" min-width="190" show-overflow-tooltip>
          <template #default="{ row }">{{ row.code || '-' }}</template>
        </el-table-column>
        <el-table-column prop="path" label="路由路径" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">{{ row.path || '-' }}</template>
        </el-table-column>
        <el-table-column label="父级权限" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ parentNameMap.get(row.parentId) || '顶级权限' }}</template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="90"/>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="openPermissionDialog(row)">编辑</el-button>
            <el-button
                type="danger"
                size="small"
                :loading="deletingIds.includes(row.id)"
                @click="handleDelete(row)"
            >
              删除
            </el-button>
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
        :title="editingId ? '编辑权限' : '新增权限'"
        width="560px"
        destroy-on-close
        align-center
    >
      <el-form ref="formRef" :model="permissionForm" :rules="rules" label-width="92px">
        <el-form-item label="权限名称" prop="name">
          <el-input v-model="permissionForm.name" maxlength="50" placeholder="请输入权限名称"/>
        </el-form-item>
        <el-form-item label="权限类型" prop="menuType">
          <el-select v-model="permissionForm.menuType" placeholder="请选择权限类型" style="width: 100%">
            <el-option label="目录" value="DIRECTORY"/>
            <el-option label="菜单" value="MENU"/>
            <el-option label="按钮" value="BUTTON"/>
          </el-select>
        </el-form-item>
        <el-form-item label="父级权限">
          <el-select v-model="permissionForm.parentId" filterable placeholder="请选择父级权限" style="width: 100%">
            <el-option label="顶级权限" :value="0"/>
            <el-option
                v-for="option in parentOptions"
                :key="option.id"
                :label="option.label"
                :value="option.id"
                :disabled="option.id === editingId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="权限标识">
          <el-input v-model="permissionForm.code" maxlength="100" placeholder="例如 role:add"/>
        </el-form-item>
        <el-form-item label="路由路径">
          <el-input v-model="permissionForm.path" maxlength="255" placeholder="例如 /permission/list"/>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="permissionForm.sort" :min="0" :max="9999" controls-position="right"/>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitPermission">
          {{ editingId ? '保存修改' : '新增权限' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, reactive, ref} from 'vue'
import {ElMessage, ElMessageBox, type FormInstance, type FormRules} from 'element-plus'
import {
  createPermission,
  deletePermission,
  getPermissionPage,
  getPermissionTree,
  updatePermission
} from '@/http/permission.ts'

interface PermissionItem {
  id: number
  name: string
  menuType: string
  code?: string
  path?: string
  parentId: number
  sort: number
  createTime?: string
  children?: PermissionItem[]
}

interface PermissionForm {
  name: string
  menuType: string
  code: string
  path: string
  parentId: number
  sort: number
}

interface ParentOption {
  id: number
  label: string
}

const emptyForm = (): PermissionForm => ({
  name: '',
  menuType: 'MENU',
  code: '',
  path: '',
  parentId: 0,
  sort: 0,
})

const formRef = ref<FormInstance>()
const permissionForm = ref<PermissionForm>(emptyForm())
const editingId = ref<number | null>(null)
const dialogVisible = ref(false)
const submitting = ref(false)
const tableLoading = ref(false)
const permissionList = ref<PermissionItem[]>([])
const permissionTree = ref<PermissionItem[]>([])
const deletingIds = ref<number[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const queryForm = reactive({
  name: '',
  code: '',
  menuType: '',
})

const permissionTypeTagMap: Record<string, 'primary' | 'success' | 'warning'> = {
  DIRECTORY: 'primary',
  MENU: 'success',
  BUTTON: 'warning',
}

const permissionTypeTextMap: Record<string, string> = {
  DIRECTORY: '目录',
  MENU: '菜单',
  BUTTON: '按钮',
}

const rules: FormRules<PermissionForm> = {
  name: [{required: true, message: '请输入权限名称', trigger: 'blur'}],
  menuType: [{required: true, message: '请选择权限类型', trigger: 'change'}],
}

const normalizeMenuType = (menuType?: string) => (menuType || '').toUpperCase()

const flattenPermissionTree = (nodes: PermissionItem[] = [], level = 0, result: ParentOption[] = []) => {
  nodes.forEach(node => {
    result.push({
      id: node.id,
      label: `${'　'.repeat(level)}${level > 0 ? '└ ' : ''}${node.name}`,
    })
    if (node.children?.length) {
      flattenPermissionTree(node.children, level + 1, result)
    }
  })
  return result
}

const parentOptions = computed(() => flattenPermissionTree(permissionTree.value))

const parentNameMap = computed(() => {
  const map = new Map<number, string>()
  flattenPermissionTree(permissionTree.value).forEach(option => {
    map.set(option.id, option.label.replace(/^[　└ ]+/, ''))
  })
  return map
})

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

const showRequestError = (error: any, fallback: string) => {
  ElMessage.error(error?.response?.data?.message || error?.message || fallback)
}

const getPermissions = async () => {
  tableLoading.value = true
  try {
    const result: any = await getPermissionPage({
      currentPage: currentPage.value,
      pageSize: pageSize.value,
      params: [
        {name: 'name', value: queryForm.name},
        {name: 'code', value: queryForm.code},
        {name: 'menuType', value: queryForm.menuType},
      ],
    })
    permissionList.value = result.data?.records || []
    total.value = Number(result.data?.total || 0)
  } catch (error: any) {
    showRequestError(error, '权限列表加载失败')
  } finally {
    tableLoading.value = false
  }
}

const getPermissionTreeData = async () => {
  try {
    const result: any = await getPermissionTree()
    permissionTree.value = result.data || []
  } catch (error: any) {
    showRequestError(error, '权限树加载失败')
  }
}

const handleSearch = () => {
  currentPage.value = 1
  getPermissions()
}

const resetSearch = () => {
  queryForm.name = ''
  queryForm.code = ''
  queryForm.menuType = ''
  handleSearch()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  getPermissions()
}

const handleCurrentChange = (page: number) => {
  currentPage.value = page
  getPermissions()
}

const openPermissionDialog = async (row?: PermissionItem) => {
  if (permissionTree.value.length === 0) {
    await getPermissionTreeData()
  }
  editingId.value = row?.id || null
  permissionForm.value = row ? {
    name: row.name,
    menuType: normalizeMenuType(row.menuType),
    code: row.code || '',
    path: row.path || '',
    parentId: row.parentId || 0,
    sort: row.sort || 0,
  } : emptyForm()
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

const submitPermission = async () => {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = {
      name: permissionForm.value.name,
      menuType: permissionForm.value.menuType,
      code: permissionForm.value.code,
      path: permissionForm.value.path,
      parentId: permissionForm.value.parentId,
      sort: permissionForm.value.sort,
    }
    const result: any = editingId.value
        ? await updatePermission(editingId.value, payload)
        : await createPermission(payload)

    ElMessage.success(result.message || (editingId.value ? '权限已更新' : '权限已新增'))
    dialogVisible.value = false
    await Promise.all([getPermissions(), getPermissionTreeData()])
  } catch (error: any) {
    showRequestError(error, '权限保存失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row: PermissionItem) => {
  try {
    await ElMessageBox.confirm(`确认删除权限“${row.name}”吗？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  deletingIds.value.push(row.id)
  try {
    const result: any = await deletePermission(row.id)
    ElMessage.success(result.message || '权限已删除')
    if (permissionList.value.length === 1 && currentPage.value > 1) {
      currentPage.value -= 1
    }
    await Promise.all([getPermissions(), getPermissionTreeData()])
  } catch (error: any) {
    showRequestError(error, '权限删除失败')
  } finally {
    deletingIds.value = deletingIds.value.filter(id => id !== row.id)
  }
}

onMounted(() => {
  getPermissions()
  getPermissionTreeData()
})
</script>

<style scoped>
.permission-page {
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

.permission-table {
  min-height: 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
}
</style>
