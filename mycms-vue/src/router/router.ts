import {createRouter,createWebHashHistory} from 'vue-router'
import Login from "@/views/Login.vue";
import Login2 from "@/views/Login2.vue";
import Menu from "@/views/Menu.vue";
const router = createRouter({
    history: createWebHashHistory(),
    routes: [{
        path:'/',
        component: Login
    },
    {
        path:'/Login2',
        component: Login2
    },
        {
        path:'/menu',
        component: Menu,
        meta: { requiresAuth: true },
    },
    {
        path: '/:pathMatch(.*)*',
        redirect: '/',
    },
    ]
})
router.beforeEach((to) => {
    if (to.meta.requiresAuth && !sessionStorage.getItem('user')) return '/'
    if (to.path === '/' && sessionStorage.getItem('user')) return '/menu'
})
export default router
