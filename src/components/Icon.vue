<script setup>
defineProps({
  name: { type: String, required: true },
  size: { type: [Number, String], default: 18 },
})
</script>

<!-- Set de íconos de línea (estilo Feather), reutilizado en todo el dashboard
     en vez de glifos de texto/emoji, para acercar el look al Figma. -->
<template>
  <svg
    :width="size" :height="size" viewBox="0 0 24 24" fill="none"
    stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
    aria-hidden="true" class="icon"
  >
    <template v-if="name === 'bell'"><path d="M6 8a6 6 0 0 1 12 0c0 5.5 2 7.5 2 7.5H4S6 13.5 6 8Z" /><path d="M10.3 20a1.94 1.94 0 0 0 3.4 0" /></template>
    <template v-else-if="name === 'chevron-down'"><path d="m6 9 6 6 6-6" /></template>
    <template v-else-if="name === 'chevron-up'"><path d="m18 15-6-6-6 6" /></template>
    <template v-else-if="name === 'calendar'"><rect x="3" y="5" width="18" height="16" rx="2" /><path d="M16 3v4M8 3v4M3 10h18" /></template>
    <template v-else-if="name === 'search'"><circle cx="11" cy="11" r="7" /><path d="m21 21-4.3-4.3" /></template>
    <template v-else-if="name === 'star'"><path d="M12 3.5l2.6 5.4 5.9.9-4.3 4.2 1 5.9L12 17l-5.2 2.9 1-5.9-4.3-4.2 5.9-.9Z" /></template>
    <template v-else-if="name === 'filter'"><path d="M4 5h16M7 12h10M10 19h4" /></template>
    <template v-else-if="name === 'grid'"><rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" /><rect x="3" y="14" width="7" height="7" rx="1" /><rect x="14" y="14" width="7" height="7" rx="1" /></template>
    <template v-else-if="name === 'list'"><path d="M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01" /></template>
    <template v-else-if="name === 'clock'"><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 3" /></template>
    <template v-else-if="name === 'folder'"><path d="M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2Z" /></template>
    <template v-else-if="name === 'layout-grid'"><rect x="3" y="3" width="8" height="8" rx="1" /><rect x="13" y="3" width="8" height="8" rx="1" /><rect x="3" y="13" width="8" height="8" rx="1" /><rect x="13" y="13" width="8" height="8" rx="1" /></template>
    <template v-else-if="name === 'bar-chart'"><path d="M4 20V10M12 20V4M20 20v-7" /></template>
    <template v-else-if="name === 'wrench'"><path d="M14.7 6.3a4 4 0 0 0-5.6 5.1L3 17.5 5.5 20l6.1-6.1a4 4 0 0 0 5.1-5.6l-3 3-2-2Z" /></template>
    <template v-else-if="name === 'help-circle'"><circle cx="12" cy="12" r="9" /><path d="M9.5 9a2.5 2.5 0 1 1 3.8 2.1c-.9.6-1.3 1.1-1.3 2.1" /><path d="M12 17h.01" /></template>
    <template v-else-if="name === 'file-text'"><path d="M7 3h7l5 5v13a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1Z" /><path d="M14 3v5h5M9 13h6M9 17h6" /></template>
    <template v-else-if="name === 'arrow-left'"><path d="M19 12H5M11 6l-6 6 6 6" /></template>
    <template v-else-if="name === 'check'"><path d="M20 6 9 17l-5-5" /></template>
    <template v-else-if="name === 'x'"><path d="M18 6 6 18M6 6l12 12" /></template>
    <template v-else-if="name === 'user-plus'"><circle cx="9" cy="8" r="4" /><path d="M2 21c0-4.4 3.1-7 7-7s7 2.6 7 7M17 8h5M19.5 5.5v5" /></template>
    <template v-else-if="name === 'shield'"><path d="M12 3l7 3v6c0 5-3.2 7.8-7 9-3.8-1.2-7-4-7-9V6Z" /></template>
    <template v-else-if="name === 'history'"><path d="M3 12a9 9 0 1 0 3-6.7" /><path d="M3 4v5h5M12 8v5l3 2" /></template>
    <template v-else-if="name === 'move'"><path d="M9 6h10M9 12h10M9 18h10M3 6h.01M3 12h.01M3 18h.01" /></template>
    <template v-else-if="name === 'plus'"><path d="M12 5v14M5 12h14" /></template>
    <template v-else-if="name === 'paperclip'"><path d="M21 12.5 12.5 21a5 5 0 0 1-7-7L14 5.5a3.5 3.5 0 0 1 5 5L10.5 19a2 2 0 0 1-3-3L15 8.5" /></template>
    <template v-else-if="name === 'printer'"><path d="M6 9V3h12v6M6 18H4a1 1 0 0 1-1-1v-5a1 1 0 0 1 1-1h16a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1h-2" /><rect x="6" y="14" width="12" height="7" /></template>
    <template v-else-if="name === 'corner-down-left'"><path d="M9 10 4 15l5 5" /><path d="M20 4v7a4 4 0 0 1-4 4H4" /></template>
    <template v-else-if="name === 'building'"><rect x="4" y="3" width="16" height="18" rx="1" /><path d="M9 8h1M14 8h1M9 12h1M14 12h1M9 16h1M14 16h1" /></template>
    <template v-else-if="name === 'log-out'"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" /><path d="M16 17l5-5-5-5M21 12H9" /></template>
    <template v-else-if="name === 'sidebar'"><rect x="3" y="4" width="18" height="16" rx="2" /><path d="M9 4v16" /></template>
    <template v-else-if="name === 'refresh'"><path d="M21 12a9 9 0 1 1-2.6-6.4M21 4v5h-5" /></template>
    <template v-else-if="name === 'inbox'"><path d="M4 12h4l2 3h4l2-3h4" /><path d="M5.4 5h13.2L21 12v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-6Z" /></template>
    <template v-else-if="name === 'zap'"><path d="M13 2 4 14h6l-1 8 9-12h-6l1-8Z" /></template>
    <template v-else-if="name === 'upload'"><path d="M12 16V4M7 9l5-5 5 5" /><path d="M4 16v3a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-3" /></template>
    <template v-else-if="name === 'chevrons-left'"><path d="m11 17-5-5 5-5M18 17l-5-5 5-5" /></template>
    <template v-else-if="name === 'chevrons-right'"><path d="m13 17 5-5-5-5M6 17l5-5-5-5" /></template>
  </svg>
</template>

<style scoped>
.icon { display: block; flex-shrink: 0; }
</style>
