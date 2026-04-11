<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const description = computed(() => route.meta.description ?? route.fullPath)
</script>

<template>
  <section>
    <h1>{{ route.name }}</h1>
    <p>{{ description }}</p>
  </section>
</template>

