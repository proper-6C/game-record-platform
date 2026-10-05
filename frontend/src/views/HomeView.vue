<script setup>
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { getRecordList, getRecordStats } from '../api'
import HeaderActions from '../components/HeaderActions.vue'

const router = useRouter()

const featured = ref([])
const stats = ref(null)
const feed = ref([])

const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches

/* ---------- 主页导航登录态 ---------- */
const navScrolled = ref(false)
const navUser = ref(null)
const navLoggedIn = computed(() => !!localStorage.getItem('token'))
function refreshNavUser() {
  try {
    navUser.value = JSON.parse(localStorage.getItem('user') || 'null')
  } catch {
    navUser.value = null
  }
}
function navName() {
  return navUser.value?.nickname || navUser.value?.username || ''
}

/* ---------- 数据 ---------- */
async function loadFeatured() {
  try {
    const data = await getRecordList({ page: 1, size: 4, sortBy: 'hot' })
    featured.value = data.list
    await nextTick()
    observeReveals()
  } catch {}
}
async function loadStats() {
  try {
    stats.value = await getRecordStats()
    await nextTick()
    observeReveals()
  } catch {}
}
async function loadFeed() {
  try {
    const data = await getRecordList({ page: 1, size: 8, sortBy: 'latest' })
    feed.value = data.list
  } catch {}
}

function resultText(r) {
  return { win: '胜利', lose: '败北', draw: '平局' }[r] || ''
}
function coverUrl(r) {
  return r.coverUrl || ''
}
function fmt(n) {
  return Number(n || 0).toLocaleString('en-US')
}
function maxGameCnt() {
  if (!stats.value?.topGames?.length) return 1
  return Math.max(...stats.value.topGames.map((g) => Number(g.cnt) || 0))
}

/* ---------- 星空（霓虹闪烁 + 视差） ---------- */
const starBox = ref(null)
function buildStars() {
  if (!starBox.value) return
  let html = ''
  let i, x, y, d
  for (i = 0; i < 96; i++) {
    x = Math.random() * 100; y = Math.random() * 100; d = Math.random() * 4.5
    html += `<i class="tiny" style="left:${x.toFixed(1)}%;top:${y.toFixed(1)}%;animation-delay:-${d.toFixed(2)}s"></i>`
  }
  for (i = 0; i < 36; i++) {
    x = Math.random() * 100; y = Math.random() * 100; d = Math.random() * 3.4
    html += `<i class="small" style="left:${x.toFixed(1)}%;top:${y.toFixed(1)}%;animation-delay:-${d.toFixed(2)}s"></i>`
  }
  for (i = 0; i < 16; i++) {
    x = Math.random() * 100; y = Math.random() * 100; d = Math.random() * 1.7
    html += `<i class="spark" style="left:${x.toFixed(1)}%;top:${y.toFixed(1)}%;animation-delay:-${d.toFixed(2)}s"></i>`
  }
  const soft = [[14, 16, 'w'], [84, 12, 'c'], [78, 70, 'p'], [20, 76, 'w'], [44, 8, 'p'], [90, 42, 'c']]
  for (i = 0; i < soft.length; i++) {
    x = soft[i][0] + Math.random() * 6; y = soft[i][1] + Math.random() * 8; d = Math.random() * 2.8
    html += `<i class="soft ${soft[i][2]}" style="left:${x.toFixed(1)}%;top:${y.toFixed(1)}%;animation-delay:-${d.toFixed(2)}s"></i>`
  }
  starBox.value.innerHTML = html
}
function onStarScroll() {
  // 减少动态时不做星空视差平移，但导航滚动态始终生效
  if (!reduce && starBox.value) starBox.value.style.transform = `translateY(${(window.scrollY * 0.12).toFixed(1)}px)`
  navScrolled.value = window.scrollY > 40
}

/* ---------- 滚动揭示 + 数字递增 ---------- */
let revealIO = null
let statsIO = null

/** 收集所有尚未展示的 .reveal 元素并监听（含数据异步渲染出来的卡片/榜单） */
function observeReveals() {
  if (reduce || !('IntersectionObserver' in window)) {
    document.querySelectorAll('.home-root .reveal:not(.in)').forEach((el) => el.classList.add('in'))
    return
  }
  document.querySelectorAll('.home-root .reveal:not(.in)').forEach((el) => {
    if (revealIO) revealIO.observe(el)
  })
}
function animateCount(el, target, dur) {
  const t0 = performance.now()
  function step(t) {
    const p = Math.min(1, (t - t0) / dur)
    el.textContent = Math.round(target * (1 - Math.pow(1 - p, 3))).toLocaleString('en-US')
    if (p < 1) requestAnimationFrame(step)
  }
  requestAnimationFrame(step)
}
function runStatsAnims() {
  const els = document.querySelectorAll('.home-root [data-count]')
  els.forEach((el) => {
    if (el.dataset.done) return
    el.dataset.done = '1'
    if (reduce) el.textContent = fmt(el.dataset.count)
    else animateCount(el, Number(el.dataset.count) || 0, 1300)
  })
  document.querySelectorAll('.home-root [data-width]').forEach((el) => {
    if (!el.dataset.done) { el.dataset.done = '1'; el.style.width = el.dataset.width + '%' }
  })
}

