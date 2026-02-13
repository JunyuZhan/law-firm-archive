<template>
  <div class="watermark-container" ref="containerRef">
    <slot />
    <div ref="watermarkRef" class="watermark-layer" :style="watermarkStyle" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'

const props = defineProps({
  // 水印文字（支持数组，多行显示）
  text: {
    type: [String, Array],
    default: ''
  },
  // 是否显示时间戳
  showTimestamp: {
    type: Boolean,
    default: false
  },
  // 时间戳格式
  timestampFormat: {
    type: String,
    default: 'YYYY-MM-DD HH:mm'
  },
  // 字体大小
  fontSize: {
    type: Number,
    default: 16
  },
  // 字体颜色
  fontColor: {
    type: String,
    default: 'rgba(0, 0, 0, 0.08)'
  },
  // 字体
  fontFamily: {
    type: String,
    default: '"PingFang SC", "Microsoft YaHei", sans-serif'
  },
  // 旋转角度
  rotate: {
    type: Number,
    default: -22
  },
  // 水印间距 [水平间距, 垂直间距]
  gap: {
    type: Array,
    default: () => [120, 100]
  },
  // 水印偏移 [水平偏移, 垂直偏移]
  offset: {
    type: Array,
    default: () => [0, 0]
  },
  // 是否显示
  show: {
    type: Boolean,
    default: true
  },
  // 防篡改模式（防止通过DevTools删除水印）
  antiTamper: {
    type: Boolean,
    default: true
  },
  // z-index
  zIndex: {
    type: Number,
    default: 9999
  }
})

const containerRef = ref(null)
const watermarkRef = ref(null)
const watermarkStyle = ref({})
let mutationObserver = null
let resizeObserver = null
let isDestroyed = false

// 获取水印文本（处理多行和时间戳）
const getWatermarkTexts = () => {
  const texts = Array.isArray(props.text) ? [...props.text] : [props.text]
  
  if (props.showTimestamp) {
    texts.push(formatTimestamp())
  }
  
  return texts.filter(t => t)
}

// 格式化时间戳
const formatTimestamp = () => {
  const now = new Date()
  const format = props.timestampFormat
  
  const pad = (n) => String(n).padStart(2, '0')
  
  return format
    .replace('YYYY', now.getFullYear())
    .replace('MM', pad(now.getMonth() + 1))
    .replace('DD', pad(now.getDate()))
    .replace('HH', pad(now.getHours()))
    .replace('mm', pad(now.getMinutes()))
    .replace('ss', pad(now.getSeconds()))
}

// 生成水印图片
const generateWatermarkImage = () => {
  const texts = getWatermarkTexts()
  if (!texts.length) return ''
  
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')
  const ratio = window.devicePixelRatio || 1
  
  // 设置字体并测量文字
  const font = `${props.fontSize * ratio}px ${props.fontFamily}`
  ctx.font = font
  
  // 计算文本最大宽度和总高度
  let maxWidth = 0
  const lineHeight = props.fontSize * ratio * 1.4
  texts.forEach(text => {
    const metrics = ctx.measureText(text)
    maxWidth = Math.max(maxWidth, metrics.width)
  })
  const totalHeight = lineHeight * texts.length
  
  // 计算画布大小（包含间距）
  const [gapX, gapY] = props.gap
  const canvasWidth = maxWidth + gapX * ratio
  const canvasHeight = totalHeight + gapY * ratio
  
  // 设置画布大小
  canvas.width = canvasWidth
  canvas.height = canvasHeight
  
  // 重新设置字体（canvas大小改变后需要重设）
  ctx.font = font
  ctx.fillStyle = props.fontColor
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  
  // 绘制多行文字
  const startY = (canvasHeight - totalHeight) / 2 + lineHeight / 2
  texts.forEach((text, index) => {
    ctx.fillText(text, canvasWidth / 2, startY + index * lineHeight)
  })
  
  return canvas.toDataURL('image/png')
}

// 更新水印样式
const updateWatermark = () => {
  if (!props.show || isDestroyed) {
    watermarkStyle.value = { display: 'none' }
    return
  }
  
  const watermarkImage = generateWatermarkImage()
  if (!watermarkImage) {
    watermarkStyle.value = { display: 'none' }
    return
  }
  
  const [offsetX, offsetY] = props.offset
  
  watermarkStyle.value = {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    pointerEvents: 'none',
    zIndex: props.zIndex,
    backgroundImage: `url(${watermarkImage})`,
    backgroundRepeat: 'repeat',
    backgroundPosition: `${offsetX}px ${offsetY}px`,
    transform: `rotate(${props.rotate}deg)`,
    // 扩大范围以覆盖旋转后的空白区域
    width: '150%',
    height: '150%',
    marginLeft: '-25%',
    marginTop: '-25%'
  }
}

// 防篡改：监听DOM变化
const setupAntiTamper = () => {
  if (!props.antiTamper || !watermarkRef.value) return
  
  mutationObserver = new MutationObserver((mutations) => {
    if (isDestroyed) return
    
    let needRestore = false
    
    mutations.forEach(mutation => {
      // 检查水印是否被删除
      if (mutation.type === 'childList') {
        mutation.removedNodes.forEach(node => {
          if (node === watermarkRef.value) {
            needRestore = true
          }
        })
      }
      
      // 检查样式是否被修改
      if (mutation.type === 'attributes' && mutation.target === watermarkRef.value) {
        needRestore = true
      }
    })
    
    if (needRestore) {
      // 重新添加水印
      nextTick(() => {
        if (!isDestroyed && containerRef.value && watermarkRef.value) {
          updateWatermark()
          if (!containerRef.value.contains(watermarkRef.value)) {
            containerRef.value.appendChild(watermarkRef.value)
          }
        }
      })
    }
  })
  
  if (containerRef.value) {
    mutationObserver.observe(containerRef.value, {
      childList: true,
      subtree: true,
      attributes: true,
      attributeFilter: ['style', 'class']
    })
  }
}

// 监听属性变化
watch(
  () => [
    props.text, 
    props.fontSize, 
    props.fontColor, 
    props.rotate, 
    props.gap, 
    props.offset,
    props.show,
    props.showTimestamp
  ],
  () => {
    updateWatermark()
  },
  { deep: true }
)

onMounted(() => {
  updateWatermark()
  
  // 设置防篡改
  if (props.antiTamper) {
    setupAntiTamper()
  }
  
  // 监听容器大小变化
  if (typeof ResizeObserver !== 'undefined' && containerRef.value) {
    resizeObserver = new ResizeObserver(() => {
      updateWatermark()
    })
    resizeObserver.observe(containerRef.value)
  }
  
  // 定时更新时间戳
  if (props.showTimestamp) {
    const updateInterval = setInterval(() => {
      if (!isDestroyed) {
        updateWatermark()
      }
    }, 60000) // 每分钟更新一次
    
    onUnmounted(() => clearInterval(updateInterval))
  }
})

onUnmounted(() => {
  isDestroyed = true
  
  if (mutationObserver) {
    mutationObserver.disconnect()
  }
  
  if (resizeObserver) {
    resizeObserver.disconnect()
  }
})

// 暴露方法
defineExpose({
  refresh: updateWatermark
})
</script>

<style lang="scss" scoped>
.watermark-container {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.watermark-layer {
  user-select: none !important;
  -webkit-user-select: none !important;
  -moz-user-select: none !important;
  -ms-user-select: none !important;
}
</style>
