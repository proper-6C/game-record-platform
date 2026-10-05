<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

/**
 * 霓虹星空背景（与主页 HomeView 同款：96 细星 + 36 小星 + 16 快闪亮点 + 6 柔焦光晕星）
 * fixed 全屏铺底，z-index 0，不挡交互；JS 动态生成星星 DOM（无 data-v），需 :deep 穿透。
 */
const box = ref(null)
let resizeClean = null

function buildStars() {
  if (!box.value) return
  let html = ''
  let i, x, y, d
  for (i = 0; i < 96; i++) {
    x = Math.random() * 100
    y = Math.random() * 100
    d = Math.random() * 4.5
    html += `<i class="tiny" style="left:${x.toFixed(1)}%;top:${y.toFixed(1)}%;animation-delay:-${d.toFixed(2)}s"></i>`
  }
  for (i = 0; i < 36; i++) {
    x = Math.random() * 100
    y = Math.random() * 100
    d = Math.random() * 3.4
    html += `<i class="small" style="left:${x.toFixed(1)}%;top:${y.toFixed(1)}%;animation-delay:-${d.toFixed(2)}s"></i>`
  }
  for (i = 0; i < 16; i++) {
    x = Math.random() * 100
    y = Math.random() * 100
    d = Math.random() * 1.7
    html += `<i class="spark" style="left:${x.toFixed(1)}%;top:${y.toFixed(1)}%;animation-delay:-${d.toFixed(2)}s"></i>`
  }
  const soft = [
    [14, 16, 'w'],
    [84, 12, 'c'],
    [78, 70, 'p'],
    [20, 76, 'w'],
    [44, 8, 'p'],
    [90, 42, 'c']
  ]
  for (i = 0; i < soft.length; i++) {
    x = soft[i][0] + Math.random() * 6
    y = soft[i][1] + Math.random() * 8
    d = Math.random() * 2.8
    html += `<i class="soft ${soft[i][2]}" style="left:${x.toFixed(1)}%;top:${y.toFixed(1)}%;animation-delay:-${d.toFixed(2)}s"></i>`
  }
  box.value.innerHTML = html
}

onMounted(() => {
  buildStars()
  const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  if (!reduce) {
    const onScroll = () => {
      if (box.value) box.value.style.transform = `translateY(${(window.scrollY * 0.06).toFixed(1)}px)`
    }
    window.addEventListener('scroll', onScroll, { passive: true })
    onScroll()
    resizeClean = () => window.removeEventListener('scroll', onScroll)
  }
})

onBeforeUnmount(() => {
  if (resizeClean) resizeClean()
})
</script>

<template>
  <div class="starfield" ref="box" aria-hidden="true"></div>
</template>

<style scoped>
.starfield {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  overflow: hidden;
  background:
    radial-gradient(1200px 500px at 15% -5%, rgba(192, 132, 252, 0.16), transparent 60%),
    radial-gradient(900px 420px at 90% 0%, rgba(34, 211, 238, 0.12), transparent 55%),
    #0a0612;
}
.starfield :deep(i) {
  position: absolute;
  border-radius: 50%;
}
.starfield :deep(i.tiny) {
  width: 1.2px;
  height: 1.2px;
  background: #dfe6f2;
  opacity: 0.55;
  animation: starfield-tw 4.5s ease-in-out infinite;
}
.starfield :deep(i.small) {
  width: 2px;
  height: 2px;
  background: #e8ecf4;
  opacity: 0.7;
  animation: starfield-tw 3.4s ease-in-out infinite;
}
.starfield :deep(i.spark) {
  width: 2.4px;
  height: 2.4px;
  background: #f7faff;
  box-shadow: 0 0 8px 1.5px rgba(247, 250, 255, 0.6);
  animation: starfield-tw 1.7s ease-in-out infinite;
}
.starfield :deep(i.soft) {
  width: 3px;
  height: 3px;
  animation: starfield-tw 2.8s ease-in-out infinite;
}
.starfield :deep(i.soft.w) {
  background: #f4f7fc;
  box-shadow: 0 0 14px 5px rgba(244, 247, 252, 0.5);
}
.starfield :deep(i.soft.p) {
  background: #c084fc;
  box-shadow: 0 0 18px 7px rgba(192, 132, 252, 0.55);
}
.starfield :deep(i.soft.c) {
  background: #67e8f9;
  box-shadow: 0 0 18px 7px rgba(103, 232, 249, 0.55);
}
@keyframes starfield-tw {
  0%,
  100% {
    opacity: 0.35;
  }
  50% {
    opacity: 1;
  }
}
@media (prefers-reduced-motion: reduce) {
  .starfield :deep(i) {
    animation: none;
    opacity: 0.6;
  }
}
</style>
