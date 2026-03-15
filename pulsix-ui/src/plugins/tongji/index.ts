import router from '@/router'

type HmtTrackCommand = [string, ...unknown[]]
type HmtWindow = Window & { _hmt?: HmtTrackCommand[] }

// 用于 router push
const hmt = (((window as HmtWindow)._hmt ??= []) as HmtTrackCommand[])
// HM_ID
const HM_ID = import.meta.env.VITE_APP_BAIDU_CODE
;(function () {
  // 有值的时候，才开启
  if (!HM_ID) {
    return
  }
  const hm = document.createElement('script')
  hm.src = 'https://hm.baidu.com/hm.js?' + HM_ID
  const s = document.getElementsByTagName('script')[0]
  s.parentNode?.insertBefore(hm, s)
})()

router.afterEach(function (to) {
  if (!HM_ID) {
    return
  }
  hmt.push(['_trackPageview', to.fullPath])
})
