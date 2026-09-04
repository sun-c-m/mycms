import axios from "@/http/axios.ts";

export interface PermissionPayload {
    name: string
    menuType: string
    code?: string
    path?: string
    parentId: number
    sort: number
}

export interface PermissionPageQuery {
    currentPage: number
    pageSize: number
    params: Array<{
        name: string
        value: string
    }>
}

export const getPermissionPage = (data: PermissionPageQuery) => {
    return axios({
        url: '/api/permission/page',
        method: 'POST',
        data
    })
}

export const getPermissionTree = () => {
    return axios({
        url: '/api/permission/tree',
        method: 'GET'
    })
}

export const createPermission = (data: PermissionPayload) => {
    return axios({
        url: '/api/permission',
        method: 'POST',
        data
    })
}

export const updatePermission = (id: number, data: PermissionPayload) => {
    return axios({
        url: `/api/permission/${id}`,
        method: 'PUT',
        data
    })
}

export const deletePermission = (id: number) => {
    return axios({
        url: `/api/permission/${id}`,
        method: 'DELETE'
    })
}