onMounted(() => {
  refreshNavUser()
  loadFeatured()
  loadStats()
  loadFeed()
  window.addEventListener('auth-changed', refreshNavUser)
  /* 星星始终生成（静态星点），减少动态时仅停用闪烁动画与视差 */
  buildStars()
  window.addEventListener('scroll', onStarScroll, { passive: true })
  onStarScroll()
  /* 之前开启过氛围音乐：尝试自动恢复（浏览器策略拒绝则回关） */
  if (localStorage.getItem('home_bgm') === '1') {
    try {
      startBgm()
      bgmOn.value = true
      audioCtx?.resume().catch(() => {
        stopBgm()
        bgmOn.value = false
      })
    } catch {
      bgmOn.value = false
    }
  }
  /* 滚动揭示 */
  if (reduce || !('IntersectionObserver' in window)) {
    document.querySelectorAll('.home-root .reveal').forEach((el) => el.classList.add('in'))
    runStatsAnims()
  } else {
    revealIO = new IntersectionObserver((entries) => {
      entries.forEach((e) => { if (e.isIntersecting) { e.target.classList.add('in'); revealIO.unobserve(e.target) } })
    }, { threshold: 0.12 })
    statsIO = new IntersectionObserver((entries) => {
      entries.forEach((e) => { if (e.isIntersecting) { runStatsAnims(); statsIO.disconnect() } })
    }, { threshold: 0.3 })
    statsIO.observe(document.querySelector('.home-root #stats'))
  }
  observeReveals()
})

/* 统计接口返回后更新数字目标并触发递增（flush: post 保证拿到最新的 data-count 属性） */
watch(stats, (v) => {
  if (!v) return
  const done = document.querySelectorAll('.home-root [data-count]')
  let allDone = done.length > 0
  done.forEach((el) => { if (!el.dataset.done) allDone = false })
  if (!allDone) runStatsAnims()
}, { flush: 'post' })

onBeforeUnmount(() => {
  window.removeEventListener('scroll', onStarScroll)
  window.removeEventListener('auth-changed', refreshNavUser)
  if (revealIO) revealIO.disconnect()
  if (statsIO) statsIO.disconnect()
})

/* ---------- 主页氛围音乐（Web Audio 合成霓虹电子乐，需用户点击开启） ---------- */
const bgmOn = ref(false)
let audioCtx = null
let bgmTimer = null
let bgmState = null

/* 和弦进行：Am → F → C → G，每个 2 小节（32 步）一轮 */
const BGM_CHORDS = [
  { root: 220.0, chord: [220.0, 261.63, 329.63] },   // Am
  { root: 174.61, chord: [174.61, 220.0, 261.63] },  // F
  { root: 196.0, chord: [196.0, 246.94, 293.66] },   // C
  { root: 196.0, chord: [196.0, 246.94, 293.66] }    // G
]
/* A 小调五声音阶频率倍率 */
const BGM_PENTA = [1, 9 / 8, 6 / 5, 3 / 2, 5 / 3]

function bgmNote(freq, time, dur, type, gainVal, dest) {
  const o = audioCtx.createOscillator()
  o.type = type
  o.frequency.value = freq
  const g = audioCtx.createGain()
  g.gain.setValueAtTime(0, time)
  g.gain.linearRampToValueAtTime(gainVal, time + 0.02)
  g.gain.exponentialRampToValueAtTime(0.0001, time + dur)
  o.connect(g)
  g.connect(dest)
  o.start(time)
  o.stop(time + dur + 0.05)
}

function startBgm() {
  stopBgm()
  const Ctx = window.AudioContext || window.webkitAudioContext
  if (!Ctx) return
  audioCtx = new Ctx()
  const master = audioCtx.createGain()
  master.gain.value = 0.14
  master.connect(audioCtx.destination)
  // 简单延迟回声，增加空间感
  const delay = audioCtx.createDelay()
  delay.delayTime.value = 0.28
  const fb = audioCtx.createGain()
  fb.gain.value = 0.32
  delay.connect(fb)
  fb.connect(delay)
  const wet = audioCtx.createGain()
  wet.gain.value = 0.24
  delay.connect(wet)
  wet.connect(master)
  bgmState = { master, delay, step: 0 }
  bgmTimer = setInterval(stepBgm, 156) // ≈96 BPM 十六分音符
  audioCtx.resume().catch(() => {})
}

