<template>
  <div class="role-page">
    <div class="page-toolbar">
      <div>
        <h2>角色管理</h2>
        <p>维护系统角色，并给角色分配菜单和按钮权限。</p>
      </div>
      <el-button type="primary" @click="openRoleDialog()">新增角色</el-button>
    </div>

    <div class="list-panel">
      <el-form :model="queryForm" inline class="query-form">
        <el-form-item label="角色名称">
          <el-input
              v-model="queryForm.roleName"
              clearable
              placeholder="请输入角色名称"
              @clear="handleSearch"
              @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table
          v-loading="tableLoading"
          :data="roleList"
          border
          class="role-table"
          height="100%"
      >
        <el-table-column prop="id" label="角色ID" width="120"/>
        <el-table-column prop="name" label="角色名称" min-width="220" show-overflow-tooltip/>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="openRoleDialog(row)">编辑</el-button>
            <el-button type="success" size="small" @click="handleAuthorize(row)">授权</el-button>
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
        v-model="roleDialogVisible"
        :title="editingId ? '编辑角色' : '新增角色'"
        width="460px"
        destroy-on-close
        align-center
    >
      <el-form ref="roleFormRef" :model="roleForm" :rules="roleRules" label-width="88px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="roleForm.name" maxlength="50" placeholder="请输入角色名称"/>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitRole">
          {{ editingId ? '保存修改' : '新增角色' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
        v-model="authorizeDialogVisible"
        :title="`角色授权${currentRole ? ` - ${currentRole.name}` : ''}`"
        width="640px"
        destroy-on-close
        align-center
        @closed="handleAuthorizeDialogClosed"
    >
      <div class="permission-tree-wrapper">
        <el-tree
            ref="permissionTreeRef"
            :data="permissionTree"
            :props="permissionTreeProps"
            node-key="id"
            show-checkbox
            default-expand-all
            :check-on-click-node="true"
        >
          <template #default="{ node, data }">
            <div class="permission-tree-node">
              <span>{{ node.label }}</span>
              <el-tag
                  class="permission-type-tag"
                  size="small"
                  :type="permissionTypeTagMap[normalizeMenuType(data.menuType)] || 'info'"
              >
                {{ normalizeMenuType(data.menuType) }}
              </el-tag>
            </div>
          </template>
        </el-tree>
      </div>

      <template #footer>
        <el-button @click="authorizeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="authorizing" @click="handleSaveAuthorize">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {nextTick, onMounted, reactive, ref} from 'vue'
import {ElMessage, ElMessageBox, type FormInstance, type FormRules} from 'element-plus'
import {
  authorizeRole,
  createRole,
  deleteRole,
  getPermissionTree,
  getRoleInfoPage,
  getRolePermissionIds,
  updateRole
} from '@/http/role.ts'

interface RoleItem {
  id: number
  name: string
}

interface RoleForm {
  name: string
}

interface PermissionItem {
  id: number
  name: string
  menuType: string
  children?: PermissionItem[]
}

const emptyRoleForm = (): RoleForm => ({
  name: '',
})

const roleFormRef = ref<FormInstance>()
const roleForm = ref<RoleForm>(emptyRoleForm())
const editingId = ref<number | null>(null)
const roleDialogVisible = ref(false)
const authorizeDialogVisible = ref(false)
const submitting = ref(false)
const authorizing = ref(false)
const tableLoading = ref(false)
const roleList = ref<RoleItem[]>([])
const deletingIds = ref<number[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const currentRole = ref<RoleItem | null>(null)
const permissionTree = ref<PermissionItem[]>([])
const permissionTreeRef = ref<any>(null)
const queryForm = reactive({
  roleName: '',
})

const permissionTreeProps = {
  label: 'name',
  children: 'children',
}

const permissionTypeTagMap: Record<string, 'primary' | 'success' | 'warning'> = {
  DIRECTORY: 'primary',
  MENU: 'success',
  BUTTON: 'warning',
}

const roleRules: FormRules<RoleForm> = {
  name: [{required: true, message: '请输入角色名称', trigger: 'blur'}],
}

const normalizeMenuType = (menuType?: string) => (menuType || '').toUpperCase()

const showRequestError = (error: any, fallback: string) => {
  ElMessage.error(error?.response?.data?.message || error?.message || fallback)
}

const getRoleData = async () => {
  tableLoading.value = true
  try {
    const result: any = await getRoleInfoPage({
      currentPage: currentPage.value,
      pageSize: pageSize.value,
      params: [{name: 'roleName', value: queryForm.roleName}],
    })
    roleList.value = result.data?.records || []
    total.value = Number(result.data?.total || 0)
  } catch (error: any) {
    showRequestError(error, '角色列表加载失败')
  } finally {
    tableLoading.value = false
  }
}

const getPermissionTreeData = async () => {
  const result: any = await getPermissionTree()
  permissionTree.value = result.data || []
}

const handleSearch = () => {
  currentPage.value = 1
  getRoleData()
}

const resetSearch = () => {
  queryForm.roleName = ''
  handleSearch()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  getRoleData()
}

const handleCurrentChange = (page: number) => {
  currentPage.value = page
  getRoleData()
}

const openRoleDialog = (row?: RoleItem) => {
  editingId.value = row?.id || null
  roleForm.value = row ? {name: row.name} : emptyRoleForm()
  roleFormRef.value?.clearValidate()
  roleDialogVisible.value = true
}

const submitRole = async () => {
  if (!roleFormRef.value) return

  const valid = await roleFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = {name: roleForm.value.name}
    const result: any = editingId.value
        ? await updateRole(editingId.value, payload)
        : await createRole(payload)

    ElMessage.success(result.message || (editingId.value ? '角色已更新' : '角色已新增'))
    roleDialogVisible.value = false
    await getRoleData()
  } catch (error: any) {
    showRequestError(error, '角色保存失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row: RoleItem) => {
  try {
    await ElMessageBox.confirm(`确认删除角色“${row.name}”吗？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  deletingIds.value.push(row.id)
  try {
    const result: any = await deleteRole(row.id)
    ElMessage.success(result.message || '角色已删除')
    if (roleList.value.length === 1 && currentPage.value > 1) {
      currentPage.value -= 1
    }
    await getRoleData()
  } catch (error: any) {
    showRequestError(error, '角色删除失败')
  } finally {
    deletingIds.value = deletingIds.value.filter(id => id !== row.id)
  }
}

const handleAuthorize = async (role: RoleItem) => {
  currentRole.value = role
  authorizeDialogVisible.value = true

  try {
    if (permissionTree.value.length === 0) {
      await getPermissionTreeData()
    }

    const result: any = await getRolePermissionIds(role.id)
    const permissionIds: number[] = result.data || []

    await nextTick()
    permissionTreeRef.value?.setCheckedKeys([])
    permissionIds.forEach(permissionId => {
      permissionTreeRef.value?.setChecked(permissionId, true, false)
    })
  } catch (error: any) {
    showRequestError(error, '角色权限加载失败')
  }
}

const handleSaveAuthorize = async () => {
  if (!currentRole.value) return

  const checkedKeys = permissionTreeRef.value?.getCheckedKeys(false) || []
  const halfCheckedKeys = permissionTreeRef.value?.getHalfCheckedKeys() || []
  const selectedPermissionIds = [...new Set([...checkedKeys, ...halfCheckedKeys])].map(Number)

  authorizing.value = true
  try {
    const result: any = await authorizeRole({
      roleId: currentRole.value.id,
      permissionIds: selectedPermissionIds,
    })
    ElMessage.success(result.message || '授权保存成功')
    authorizeDialogVisible.value = false
  } catch (error: any) {
    showRequestError(error, '授权保存失败')
  } finally {
    authorizing.value = false
  }
}

const handleAuthorizeDialogClosed = () => {
  permissionTreeRef.value?.setCheckedKeys([])
  currentRole.value = null
}

onMounted(() => {
  getRoleData()
})
</script>

<style scoped>
.role-page {
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

.role-table {
  min-height: 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
}

.permission-tree-wrapper {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  max-height: 420px;
  overflow-y: auto;
  padding: 10px 12px;
}

.permission-tree-node {
  align-items: center;
  display: flex;
  gap: 10px;
  width: 100%;
}

.permission-type-tag {
  margin-left: auto;
}
</style>
