<template>
  <div class="user-page">
    <div class="page-toolbar">
      <div>
        <h2>用户管理</h2>
        <p>维护系统登录账号，并给用户分配角色。</p>
      </div>
      <el-button type="primary" @click="openUserDialog()">新增用户</el-button>
    </div>

    <div class="list-panel">
      <el-form :model="queryForm" inline class="query-form">
        <el-form-item label="用户名">
          <el-input
              v-model="queryForm.username"
              clearable
              placeholder="请输入用户名"
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
          :data="userList"
          border
          class="user-table"
          height="100%"
      >
        <el-table-column prop="id" label="用户ID" width="110"/>
        <el-table-column prop="username" label="用户名" min-width="160" show-overflow-tooltip/>
        <el-table-column label="角色" min-width="220">
          <template #default="{ row }">
            <div v-if="row.roleNames.length" class="role-tags">
              <el-tag v-for="roleName in row.roleNames" :key="roleName" size="small">
                {{ roleName }}
              </el-tag>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="openUserDialog(row)">编辑</el-button>
            <el-button
                type="danger"
                size="small"
                :loading="deletingIds.includes(row.id)"
                @click="handleDeleteUser(row)"
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
        :title="editingId ? '编辑用户' : '新增用户'"
        width="520px"
        destroy-on-close
        align-center
    >
      <el-form ref="formRef" :model="userForm" :rules="rules" label-width="86px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" maxlength="50" placeholder="请输入用户名"/>
        </el-form-item>
        <el-form-item :label="editingId ? '新密码' : '密码'" prop="password">
          <el-input
              v-model="userForm.password"
              type="password"
              show-password
              :placeholder="editingId ? '留空表示不修改密码' : '请输入密码'"
          />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="userForm.roleIds" multiple clearable placeholder="请选择角色" style="width: 100%">
            <el-option
                v-for="role in roleOptions"
                :key="role.id"
                :label="role.name"
                :value="role.id"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitUser">
          {{ editingId ? '保存修改' : '新增用户' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {onMounted, reactive, ref} from "vue";
import {ElMessage, ElMessageBox, type FormInstance, type FormRules} from "element-plus";
import {createUser, deleteUser, getUserPage, updateUser} from "@/http/user.ts";
import {getRoleInfoPage} from "@/http/role.ts";

interface RoleOption {
  id: number
  name: string
}

interface UserItem {
  id: string
  username: string
  createTime?: string
  updateTime?: string
  roleIds: number[]
  roleNames: string[]
}

interface UserForm {
  username: string
  password: string
  roleIds: number[]
}

const emptyForm = (): UserForm => ({
  username: '',
  password: '',
  roleIds: [],
})

const formRef = ref<FormInstance>()
const userForm = ref<UserForm>(emptyForm())
const editingId = ref<string | null>(null)
const dialogVisible = ref(false)
const submitting = ref(false)
const tableLoading = ref(false)
const userList = ref<UserItem[]>([])
const roleOptions = ref<RoleOption[]>([])
const deletingIds = ref<string[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const queryForm = reactive({
  username: '',
})

const rules: FormRules<UserForm> = {
  username: [{required: true, message: '请输入用户名', trigger: 'blur'}],
  password: [{
    validator: (_rule, value, callback) => {
      if (!editingId.value && !value) {
        callback(new Error('请输入密码'))
        return
      }
      callback()
    },
    trigger: 'blur'
  }],
}

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

const showRequestError = (error: any, fallback: string) => {
  ElMessage.error(error?.response?.data?.message || error?.message || fallback)
}

const getUsers = async () => {
  tableLoading.value = true
  try {
    const result: any = await getUserPage({
      currentPage: currentPage.value,
      pageSize: pageSize.value,
      params: [{name: 'username', value: queryForm.username}],
    })
    userList.value = (result.data?.records || []).map((item: UserItem) => ({
      ...item,
      roleIds: Array.isArray(item.roleIds) ? item.roleIds : [],
      roleNames: Array.isArray(item.roleNames) ? item.roleNames : [],
    }))
    total.value = Number(result.data?.total || 0)
  } catch (error: any) {
    showRequestError(error, '用户列表加载失败')
  } finally {
    tableLoading.value = false
  }
}

const getRoles = async () => {
  try {
    const result: any = await getRoleInfoPage({
      currentPage: 1,
      pageSize: 100,
      params: [{name: 'roleName', value: ''}],
    })
    roleOptions.value = result.data?.records || []
  } catch (error: any) {
    showRequestError(error, '角色列表加载失败')
  }
}

const handleSearch = () => {
  currentPage.value = 1
  getUsers()
}

const resetSearch = () => {
  queryForm.username = ''
  handleSearch()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  getUsers()
}

const handleCurrentChange = (page: number) => {
  currentPage.value = page
  getUsers()
}

const openUserDialog = (row?: UserItem) => {
  editingId.value = row?.id || null
  userForm.value = row ? {
    username: row.username,
    password: '',
    roleIds: [...row.roleIds],
  } : emptyForm()
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

const submitUser = async () => {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = {
      username: userForm.value.username,
      password: userForm.value.password,
      roleIds: userForm.value.roleIds,
    }
    const result: any = editingId.value
        ? await updateUser(editingId.value, payload)
        : await createUser(payload)

    ElMessage.success(result.message || (editingId.value ? '用户已更新' : '用户已新增'))
    dialogVisible.value = false
    await getUsers()
  } catch (error: any) {
    showRequestError(error, '用户保存失败')
  } finally {
    submitting.value = false
  }
}

const handleDeleteUser = async (row: UserItem) => {
  try {
    await ElMessageBox.confirm(`确认删除用户“${row.username}”吗？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  deletingIds.value.push(row.id)
  try {
    const result: any = await deleteUser(row.id)
    ElMessage.success(result.message || '用户已删除')
    if (userList.value.length === 1 && currentPage.value > 1) {
      currentPage.value -= 1
    }
    await getUsers()
  } catch (error: any) {
    showRequestError(error, '用户删除失败')
  } finally {
    deletingIds.value = deletingIds.value.filter(id => id !== row.id)
  }
}

onMounted(() => {
  getRoles()
  getUsers()
})
</script>

<style scoped>
.user-page {
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

.user-table {
  min-height: 0;
}

.role-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
}

</style>
