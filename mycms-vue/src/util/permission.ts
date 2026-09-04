import { useUserStore } from '@/stores/user.ts'

export function hasPermission(code: string) {
    const userStore = useUserStore();
    // 这里的 permissions 是我们在 pinia 里定义的 computed，它是 string[] 类型
    return userStore.permissions.includes(code);
}