import axios from "@/http/axios.ts";

export interface UserPayload {
    username: string
    password?: string
    roleIds: number[]
}

export interface UserPageQuery {
    currentPage: number
    pageSize: number
    params: Array<{
        name: string
        value: string
    }>
}

export const getUserPage = (data: UserPageQuery) => {
    return axios({
        url: '/api/user/page',
        method: 'POST',
        data
    })
}

export const createUser = (data: UserPayload) => {
    return axios({
        url: '/api/user',
        method: 'POST',
        data
    })
}

export const updateUser = (id: string, data: UserPayload) => {
    return axios({
        url: `/api/user/${id}`,
        method: 'PUT',
        data
    })
}

export const deleteUser = (id: string) => {
    return axios({
        url: `/api/user/${id}`,
        method: 'DELETE'
    })
}