function stepBgm() {
  if (!audioCtx || !bgmState) return
  const t = audioCtx.currentTime + 0.05
  const step = bgmState.step % 32
  const chord = BGM_CHORDS[Math.floor(step / 8) % 4]
  // 每小节开头铺一层长音 pad
  if (step % 8 === 0) {
    chord.chord.forEach((f) => bgmNote(f, t, 3.4, 'sawtooth', 0.024, bgmState.master))
  }
  // 十六分音符琶音（带延迟回声）
  if (step % 2 === 0) {
    const idx = Math.floor(step / 2) % 4
    const f = chord.root * BGM_PENTA[idx % 5]
    bgmNote(f, t, 0.22, 'triangle', 0.05, bgmState.delay)
    bgmNote(f * 2, t, 0.16, 'triangle', 0.028, bgmState.master)
  }
  // 弱化四拍底鼓，稳住节奏
  if (step % 4 === 0) {
    const o = audioCtx.createOscillator()
    o.type = 'sine'
    o.frequency.setValueAtTime(110, t)
    o.frequency.exponentialRampToValueAtTime(38, t + 0.12)
    const g = audioCtx.createGain()
    g.gain.setValueAtTime(0.16, t)
    g.gain.exponentialRampToValueAtTime(0.001, t + 0.16)
    o.connect(g)
    g.connect(bgmState.master)
    o.start(t)
    o.stop(t + 0.2)
  }
  bgmState.step++
}

function stopBgm() {
  if (bgmTimer) {
    clearInterval(bgmTimer)
    bgmTimer = null
  }
  if (audioCtx) {
    audioCtx.close().catch(() => {})
    audioCtx = null
  }
  bgmState = null
}

function toggleBgm() {
  if (bgmOn.value) {
    stopBgm()
    bgmOn.value = false
    localStorage.removeItem('home_bgm')
  } else {
    try {
      startBgm()
      bgmOn.value = true
      localStorage.setItem('home_bgm', '1')
    } catch {
      ElMessage?.warning?.('浏览器限制了自动播放，请再点击一次')
    }
  }
}

// 离开主页时停止音乐
onBeforeUnmount(() => {
  stopBgm()
})
</script>

