import {createRouter,createWebHashHistory} from 'vue-router'
import Login from "@/views/Login.vue";
import Login2 from "@/views/Login2.vue";
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
        component: ()=>{return import('@/views/Menu.vue')},
    },
    ]
})
export default router