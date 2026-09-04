<script setup lang="ts">

import Top from "@/index/components/top.vue";
import Main from "@/index/components/main.vue";
import Footer from "@/index/components/footer.vue";

</script>

<template>
  <div class="index">
    <Top></Top>
    <Main></Main>
    <Footer></Footer>
  </div>
</template>

<style>
  *{
    padding: 0;
    margin: 0;
  }
  .index{
    height: 1000px;
  }
</style>