<template>
  <div class="home-root">
    <div class="stars" ref="starBox" aria-hidden="true"></div>

    <!-- ===== 霓虹导航（主页独立，不套全局顶栏） ===== -->
    <nav class="home-nav" :class="{ scrolled: navScrolled }">
      <button class="nav-logo" @click="router.push('/')">
        <span class="mark">GR</span>
        <span class="nav-title">游戏对局记录<small>GAME RECORD CENTER</small></span>
      </button>
      <div class="nav-links">
        <button class="nav-link" @click="router.push('/records')">全部对局</button>
        <button class="nav-link" @click="router.push('/upload')">上传对局</button>
        <HeaderActions />
        <button v-if="navLoggedIn" class="nav-user" @click="router.push('/profile')">
          <span class="nav-avatar">{{ (navName() || '?').charAt(0).toUpperCase() }}</span>
          <span class="nav-name">{{ navName() }}</span>
        </button>
        <button v-else class="nav-link nav-login" @click="router.push('/login')">登录 / 注册</button>
      </div>
    </nav>

    <!-- ===== Hero ===== -->
    <header class="hero" id="top">
      <div class="hero-inner">
        <div class="hero-eyebrow">GAME RECORD CENTER</div>
        <h1>
          <span class="line"><span>每一局，</span></span>
          <span class="line"><span class="grad">都是高光时刻</span></span>
        </h1>
        <p class="hero-sub">霓虹灯下的对局档案馆——上传你的名场面，让全场为你亮灯。点赞、评论、冲榜，把战绩点亮成社区的热度。</p>
        <div class="hero-cta">
          <button class="btn btn-primary" @click="router.push('/records')">
            进入对局库
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14M13 6l6 6-6 6"/></svg>
          </button>
          <button class="btn btn-ghost" @click="router.push('/upload')">上传对局</button>
        </div>
        <div class="hero-ticker" v-if="feed.length">
          <div class="tick">
            <span v-for="(r, i) in feed" :key="r.id + '-t1'"><b>{{ r.uploaderName }}</b>上传了对局《{{ r.gameName }}》</span>
            <span v-for="(r, i) in feed" :key="r.id + '-t2'"><b>{{ r.uploaderName }}</b>上传了对局《{{ r.gameName }}》</span>
          </div>
        </div>
      </div>
    </header>

    <!-- ===== 精选对局 ===== -->
    <section class="sec" id="featured">
      <div class="wrap">
        <div class="sec-head reveal">
          <div>
            <div class="kicker">FEATURED</div>
            <h2>精选对局</h2>
          </div>
          <button class="sec-more" @click="router.push('/records')">查看全部
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14M13 6l6 6-6 6"/></svg>
          </button>
        </div>
        <div v-if="featured.length" class="feature-grid">
          <article v-for="r in featured" :key="r.id" class="f-card reveal" @click="router.push(`/record/${r.id}`)">
            <div class="f-cover">
              <span v-if="r.result" class="f-badge" :class="r.result === 'win' ? 'win' : r.result === 'lose' ? 'lose' : ''">{{ resultText(r.result) }}</span>
              <span v-else class="f-badge video">视频对局</span>
              <img v-if="coverUrl(r)" :src="coverUrl(r)" :alt="r.gameName">
              <div v-else class="f-ph">
                <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="5" width="20" height="14" rx="3"/><path d="M10 9.5l5 3-5 3z"/></svg>
                <span>视频对局</span>
              </div>
            </div>
            <div class="f-body">
              <div class="f-game">{{ r.gameName }}<span v-if="r.gameMode" class="f-mode">{{ r.gameMode }}</span></div>
              <div class="f-title">{{ r.description || r.gameName + ' 对局' }}</div>
              <div class="f-foot">
                <span class="f-user" @click.stop="router.push(`/user/${r.uploaderId}`)">
                  <span class="f-avatar">{{ (r.uploaderName || '?').charAt(0).toUpperCase() }}</span>{{ r.uploaderName }}
                </span>
                <span class="f-nums">
                  <i><svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 3l2.7 5.5 6.1.9-4.4 4.3 1 6-5.4-2.9-5.4 2.9 1-6L2.6 9.4l6.1-.9z"/></svg>{{ fmt(r.likeCount) }}</i>
                  <i><svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 11.5a8.4 8.4 0 0 1-8.5 8.3c-1.2 0-2.4-.3-3.4-.8L3 20l1.1-5.7A8 8 0 1 1 21 11.5z"/></svg>{{ fmt(r.commentCount) }}</i>
                </span>
              </div>
            </div>
          </article>
        </div>
        <el-empty v-else description="暂无对局，快去上传第一局吧" :image-size="80" />
      </div>
    </section>

    <!-- ===== 平台数据 ===== -->
    <section class="sec" id="stats">
      <div class="wrap">
        <div class="sec-head reveal">
          <div>
            <div class="kicker">PLATFORM</div>
            <h2>平台数据</h2>
          </div>
          <p>霓虹之下，社区的每一次跳动都有记录。</p>
        </div>
        <div class="stat-grid">
          <div class="stat reveal"><div class="num"><span :data-count="stats ? stats.totalRecords : 0">0</span><em> 局</em></div><div class="lab">已上传对局</div><div class="cap"><i :style="{ width: (stats ? Math.min(100, Math.round(stats.totalRecords / 20)) : 0) + '%' }"></i></div></div>
          <div class="stat reveal"><div class="num"><span :data-count="stats ? stats.totalLikes : 0">0</span><em> 次</em></div><div class="lab">累计点赞</div><div class="cap"><i :style="{ width: (stats ? Math.min(100, Math.round(stats.totalLikes / 40)) : 0) + '%' }"></i></div></div>
          <div class="stat reveal"><div class="num"><span :data-count="stats ? stats.totalComments : 0">0</span><em> 条</em></div><div class="lab">累计评论</div><div class="cap"><i :style="{ width: (stats ? Math.min(100, Math.round(stats.totalComments / 15)) : 0) + '%' }"></i></div></div>
          <div class="stat reveal"><div class="num"><span :data-count="stats ? stats.totalUsers : 0">0</span><em> 位</em></div><div class="lab">注册玩家</div><div class="cap"><i :style="{ width: (stats ? Math.min(100, Math.round(stats.totalUsers / 5)) : 0) + '%' }"></i></div></div>
        </div>
      </div>
    </section>

    <!-- ===== 热门游戏榜 + 玩家获赞榜 ===== -->
    <section class="sec" id="rank">
      <div class="wrap">
        <div class="sec-head reveal">
          <div>
            <div class="kicker">HALL OF FAME</div>
            <h2>热门游戏 · 玩家榜</h2>
          </div>
          <button class="sec-more" @click="router.push('/records')">进入对局库
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14M13 6l6 6-6 6"/></svg>
          </button>
        </div>
        <div class="rank-duo">
          <div class="rank-panel reveal">
            <h3 class="rank-title">热门游戏榜</h3>
            <div v-if="stats?.topGames?.length" class="rank-ol">
              <div v-for="(g, idx) in stats.topGames" :key="g.gameName" class="rank-row2">
                <span class="rank-no" :class="{ top1: idx === 0 }">{{ String(idx + 1).padStart(2, '0') }}</span>
                <span class="rank-name">{{ g.gameName }}</span>
                <span class="rank-cnt">{{ fmt(g.cnt) }} 局</span>
                <span class="rank-bar"><i :style="{ width: Math.round((Number(g.cnt) / maxGameCnt()) * 100) + '%' }"></i></span>
              </div>
            </div>
            <p v-else class="rank-empty">暂无对局数据</p>
          </div>
          <div class="rank-panel reveal">
            <h3 class="rank-title">玩家获赞榜</h3>
            <div v-if="stats?.topPlayers?.length" class="rank-ol">
              <div v-for="(p, idx) in stats.topPlayers" :key="p.userId" class="rank-row2">
                <span class="rank-no" :class="{ top1: idx === 0 }">{{ String(idx + 1).padStart(2, '0') }}</span>
                <span class="rank-name">{{ p.username }}</span>
                <span class="rank-cnt"><b>{{ fmt(p.cnt) }}</b> 获赞</span>
                <span class="rank-bar"><i :style="{ width: Math.round((Number(p.cnt) / Math.max(1, ...stats.topPlayers.map((x) => Number(x.cnt)))) * 100) + '%' }"></i></span>
              </div>
            </div>
            <p v-else class="rank-empty">暂无点赞数据</p>
          </div>
        </div>
      </div>
    </section>

    <!-- ===== 页脚 ===== -->
    <footer class="foot">
      <div class="wrap">
        <div class="foot-bar">
          <span>© 2026 游戏对局记录 · 毕业设计项目</span>
          <span class="tag">电竞霓虹主页 · 已接入真实数据</span>
        </div>
      </div>
    </footer>

    <!-- ===== 氛围音乐开关（点击开启，Web Audio 合成霓虹电子乐） ===== -->
    <button class="bgm-btn" :class="{ on: bgmOn }" :title="bgmOn ? '关闭氛围音乐' : '开启氛围音乐'" @click="toggleBgm">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M9 18V5l12-2v13" />
        <circle cx="6" cy="18" r="3" />
        <circle cx="18" cy="16" r="3" />
      </svg>
    </button>
  </div>
