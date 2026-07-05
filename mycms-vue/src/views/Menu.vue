<script setup lang="ts">
import {computed, ref} from "vue";
const menuData = ref([
  {id: 1111, name: '首页', path: '/dashboard', children: []},
  {
    id: 2222, name: '账号管理', children: [{id: 22220, name: '用户管理', url: '/dashboard/user/userManage'}]
  },
  {
    id: 3333, name: '角色管理', children: [
      {id: 33330, name: '角色信息', path: '/dashboard/role/roleInfo'}
    ]
  },
  {
    id: 5555, name: '订单管理', children: [
      {id: 55550, name: '我的订单', path: ''}
    ]
  },
  {
    id: 6666, name: '发票管理', children: [
      {id: 66660, name: '发票查询', path: ''}
    ]
  },
  {
    id: 8888, name: '使用记录', children: [
      {id: 88880, name: '数据统计', path: ''},
      {id: 88881, name: '我的信息', path: ''},
      {id: 88882, name: '我的数据', path: ''}
    ]
  }
])
interface MenuItem {
  id: number
  name: string
  path: string
  children: MenuItem[]
}
const activeIndex = ref<number>(0);
const toggleMenu = (idx: number) => {
  //如何拿到你所点击的菜单的索引呢？
  if (activeIndex.value == idx) {
    activeIndex.value = -1
  } else {
    activeIndex.value = idx;
  }
}
</script>

<template>
  <div class="common-layout">
    <el-container>
      <el-header>Header</el-header>
      <el-container>
        <el-aside width="200px">
        <div class="aside">
          <ul class="menu">
            <li v-for="(menu,idx) in menuData" :key="menu.id">
              <div class="menu-title" v-bind:class="{active: activeIndex==idx}" @click="toggleMenu(idx)">
                <div class="left">
                  <div class="icon"></div>
                  <router-link v-if="menu.path && menu.children.length === 0" :to="menu.path">{{ menu.name }}</router-link>
                  <span v-else>{{ menu.name }}</span>
                </div>
                <i v-if="menu.children.length>0" class="arrow"></i>
              </div>
              <ul v-if="menu.children.length>0" class="submenu" :class="{show: activeIndex==idx}">
                <li v-for="secondMenu in menu.children" :key="secondMenu.id">
                  <router-link v-if="secondMenu.path" :to="secondMenu.path">{{ secondMenu.name }}</router-link>
                  <span v-else>{{ secondMenu.name }}</span>
                </li>
              </ul>
            </li>
          </ul>
        </div>
        </el-aside>
        <el-main>Main</el-main>
      </el-container>
    </el-container>
  </div>
</template>

<style scoped>
.aside {
  width: 260px;
  background: #fff;
  border-right: 1px solid #e5e6eb;
  height: 100vh;
}

li a {
  text-decoration: none;
  color: #999999;
}

.menu {
  list-style: none;
  padding: 0;
  margin: 0;
}

.menu > li {
  border-bottom: 1px solid #eee;
}

.menu-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  cursor: pointer;
  color: #333;
}

.menu-title.active {
  color: #1677ff;
}

.menu-title .left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.icon {
  width: 18px;
  height: 18px;
  background: #1677ff;
  border-radius: 3px;
  opacity: 0.85;
}

.menu-title:not(.active) .icon {
  background: #999;
  opacity: 0.6;
}

.arrow {
  border: solid #666;
  border-width: 0 2px 2px 0;
  display: inline-block;
  padding: 4px;
  transform: rotate(45deg);
  transition: transform 0.2s;
}

.menu-title.active .arrow {
  transform: rotate(-135deg);
  border-color: #1677ff;
}

.submenu {
  list-style: none;
  padding-left: 32px;
  margin: 0;
  display: none;
  position: relative;
}

.submenu::before {
  content: "";
  position: absolute;
  left: 18px;
  top: 0;
  bottom: 0;
  border-left: 1px dotted #c0c4cc;
}

.submenu li {
  padding: 10px 0 10px 20px;
  color: #666;
  position: relative;
  cursor: pointer;
}

.submenu li::before {
  content: "";
  position: absolute;
  left: -2px;
  top: 50%;
  width: 12px;
  border-top: 1px dotted #c0c4cc;
}

.submenu li:hover {
  color: #1677ff;
}

.submenu.show {
  display: block;
}

</style>