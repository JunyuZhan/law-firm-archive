<template>
  <div class="watermark-container" ref="containerRef">
    <slot />
    <canvas ref="canvasRef" class="watermark-canvas" />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'

const props = defineProps({
  // 水印文字
  text: {
    type: String,
    default: ''
  },
  // 字体大小
  fontSize: {
    type: Number,
    default: 16
  },
  // 字体颜色
  fontColor: {
    type: String,
    default: 'rgba(0, 0, 0, 0.1)'
  },
  // 旋转角度
  rotate: {
    type: Number,
    default: -25
  },
  // 水印间距
  gap: {
    type: Array,
    default: () => [100, 80]
  },
  // 是否显示
  show: {
    type: Boolean,
    default: true
  }
})

const containerRef = ref(null)
const canvasRef = ref(null)
let resizeObserver = null

// 绘制水印
const drawWatermark = () => {
  if (!canvasRef.value || !containerRef.value || !props.show || !props.text) {
    return
  }

  const container = containerRef.value
  const canvas = canvasRef.value
  const ctx = canvas.getContext('2d')

  // 获取容器尺寸
  const width = container.offsetWidth
  const height = container.offsetHeight

  // 设置canvas尺寸（使用设备像素比提升清晰度）
  const ratio = window.devicePixelRatio || 1
  canvas.width = width * ratio
  canvas.height = height * ratio
  canvas.style.width = `${width}px`
  canvas.style.height = `${height}px`
  ctx.scale(ratio, ratio)

  // 清除画布
  ctx.clearRect(0, 0, width, height)

  // 设置文字样式
  ctx.font = `${props.fontSize}px "PingFang SC", "Microsoft YaHei", sans-serif`
  ctx.fillStyle = props.fontColor
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'

  // 计算水印文字尺寸
  const textMetrics = ctx.measureText(props.text)
  const textWidth = textMetrics.width
  const textHeight = props.fontSize

  // 计算间距
  const [gapX, gapY] = props.gap
  const stepX = textWidth + gapX
  const stepY = textHeight + gapY

  // 计算需要绘制的行列数（考虑旋转后需要更大范围覆盖）
  const diagonal = Math.sqrt(width * width + height * height)
  const cols = Math.ceil(diagonal / stepX) + 2
  const rows = Math.ceil(diagonal / stepY) + 2

  // 保存当前状态
  ctx.save()

  // 移动到中心点并旋转
  ctx.translate(width / 2, height / 2)
  ctx.rotate((props.rotate * Math.PI) / 180)

  // 绘制水印网格
  const startX = -diagonal / 2
  const startY = -diagonal / 2

  for (let i = 0; i < rows; i++) {
    for (let j = 0; j < cols; j++) {
      const x = startX + j * stepX
      const y = startY + i * stepY
      ctx.fillText(props.text, x, y)
    }
  }

  // 恢复状态
  ctx.restore()
}

// 监听属性变化
watch(
  () => [props.text, props.fontSize, props.fontColor, props.rotate, props.gap, props.show],
  () => {
    drawWatermark()
  }
)

onMounted(() => {
  drawWatermark()

  // 监听容器大小变化
  if (typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => {
      drawWatermark()
    })
    if (containerRef.value) {
      resizeObserver.observe(containerRef.value)
    }
  }
})

onUnmounted(() => {
  if (resizeObserver) {
    resizeObserver.disconnect()
  }
})
</script>

<style lang="scss" scoped>
.watermark-container {
  position: relative;
  width: 100%;
  height: 100%;
}

.watermark-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 9999;
}
</style>
