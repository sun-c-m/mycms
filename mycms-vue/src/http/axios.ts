import axios from "axios";
import {useUserStore} from "@/stores/user.ts";
import {ElMessage} from "element-plus";

// All API modules use paths beginning with `/api`.  Treat the optional
// environment value as the server origin, not as another `/api` prefix.
const configuredBaseUrl = String(import.meta.env.VITE_API_BASE_URL || '').replace(/\/+$/, '');
const baseURL = configuredBaseUrl === '/api'
    ? ''
    : configuredBaseUrl.endsWith('/api')
        ? configuredBaseUrl.slice(0, -4)
        : configuredBaseUrl;

const instance = axios.create({
    baseURL,
    timeout: 5000, // 1000ms 有点短，容易超时
});

// 请求拦截器
instance.interceptors.request.use((config) => {
    // 【核心】在这里获取 store 实例
    const userStore = useUserStore();
    // 直接从 store 中拿 token
    if (userStore.token) {
        // 按照后端要求的格式塞进请求头，一般是 Authorization 或 token
        config.headers['schoolName'] = 'guet';
        config.headers['Authorization'] = `Bearer ${userStore.token}`;
    }
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

        // --- 情况 1：后端返回 200 OK，但在 Result 对象里封装了 403 ---
        if (res.code === 403) {
            ElMessage.error(res.message || '权限不足，无法操作');
            return Promise.reject(new Error(res.message || 'Forbidden'));
        }

        // 如果业务代码不是 200 (假设你的成功码是 200)，可以在这里统一弹窗
        if (res.code !== 200) {
            ElMessage.warning(res.message || '操作失败');
            return Promise.reject(res);
        }

        return res;
    },
    (error) => {
        // --- 情况 2：后端直接抛出了非 200 的 HTTP 状态码 ---
        if (error.response) {
            switch (error.response.status) {
                case 401:
                    ElMessage.error("登录状态失效，请重新登录");
                    const userStore = useUserStore();
                    userStore.logout(); // 建议调用 store 的 logout 清理状态
                    window.location.href = '/login';
                    break;
                case 403:
                    // 处理真正的 HTTP 403 错误
                    ElMessage.error("对不起，您没有权限执行此操作");
                    break;
                case 500:
                    ElMessage.error("服务器开小差了，请稍后再试");
                    break;
                default:
                    ElMessage.error(error.response.data?.message || "网络错误");
            }
        } else {
            ElMessage.error("连接服务器失败");
        }
        return Promise.reject(error);
    }
);

export default (options: any) => instance(options);
