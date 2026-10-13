import { buscarCargosAutorizados } from '../services/requerimientoService'

// TipBus que usa /buscarCargo para listar los cargos autorizados de un requerimiento.
const TIP_BUS_CARGOS_AUTORIZADOS = 2

// Cargos ya consultados en esta sesión, por requerimiento/categoría/subcategoría.
// Solo se guardan respuestas exitosas: si falla, el siguiente intento vuelve a consultar.
const cacheCargosAutorizados = new Map()

// Campos que devuelve /buscarCargo (se conservan todos aunque la grilla no los muestre).
function aCargoAutorizado(cargo) {
  return {
    codigoCargo: cargo.codigoCargo,
    nombreCargo: cargo.nombreCargo,
    tipAprob: cargo.tipAprob,
    codigo_EvalCar: cargo.codigo_EvalCar,
  }
}

/**
 * Cargos autorizados del requerimiento. Si ya se consultaron con los mismos
 * parámetros, devuelve lo que está en memoria sin volver a llamar al endpoint.
 */
export async function cargarCargosAutorizados({ codigoReq, codigoCat, codigoSubCat }) {
  const clave = `${codigoReq}|${codigoCat}|${codigoSubCat}`
  if (cacheCargosAutorizados.has(clave)) return cacheCargosAutorizados.get(clave)

  const cargos = await buscarCargosAutorizados({
    tipBus: TIP_BUS_CARGOS_AUTORIZADOS,
    codigoReq,
    codigoCat,
    codigoSubCat,
  })
  const resultado = (cargos || []).map(aCargoAutorizado)
  cacheCargosAutorizados.set(clave, resultado)
  return resultado
}
