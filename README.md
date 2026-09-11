# Frontend - Gestion de Requerimientos

Interfaz web para la Gestion de Requerimientos del SAT, desarrollada con Vue 3 y Vite.

## Requisitos

- Node.js 18 o una version posterior.
- npm 9 o una version posterior.

## Instalacion

```bash
npm install
```

## Ejecutar en desarrollo

```bash
npm run dev
```

Vite mostrara en la terminal la URL local, normalmente `http://localhost:5173`.

Para exponer el servicio en la red local:

```bash
npm run dev -- --host 0.0.0.0
```

## Compilar para produccion

```bash
npm run build
```

Los archivos compilados se generan en el directorio `dist/`.

## Vista previa de produccion

```bash
npm run preview
```

## Estructura

```text
src/
  controllers/  Logica de acciones y autenticacion
  models/       Datos del tablero
  views/        Vistas de login y tablero
  assets/       Estilos globales
```
