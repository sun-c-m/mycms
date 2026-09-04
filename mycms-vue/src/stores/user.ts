import {defineStore} from 'pinia';
import {ref, computed} from 'vue';

export const useUserStore = defineStore('user', () => {
    // --- 1. 状态声明 ---
    // 初始值直接给空即可，插件会自动从 localStorage 加载覆盖它们
    const token = ref('');
    const userInfo = ref<any>(null);

    // --- 2. 计算属性 (Getters) ---
    const permissions = computed(() => {
        // 插件恢复数据后，userInfo 会有值，这里会自动计算
        return userInfo.value?.permissionList?.map((p: any) => p.code) || [];
    });

    // --- 3. Actions ---
    const setToken = (newToken: string) => {
        token.value = newToken;
        // 不再需要手动 localStorage.setItem
    };

    const setUserInfo = (info: any) => {
        userInfo.value = info;
    };

    const logout = () => {
        token.value = '';
        userInfo.value = null;
        // 不再需要手动 localStorage.removeItem
    };

    return {
        token,
        userInfo,
        permissions,
        setToken,
        setUserInfo,
        logout
    };
}, {
    // --- 4. 开启持久化配置 ---
    persist: {
        key: 'my-cms-user', // 存储在 localStorage 里的 key 名
        storage: localStorage, // 存储方式，默认为 localStorage
        // paths: ['token', 'userInfo'] // 如果只想持久化部分字段，可以取消注释
    }
});