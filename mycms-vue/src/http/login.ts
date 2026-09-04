import axios from "@/http/axios.ts";

export const login = (data: any) => {
    return axios({
        url: '/api/auth/login',
        method: 'POST',
        data
    })
}
export const getUser = (username: string, password: string) => login({ username, password })
