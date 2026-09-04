

import axios from "@/http/axios.ts";

export interface RolePayload {
    name: string
}

export interface RolePageQuery {
    currentPage: number
    pageSize: number
    params: Array<{
        name: string
        value: string
    }>
}

export interface RoleAuthorizePayload {
    roleId: number
    permissionIds: number[]
}

export const getRoleInfoPage = (data: RolePageQuery) => {
    return axios({
        url: '/api/role/getRoleInfoPage',
        method: 'POST',
        data
    })
}

export const createRole = (data: RolePayload) => {
    return axios({
        url: '/api/role',
        method: 'POST',
        data
    })
}

export const updateRole = (id: number, data: RolePayload) => {
    return axios({
        url: `/api/role/${id}`,
        method: 'PUT',
        data
    })
}

export const deleteRole = (id: number) => {
    return axios({
        url: `/api/role/${id}`,
        method: 'DELETE'
    })
}

export const getPermissionTree = () => {
    return axios({
        url: '/api/role/permissionTree',
        method: 'GET'
    })
}

export const getRolePermissionIds = (roleId: number) => {
    return axios({
        url: `/api/role/${roleId}`,
        method: 'GET'
    })
}

export const authorizeRole = (data: RoleAuthorizePayload) => {
    return axios({
        url: '/api/role/authorize',
        method: 'POST',
        data
    })
}