</template>

<style scoped>
.home-root {
  position: relative;
  min-height: 100vh;
  background:
    radial-gradient(1000px 620px at 12% -6%, rgba(192,132,252,.16), transparent 60%),
    radial-gradient(900px 560px at 88% 110%, rgba(34,211,238,.12), transparent 60%),
    #0a0612;
  color: #f3eefc;
  font-size: 15px;
  line-height: 1.7;
  overflow: hidden;
}
.home-root img { display: block; max-width: 100%; }
.home-root button { font-family: inherit; cursor: pointer; }

/* ===== 主页独立导航（B 版霓虹） ===== */
.home-nav {
  position: fixed; top: 0; left: 0; right: 0; z-index: 50;
  display: flex; align-items: center; justify-content: space-between;
  height: 68px; padding: 0 28px;
  background: transparent;
  transition: background .35s cubic-bezier(.22,1,.36,1), box-shadow .35s cubic-bezier(.22,1,.36,1);
}
.home-nav.scrolled { background: rgba(10, 6, 18, .92); box-shadow: 0 1px 0 rgba(192,132,252,.16), 0 12px 32px rgba(0,0,0,.4); }
.nav-logo { display: flex; align-items: center; gap: 12px; font-weight: 900; letter-spacing: .04em; background: none; border: 0; color: #f3eefc; text-align: left; }
.nav-logo .mark {
  width: 34px; height: 34px; border-radius: 10px; display: grid; place-items: center;
  color: #0a0612; font-weight: 900; font-size: 15px;
  background: linear-gradient(135deg, #c084fc, #22d3ee);
  box-shadow: 0 0 18px rgba(192,132,252,.55);
}
.nav-title { display: flex; flex-direction: column; line-height: 1.15; font-size: 16px; }
.nav-title small { color: #6f6391; font-weight: 400; font-size: 11px; letter-spacing: .14em; margin-top: 3px; }
.nav-links { display: flex; align-items: center; gap: 6px; }
.nav-link {
  border: 0; background: transparent; color: #a99bc9;
  font-size: 14px; padding: 8px 14px; border-radius: 10px;
  transition: color .2s, background .2s, text-shadow .2s;
}
.nav-link:hover { color: #c084fc; text-shadow: 0 0 12px rgba(192,132,252,.7); background: rgba(192,132,252,.08); }
.nav-login {
  color: #22d3ee; border: 1px solid rgba(34,211,238,.4); margin-left: 8px;
  text-shadow: 0 0 8px rgba(34,211,238,.4);
}
.nav-login:hover { color: #67e8f9; border-color: #22d3ee; box-shadow: 0 0 18px rgba(34,211,238,.25); }
.nav-user { display: inline-flex; align-items: center; gap: 8px; background: none; border: 0; color: #a99bc9; margin-left: 8px; padding: 6px 10px; border-radius: 999px; transition: background .2s, color .2s; }
.nav-user:hover { background: rgba(192,132,252,.1); color: #f3eefc; }
.nav-avatar { width: 28px; height: 28px; border-radius: 50%; background: linear-gradient(135deg, #c084fc, #22d3ee); color: #0a0612; font-size: 12px; font-weight: 900; display: grid; place-items: center; }
.nav-name { max-width: 110px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 13.5px; }

/* ===== 霓虹星空 =====
   星星由 JS innerHTML 动态生成，不带 data-v 属性，必须用 :deep() 穿透 scoped 限制 */
.stars { position: absolute; inset: 0; z-index: 0; pointer-events: none; overflow: hidden; will-change: transform; }
.stars :deep(i) { position: absolute; border-radius: 50%; }
.stars :deep(i.tiny) { width: 1.2px; height: 1.2px; background: #dfe6f2; opacity: .55; animation: tw 4.5s ease-in-out infinite; }
.stars :deep(i.small) { width: 2px; height: 2px; background: #e8ecf4; opacity: .7; animation: tw 3.4s ease-in-out infinite; }
.stars :deep(i.spark) { width: 2.4px; height: 2.4px; background: #f7faff; box-shadow: 0 0 8px 1.5px rgba(247, 250, 255, .6); animation: tw 1.7s ease-in-out infinite; }
.stars :deep(i.soft) { width: 3px; height: 3px; animation: tw 2.8s ease-in-out infinite; }
.stars :deep(i.soft.w) { background: #f4f7fc; box-shadow: 0 0 14px 5px rgba(244, 247, 252, .5); }
.stars :deep(i.soft.p) { background: #c084fc; box-shadow: 0 0 18px 7px rgba(192, 132, 252, .55); }
.stars :deep(i.soft.c) { background: #67e8f9; box-shadow: 0 0 18px 7px rgba(103, 232, 249, .55); }
@keyframes tw { 0%, 100% { opacity: .35; } 50% { opacity: 1; } }

.wrap { max-width: 1160px; margin: 0 auto; padding: 0 24px; position: relative; z-index: 1; }
section { position: relative; z-index: 1; }

/* ===== Hero ===== */
.hero { position: relative; min-height: 100vh; display: flex; align-items: center; }
.hero-inner { position: relative; z-index: 2; max-width: 1160px; margin: 0 auto; padding: 120px 24px 90px; width: 100%; }
.hero-eyebrow { font-size: 12px; letter-spacing: .34em; color: #22d3ee; margin-bottom: 26px; display: flex; align-items: center; gap: 12px; text-shadow: 0 0 14px rgba(34,211,238,.6); }
.hero-eyebrow::before { content: ""; width: 38px; height: 1px; background: #22d3ee; box-shadow: 0 0 10px #22d3ee; }
.hero h1 { font-size: clamp(46px, 8.5vw, 100px); font-weight: 900; line-height: 1.1; letter-spacing: .02em; text-wrap: balance; }
.hero h1 .line { display: block; overflow: hidden; }
.hero h1 .line span {
  display: block; transform: translateY(112%);
  background: linear-gradient(92deg, #fff 12%, #c084fc 52%, #22d3ee 92%);
  -webkit-background-clip: text; background-clip: text; color: transparent;
  filter: drop-shadow(0 0 18px rgba(192,132,252,.35));
  animation: rise .8s cubic-bezier(.22,1,.36,1) forwards;
}
.hero h1 .line:nth-child(2) span { animation-delay: .12s; }
@keyframes rise { to { transform: translateY(0); } }
.hero-sub { margin-top: 26px; max-width: 560px; color: #a99bc9; font-size: 16px; font-weight: 300; opacity: 0; animation: fade .9s cubic-bezier(.22,1,.36,1) .5s forwards; }
@keyframes fade { to { opacity: 1; } }
.hero-cta { display: flex; gap: 14px; margin-top: 40px; flex-wrap: wrap; opacity: 0; animation: fade .9s cubic-bezier(.22,1,.36,1) .66s forwards; }
.btn { display: inline-flex; align-items: center; gap: 8px; padding: 13px 26px; border-radius: 12px; font-size: 15px; font-weight: 700; border: 1px solid transparent; transition: transform .2s cubic-bezier(.22,1,.36,1), box-shadow .2s, background .2s, color .2s; }
.btn-primary { background: linear-gradient(135deg, #c084fc, #22d3ee); color: #0a0612; box-shadow: 0 0 26px rgba(192,132,252,.35); }
.btn-primary:hover { transform: translateY(-2px); box-shadow: 0 0 40px rgba(192,132,252,.55); }
.btn-ghost { border-color: rgba(192,132,252,.4); color: #f3eefc; background: rgba(192,132,252,.06); }
.btn-ghost:hover { border-color: #22d3ee; color: #22d3ee; transform: translateY(-2px); box-shadow: 0 0 24px rgba(34,211,238,.25); }
.hero-ticker {
  margin-top: 54px; border-top: 1px solid rgba(192,132,252,.16); border-bottom: 1px solid rgba(192,132,252,.16);
  padding: 12px 0; overflow: hidden; white-space: nowrap; opacity: 0; animation: fade .9s cubic-bezier(.22,1,.36,1) .8s forwards;
}
.hero-ticker .tick { display: inline-block; animation: marquee 26s linear infinite; }
.hero-ticker span { margin-right: 56px; color: #a99bc9; font-size: 13px; }
.hero-ticker b { color: #c084fc; font-weight: 700; margin-right: 8px; }
@keyframes marquee { to { transform: translateX(-50%); } }

/* ===== 区块 ===== */
.sec { padding: 104px 0; }
.sec-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; margin-bottom: 44px; }
.sec-head .kicker { font-size: 12px; letter-spacing: .3em; color: #c084fc; margin-bottom: 12px; text-shadow: 0 0 10px rgba(192,132,252,.5); }
.sec-head h2 {
  font-size: clamp(26px, 4vw, 38px); font-weight: 900; letter-spacing: .02em;
  background: linear-gradient(90deg, #f3eefc 30%, #c084fc 70%, #22d3ee);
  -webkit-background-clip: text; background-clip: text; color: transparent;
}
.sec-head p { color: #a99bc9; font-weight: 300; font-size: 14px; max-width: 380px; }
.sec-more { color: #22d3ee; font-size: 14px; white-space: nowrap; background: none; border: 0; display: inline-flex; align-items: center; gap: 6px; padding: 4px 0; }
.sec-more:hover svg { transform: translateX(4px); }
.sec-more svg { transition: transform .25s cubic-bezier(.22,1,.36,1); }
.reveal { opacity: 0; transform: translateY(26px); transition: opacity .7s cubic-bezier(.22,1,.36,1), transform .7s cubic-bezier(.22,1,.36,1); }
.reveal.in { opacity: 1; transform: none; }

/* ===== 精选对局 ===== */
.feature-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; }
.f-card {
  position: relative; border-radius: 14px; overflow: hidden;
  background: #151022; border: 1px solid rgba(192,132,252,.16);
  cursor: pointer;
  transition: transform .25s cubic-bezier(.22,1,.36,1), border-color .25s, box-shadow .25s;
}
.f-card:hover {
  transform: translateY(-6px); border-color: rgba(192,132,252,.6);
  box-shadow: 0 0 30px rgba(192,132,252,.28), 0 18px 40px rgba(0,0,0,.4);
}
.f-cover { position: relative; aspect-ratio: 16 / 10; overflow: hidden; background: #0d0a18; }
.f-cover img { width: 100%; height: 100%; object-fit: cover; transition: transform .6s cubic-bezier(.22,1,.36,1); }
.f-card:hover .f-cover img { transform: scale(1.06); }
.f-cover::after { content: ""; position: absolute; inset: 0; background: linear-gradient(180deg, transparent 45%, rgba(10,6,18,.9)); }
.f-ph { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; color: #6f6391; font-size: 13px; }
.f-badge {
  position: absolute; top: 14px; left: 14px; z-index: 2;
  font-size: 11px; font-weight: 700; padding: 4px 10px; border-radius: 999px;
  background: rgba(10,6,18,.75); border: 1px solid rgba(192,132,252,.16);
}
.f-badge.win { color: #34d399; border-color: rgba(52,211,153,.45); text-shadow: 0 0 8px rgba(52,211,153,.5); }
.f-badge.lose { color: #fb7185; border-color: rgba(251,113,133,.45); text-shadow: 0 0 8px rgba(251,113,133,.5); }
.f-badge.video { display: inline-flex; align-items: center; gap: 5px; color: #f3eefc; }
.f-badge.video::before { content: ""; width: 6px; height: 6px; border-radius: 50%; background: #22d3ee; box-shadow: 0 0 8px #22d3ee; }
.f-body { padding: 16px 18px 18px; }
.f-game { font-size: 12px; color: #c084fc; font-weight: 700; letter-spacing: .05em; text-shadow: 0 0 8px rgba(192,132,252,.4); }
.f-mode { margin-left: 6px; color: #6f6391; font-weight: 500; text-shadow: none; }
.f-title { font-size: 17px; font-weight: 700; margin: 4px 0 12px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.f-foot { display: flex; align-items: center; justify-content: space-between; color: #6f6391; font-size: 12.5px; }
.f-user { display: flex; align-items: center; gap: 8px; cursor: pointer; transition: color 0.2s, text-shadow 0.2s; }
.f-user:hover { color: var(--accent, #c084fc); text-shadow: 0 0 10px rgba(192, 132, 252, 0.5); }
.f-avatar { width: 24px; height: 24px; border-radius: 50%; background: linear-gradient(135deg, #c084fc, #22d3ee); color: #0a0612; font-size: 11px; font-weight: 900; display: grid; place-items: center; }
.f-nums { display: flex; gap: 14px; font-variant-numeric: tabular-nums; }
.f-nums i { font-style: normal; display: inline-flex; align-items: center; gap: 5px; }
.f-nums svg { opacity: .8; }

/* ===== 平台数据 ===== */
#stats { background: #0e081a; border-top: 1px solid rgba(192,132,252,.16); border-bottom: 1px solid rgba(192,132,252,.16); }
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; }
.stat { padding: 34px 28px; border-radius: 14px; background: #151022; border: 1px solid rgba(192,132,252,.16); position: relative; overflow: hidden; }
.stat::before { content: ""; position: absolute; left: 0; top: 0; bottom: 0; width: 3px; background: linear-gradient(180deg, #c084fc, #22d3ee); box-shadow: 0 0 14px rgba(192,132,252,.6); }
.stat .num {
  font-size: clamp(30px, 4vw, 44px); font-weight: 900; font-variant-numeric: tabular-nums;
  background: linear-gradient(180deg, #fff, #c084fc); -webkit-background-clip: text; background-clip: text; color: transparent;
  filter: drop-shadow(0 0 14px rgba(192,132,252,.3));
}
.stat .num em { font-style: normal; font-size: .6em; font-weight: 700; -webkit-text-fill-color: #6f6391; }
.stat .lab { margin-top: 6px; color: #a99bc9; font-size: 14px; }
.stat .cap { margin-top: 16px; height: 4px; border-radius: 4px; background: rgba(148,163,184,.1); overflow: hidden; }
.stat .cap i { display: block; height: 100%; border-radius: 4px; background: linear-gradient(90deg, #c084fc, #22d3ee); box-shadow: 0 0 10px rgba(192,132,252,.6); transition: width 1.4s cubic-bezier(.22,1,.36,1); }

/* ===== 排行榜 ===== */
.rank-duo { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.rank-panel { border-radius: 14px; background: #151022; border: 1px solid rgba(192,132,252,.16); padding: 26px 24px 20px; }
.rank-title { font-size: 15px; font-weight: 700; margin-bottom: 14px; color: #f3eefc; display: flex; align-items: center; gap: 10px; }
.rank-title::before { content: ""; width: 4px; height: 16px; border-radius: 2px; background: linear-gradient(180deg, #c084fc, #22d3ee); box-shadow: 0 0 10px rgba(192,132,252,.6); }
.rank-ol { display: grid; gap: 4px; }
.rank-row2 { display: grid; grid-template-columns: 44px 1fr 92px 120px; align-items: center; gap: 12px; padding: 13px 10px; border-radius: 10px; transition: background .2s, transform .2s cubic-bezier(.22,1,.36,1); }
.rank-row2:hover { background: rgba(192,132,252,.07); transform: translateX(4px); }
.rank-no { font-size: 22px; font-weight: 900; color: transparent; -webkit-text-stroke: 1.3px #c084fc; font-variant-numeric: tabular-nums; }
.rank-no.top1 { color: #22d3ee; -webkit-text-stroke: 0; text-shadow: 0 0 18px rgba(34,211,238,.6); }
.rank-name { font-size: 15px; font-weight: 700; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.rank-cnt { font-size: 12.5px; color: #a99bc9; font-variant-numeric: tabular-nums; text-align: right; white-space: nowrap; }
.rank-cnt b { color: #c084fc; font-weight: 900; font-size: 15px; }
.rank-bar { height: 5px; border-radius: 5px; background: rgba(120,150,205,.12); overflow: hidden; }
.rank-bar i { display: block; height: 100%; border-radius: 5px; background: linear-gradient(90deg, #22d3ee, #c084fc); box-shadow: 0 0 8px rgba(192,132,252,.5); }
.rank-empty { color: #6f6391; font-size: 13.5px; padding: 20px 10px; }

/* ===== 氛围音乐按钮 ===== */
.bgm-btn {
  position: fixed;
  right: 26px;
  bottom: 26px;
  z-index: 60;
  width: 46px;
  height: 46px;
  border-radius: 50%;
  border: 1px solid rgba(192, 132, 252, 0.4);
  background: rgba(16, 10, 32, 0.72);
  color: #c084fc;
  cursor: pointer;
  display: grid;
  place-items: center;
  backdrop-filter: blur(6px);
  transition: color 0.25s, box-shadow 0.25s, border-color 0.25s, transform 0.25s;
}
.bgm-btn:hover {
  color: #e9d5ff;
  border-color: rgba(192, 132, 252, 0.8);
  box-shadow: 0 0 18px rgba(192, 132, 252, 0.5);
  transform: translateY(-2px);
}
.bgm-btn.on {
  color: #22d3ee;
  border-color: rgba(34, 211, 238, 0.65);
  box-shadow: 0 0 22px rgba(34, 211, 238, 0.45);
  animation: bgm-spin 9s linear infinite;
}
.bgm-btn.on svg { animation: bgm-pulse 1.1s ease-in-out infinite; }
@keyframes bgm-spin { to { transform: rotate(360deg); } }
@keyframes bgm-pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }

/* ===== 页脚 ===== */
.foot { border-top: 1px solid rgba(192,132,252,.16); padding: 40px 0 30px; color: #6f6391; font-size: 13px; position: relative; z-index: 1; }
.foot-bar { display: flex; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
.foot .tag { color: #22d3ee; border: 1px solid rgba(34,211,238,.35); border-radius: 999px; padding: 2px 10px; font-size: 11px; }

/* ===== 响应式 ===== */
@media (max-width: 1080px) {
  .feature-grid { grid-template-columns: repeat(2, 1fr); }
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
  .rank-duo { grid-template-columns: 1fr; }
  .rank-row2 { grid-template-columns: 40px 1fr 92px 110px; }
}
@media (max-width: 640px) {
  .home-nav { padding: 0 16px; }
  .nav-links .nav-link:not(.nav-login) { display: none; }
  .nav-title small { display: none; }
  .hero-inner { padding-top: 96px; }
  .sec { padding: 72px 0; }
  .sec-head { flex-direction: column; align-items: flex-start; }
  .feature-grid { grid-template-columns: 1fr; }
  .stat-grid { grid-template-columns: 1fr 1fr; }
  .rank-row2 { grid-template-columns: 36px 1fr 70px; }
  .rank-bar { display: none; }
}

@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after { animation: none !important; transition: none !important; }
  .hero h1 .line span, .hero-sub, .hero-cta, .hero-ticker { transform: none; opacity: 1; }
  .reveal { opacity: 1; transform: none; }
}
</style>
