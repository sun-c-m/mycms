import axios from "@/http/axios.ts";

export const getUser=((username:String,password:String)=>{
    return axios({
        url:'/api/login/getUser',
        method: "POST",
        data: {username,password}
    })
})