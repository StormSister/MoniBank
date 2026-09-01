<template>
  <div ref="container" :class="className" />
</template>

<script setup lang="ts">
import mermaid from 'mermaid'
import { onMounted, onUnmounted, ref } from 'vue'

const props = withDefaults(defineProps<{
  graph: string
  id: string
  class?: string
}>(), {
  class: 'mermaid'
})

const container = ref<HTMLElement | null>(null)
const className = props.class

let observer: MutationObserver | undefined
let renderNumber = 0

async function renderDiagram() {
  if (!container.value) return

  const dark = document.documentElement.classList.contains('dark')

  mermaid.initialize({
    startOnLoad: false,
    securityLevel: 'loose',
    theme: 'base',

    flowchart: {
      htmlLabels: false,
      useMaxWidth: true,
      curve: 'basis',
      nodeSpacing: 35,
      rankSpacing: 55
    },

    themeVariables: {
      fontFamily: 'Inter, system-ui, sans-serif',
      fontSize: '16px',

      background: dark ? '#091923' : '#f5f8fa',
      mainBkg: dark ? '#102431' : '#ffffff',

      primaryTextColor: dark ? '#f5f7fa' : '#10202c',
      secondaryTextColor: dark ? '#f5f7fa' : '#10202c',
      tertiaryTextColor: dark ? '#f5f7fa' : '#10202c',

      lineColor: dark ? '#8da1af' : '#526673',
      edgeLabelBackground: dark ? '#07131d' : '#ffffff',

      clusterBkg: dark ? '#0b1b27' : '#edf2f5',
      clusterBorder: '#d7a23b'
    }
  })

  const renderId = `${props.id}-${renderNumber++}`

  const { svg } = await mermaid.render(
    renderId,
    decodeURIComponent(props.graph)
  )

  if (container.value) {
    container.value.innerHTML = svg
  }
}

onMounted(async () => {
  await renderDiagram()

  observer = new MutationObserver(async mutations => {
    if (mutations.some(mutation => mutation.attributeName === 'class')) {
      await renderDiagram()
    }
  })

  observer.observe(document.documentElement, {
    attributes: true,
    attributeFilter: ['class']
  })
})

onUnmounted(() => observer?.disconnect())
</script>