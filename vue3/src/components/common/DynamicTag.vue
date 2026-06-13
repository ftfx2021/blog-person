<template>
  <span
    class="dynamic-tag"
    :class="[`size-${size}`, { clickable }]"
    :style="tagStyle"
    role="button"
    :aria-label="`标签：${label}`"
    @click="handleClick"
  >
    <span class="label">{{ label }}</span>
  </span>
  
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  label: { type: String, required: true },
  bgColor: { type: String, default: '#68D391' }, // 薄荷绿
  textColor: { type: String, default: '' }, // 自定义文字颜色，为空时自动计算
  size: { type: String, default: 'small' }, // small | medium | large
  clickable: { type: Boolean, default: true }
})

const emit = defineEmits(['click'])

function parseHexColor(color) {
  if (!color || typeof color !== 'string') return null
  const hex = color.trim()
    .replace('#', '')
    .replace(/^([a-f\d])([a-f\d])([a-f\d])$/i, (m, r, g, b) => r + r + g + g + b + b)
  if (!/^[a-f\d]{6}$/i.test(hex)) return null
  const r = parseInt(hex.slice(0, 2), 16)
  const g = parseInt(hex.slice(2, 4), 16)
  const b = parseInt(hex.slice(4, 6), 16)
  return { r, g, b }
}

function relativeLuminance({ r, g, b }) {
  // sRGB -> linear
  const toLinear = (v) => {
    v /= 255
    return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4)
  }
  const R = toLinear(r)
  const G = toLinear(g)
  const B = toLinear(b)
  return 0.2126 * R + 0.7152 * G + 0.0722 * B
}

function getContrastText(hexColor) {
  const rgb = parseHexColor(hexColor)
  if (!rgb) {
    // 解析失败时的稳妥对比色：深色文字
    return '#2D3748' // 墨蓝灰
  }
  const L = relativeLuminance(rgb)
  // 与白/深色比较对比度，选择更高者
  const contrastWithWhite = (1.05) / (L + 0.05)
  const contrastWithDark = (L + 0.05) / (0.05)
  return contrastWithWhite >= contrastWithDark ? '#FFFFFF' : '#2D3748'
}

function mix(colorHex, ratio = 0.1, toWhite = true) {
  const rgb = parseHexColor(colorHex)
  if (!rgb) return colorHex
  const t = toWhite ? 255 : 0
  const r = Math.round(rgb.r + (t - rgb.r) * ratio)
  const g = Math.round(rgb.g + (t - rgb.g) * ratio)
  const b = Math.round(rgb.b + (t - rgb.b) * ratio)
  return `#${[r, g, b].map(x => x.toString(16).padStart(2, '0')).join('')}`
}

const textColor = computed(() => {
  // 如果传入了自定义文字颜色，直接使用
  if (props.textColor && props.textColor.trim()) {
    return props.textColor
  }
  // 否则根据背景颜色自动计算
  return getContrastText(props.bgColor)
})

const tagStyle = computed(() => {
  const base = props.bgColor || '#68D391'
  const lighter = mix(base, 0.18, true)
  const borderClr = mix(base, 0.35, true)
  return {
    '--tag-bg': base,
    '--tag-bg-grad': `linear-gradient(180deg, ${lighter}, ${base})`,
    '--tag-text': textColor.value,
    '--tag-border': borderClr
  }
})

function handleClick(e) {
  if (props.clickable) emit('click', e)
}
</script>

<style scoped>

.dynamic-tag {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 3px; /* 直角圆角 2px */
  background: var(--tag-bg-grad);
  color: var(--tag-text);
  border: 1px solid var(--tag-border);
  line-height: 1;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.2px;
  user-select: none;

    background:var(--tag-bg-grad);
  backdrop-filter: blur(10px);

}

.dynamic-tag.clickable {
  cursor: pointer;
}





.dynamic-tag .label {
  white-space: nowrap;
}

.size-small { padding: 6px 10px; font-size: 12px; }
.size-medium { padding: 8px 12px; font-size: 13px; }
.size-large { padding: 10px 14px; font-size: 14px; }
</style>


