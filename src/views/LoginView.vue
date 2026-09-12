<script setup>
defineProps({ auth: { type: Object, required: true } })
</script>

<template>
  <main class="reference-login">
    <header class="reference-header">
      <div class="reference-sat"><strong>SAT</strong><span>Servicio de<br>Administracion<br>Tributaria de Lima</span></div>
      <span class="reference-divider"></span>
      <div class="reference-system"><span class="reference-document">☷</span>Gestion de Requerimientos</div>
    </header>
    <section class="reference-content" aria-labelledby="login-title">
      <div class="reference-card">
        <div class="reference-access-icon" aria-hidden="true">
          <svg viewBox="0 0 130 130" fill="none"><circle cx="65" cy="65" r="58" fill="#e8f0ff"/><circle cx="65" cy="46" r="17" stroke="#0b3786" stroke-width="3"/><path d="M32 97c1-19 15-31 33-31s32 12 33 31" stroke="#0b3786" stroke-width="3" stroke-linecap="round"/><circle cx="94" cy="93" r="22" fill="#0753ce"/><rect x="86" y="92" width="16" height="13" rx="2" fill="white"/><path d="M89 92v-4a5 5 0 0 1 10 0v4" stroke="white" stroke-width="3" stroke-linecap="round"/></svg>
        </div>
        <h1 id="login-title">Iniciar sesión</h1>
        <p>Ingresa tus credenciales para acceder al sistema</p>
        <form novalidate @submit.prevent="auth.login">
          <div class="reference-field"><label class="sr-only" for="username">Usuario</label><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true"><circle cx="12" cy="8" r="4"/><path d="M4.5 20c.8-4 3.2-6 7.5-6s6.7 2 7.5 6" stroke-linecap="round"/></svg><input id="username" v-model.trim="auth.username.value" placeholder="Usuario" autocomplete="username" :aria-invalid="auth.submitted.value && !auth.username.value"><span v-if="auth.submitted.value && !auth.username.value" class="reference-error">Ingresa tu usuario.</span></div>
          <div class="reference-field"><label class="sr-only" for="password">Contraseña</label><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true"><rect x="5" y="10" width="14" height="10" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3M12 14v2" stroke-linecap="round"/></svg><input id="password" v-model="auth.password.value" :type="auth.showPassword.value ? 'text' : 'password'" placeholder="Contraseña" autocomplete="current-password" :aria-invalid="auth.submitted.value && !auth.password.value"><button type="button" aria-label="Mostrar contraseña" @click="auth.showPassword.value = !auth.showPassword.value"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 12s3.5-7 9-7 9 7 9 7-3.5 7-9 7-9-7-9-7Z"/><circle cx="12" cy="12" r="3"/></svg></button><span v-if="auth.submitted.value && !auth.password.value" class="reference-error">Ingresa tu contraseña.</span></div>
          <p v-if="auth.errorMessage.value" class="reference-error reference-error--login" role="alert">{{ auth.errorMessage.value }}</p><button class="reference-submit" type="submit" :disabled="auth.loading.value">{{ auth.loading.value ? 'Ingresando…' : 'Iniciar sesión' }}</button>
        </form>
      </div>
    </section>
    <footer class="reference-footer">© 2026 Servicio de Administración Tributaria de Lima - <strong>SAT</strong></footer>
  </main>
</template>
