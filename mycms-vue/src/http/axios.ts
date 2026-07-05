import axios from "axios";

const instance = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL,
    timeout: 50000, // 1000ms 有点短，容易超时
});

// 请求拦截器
instance.interceptors.request.use((config) => {
    // FormData 上传文件时必须交给浏览器自动生成 multipart boundary。
    if (config.data instanceof FormData) {
        delete config.headers['Content-Type'];
        return config;
    }
    // 只有当方法不是 GET 时，才强制设置 JSON 格式
    if (config.method?.toLowerCase() !== 'get') {
        config.headers['Content-Type'] = 'application/json;charset=UTF-8';
    }
    return config;
}, (error) => {
    return Promise.reject(error);
});
// 响应拦截器
instance.interceptors.response.use(
    (response) => {
        // 这里的 response.data 就是后端的 Result 对象
        const res = response.data;
        return res;
    }
);

export default <T = any>(options: any): Promise<T> => instance(options);
