<script setup>
import { useAuthController } from './controllers/useAuthController'
import DashboardView from './views/DashboardView.vue'
import LoginView from './views/LoginView.vue'

const auth = useAuthController()
</script>

<template>
  <LoginView v-if="!auth.isAuthenticated.value" :auth="auth" />
  <DashboardView v-else :perfil="auth.perfil.value" @logout="auth.logout" />
</template>
