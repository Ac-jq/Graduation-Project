<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { fetchCurrentUser, getBootstrappedUser } from '@/core/auth-service'
import { getToken, resolveRoleHome } from '@/core/session'

const router = useRouter()

async function redirectToDefaultHome(): Promise<void> {
  const token = getToken()
  if (!token) {
    await router.replace('/login')
    return
  }

  const currentUser = getBootstrappedUser() ?? (await fetchCurrentUser())
  if (!currentUser) {
    await router.replace('/login')
    return
  }

  await router.replace(resolveRoleHome(currentUser.roleCode))
}

onMounted(() => {
  void redirectToDefaultHome()
})
</script>

<template>
  <section>
    <h1>Redirecting</h1>
  </section>
</template>

