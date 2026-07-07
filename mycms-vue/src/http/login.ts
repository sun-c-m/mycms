import axios from "@/http/axios.ts";

export const getUser = (username: string, password: string) => {
    return axios({
        url: '/login/getUser',
        method: "POST",
        data: {username,password}
    })
}
