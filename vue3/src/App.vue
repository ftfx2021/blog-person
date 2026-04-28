<template>
  <router-view/>
</template>

<style lang="scss">
/* 全局平滑滚动 */
html {
  scroll-behavior: smooth;
}

#app {
  
}

/* 全局滚动条美化 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
  transition: background 0.3s ease;
}

::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

::-webkit-scrollbar-thumb:active {
  background: #909090;
}

/* Firefox 滚动条美化 */
* {
  scrollbar-width: thin;
  scrollbar-color: #c1c1c1 #f1f1f1;
}
</style>
