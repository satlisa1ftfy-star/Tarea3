<script setup>
import { computed } from 'vue'

// Paginador del Figma: ‹ 1 2 3 › · "1–15 de 120".
const props = defineProps({
  pagina: { type: Number, required: true },
  total: { type: Number, required: true }, // cantidad de registros
  tamano: { type: Number, required: true }, // registros por página
})
const emit = defineEmits(['cambiar'])

const totalPaginas = computed(() => Math.max(1, Math.ceil(props.total / props.tamano)))
const desde = computed(() => (props.total ? (props.pagina - 1) * props.tamano + 1 : 0))
const hasta = computed(() => Math.min(props.pagina * props.tamano, props.total))

// Primera, última y vecinas de la actual; "…" en los saltos.
const numeros = computed(() => {
  const total = totalPaginas.value
  const actual = props.pagina
  const lista = []
  for (let n = 1; n <= total; n++) {
    if (n === 1 || n === total || Math.abs(n - actual) <= 1) lista.push(n)
    else if (lista[lista.length - 1] !== '…') lista.push('…')
  }
  return lista
})

function ir(n) {
  if (n < 1 || n > totalPaginas.value || n === props.pagina) return
  emit('cambiar', n)
}
</script>

<template>
  <nav class="paginador" aria-label="Paginación">
    <button type="button" :disabled="pagina <= 1" aria-label="Página anterior" @click.stop="ir(pagina - 1)">‹</button>
    <template v-for="(n, i) in numeros" :key="`${n}-${i}`">
      <span v-if="n === '…'" class="paginador-salto">…</span>
      <button v-else type="button" :class="{ activo: n === pagina }" @click.stop="ir(n)">{{ n }}</button>
    </template>
    <button type="button" :disabled="pagina >= totalPaginas" aria-label="Página siguiente" @click.stop="ir(pagina + 1)">›</button>
    <span class="paginador-rango">{{ desde }}–{{ hasta }} de {{ total }}</span>
  </nav>
</template>
