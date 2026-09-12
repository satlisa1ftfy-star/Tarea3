/*********************************************************************************************************/
/* <Base de Datos> : GestionRQLD                                                                         */
/* <Descripción>   : Búsqueda de requerimientos mediante filtros opcionales.                             */
/*                                                                                                       */
/* Valores que indican "Todos":                                                                          */
/*    iCodigo_Req               = 0                                                                      */
/*    vTituloReq                = TODO                                                                   */
/*    cFechaInicialReq          = TODO o vacío                                                            */
/*    cFechaFinalReq            = TODO o vacío                                                            */
/*    iCodigoPerSolicitante     = 0                                                                      */
/*    iCodUoSolicitante         = 0                                                                      */
/*    iCodigoPerResponsable     = 0                                                                      */
/*    iCodUoResponsable         = 0                                                                      */
/*    siCodigoEst               = 0                                                                      */
/*    siVigencia                = 3   (1 = Vigente, 2 = No vigente, 3 = Todos)                          */
/*                                                                                                       */
/* Formato de fecha: yyyy-MM-dd                                                                           */
/*********************************************************************************************************/
/* <Corregido por>  : Claude (a pedido de July)                                                          */
/* <Motivo>         : El filtro por responsable tardaba ~12s (5253 filas) contra ~1s del filtro por      */
/*                    solicitante. Causa: el CTE "MovimientoActual" con ROW_NUMBER() OVER (PARTITION BY) */
/*                    calculaba el último movimiento de TODOS los requerimientos (todo GRMovRequerimiento*/
/*                    activo/vigente) antes de poder filtrar por responsable, porque una función de      */
/*                    ventana necesita materializar la partición completa antes de filtrar.              */
/*                    Se reemplazó ese CTE por un OUTER APPLY con TOP 1 correlacionado por iCodigo_Req:   */
/*                    así SQL Server resuelve el "último movimiento" fila por fila de GRMaeRequerimiento, */
/*                    pudiendo usar un índice seek en vez de escanear/ordenar toda la tabla de            */
/*                    movimientos. El resultado (columnas y filas devueltas) es idéntico, solo cambia     */
/*                    cómo se calcula.                                                                   */
/*                    Para que el OUTER APPLY realmente use un seek, crear (si no existe ya un índice     */
/*                    equivalente — revisar antes con sp_helpindex GRMovRequerimiento):                  */
/*                                                                                                       */
/*                    CREATE INDEX IX_GRMovRequerimiento_Req_Vigente                                      */
/*                    ON GRMovRequerimiento (iCodigo_Req, bActivo_ReqMov, bVigencia_ReqMov,                */
/*                                           sdFecha_ReqMov DESC, iCodigo_ReqMov DESC)                    */
/*                    INCLUDE (iCodigo_PerDes_ReqMov, iCodUoDes_ReqMov)                                   */
/*                                                                                                       */
/*                    Se agregó además el parámetro @siVigencia para filtrar por                         */
/*                    r.bVigencia_Req (1=Vigente, 2=No vigente, 3=Todos). Motivo: en el                  */
/*                    tablero ("mis pendientes") no tiene sentido traer años de                          */
/*                    requerimientos ya cerrados/anulados; filtrar bVigencia_Req=1 ANTES                 */
/*                    de resolver el OUTER APPLY y los joins a organizacion reduce el                    */
/*                    tamaño del conjunto de filas que hay que resolver, sin necesitar                   */
/*                    ningún cambio en la base de datos organizacion.                                    */
/*                    PERO: se comprobó (July, 2026-09-12) que para el filtro por responsable esto casi  */
/*                    no ayuda, porque la mayoría de sus requerimientos siguen "vigentes" (4866 de 5253, */
/*                    10s en vez de 11s). El motivo real es otro: el OUTER APPLY de "Responsable         */
/*                    vigente" se evalúa fila por fila para TODO GRMaeRequerimiento activo (~285,000     */
/*                    filas totales en la tabla), y recién DESPUÉS de resolverlo para todas esas filas   */
/*                    se filtra en el WHERE por mov.iCodigo_PerDes_ReqMov = @iCodigoPerResponsable. O    */
/*                    sea: aunque el resultado final sean 5253 filas, el motor tiene que hacer ese       */
/*                    índice seek a GRMovRequerimiento ~285,000 veces (una por cada requerimiento activo)*/
/*                    antes de poder descartar los que no son de este responsable.                      */
/*                                                                                                       */
/*                    SOLUCIÓN (2026-09-12): cuando SÍ se filtra por @iCodigoPerResponsable, se usa un   */
/*                    camino distinto (rama IF más abajo) que arranca por GRMovRequerimiento filtrando   */
/*                    DIRECTAMENTE por iCodigo_PerDes_ReqMov = @iCodigoPerResponsable (usa el índice     */
/*                    IX_GRMovRequerimiento_003, que ya existe: iCodigo_PerDes_ReqMov, bVigencia_ReqMov, */
/*                    bActivo_ReqMov). Eso da de entrada solo los movimientos de ESE responsable —       */
/*                    típicamente un puñado, no 285,000 — y luego, solo para esos, se confirma con un    */
/*                    NOT EXISTS que sea realmente el ÚLTIMO movimiento vigente del requerimiento (que   */
/*                    nadie se lo haya reasignado después). Mismo resultado que el OUTER APPLY original, */
/*                    muchísimo menos trabajo. No requiere cambios en Java/frontend: la firma del SP     */
/*                    (@ptXmlGR) y las columnas que devuelve no cambian.                                 */
/*                    Cuando NO se filtra por responsable (@iCodigoPerResponsable = 0) se sigue usando   */
/*                    el mismo OUTER APPLY de siempre (rama ELSE), sin cambios.                          */
/*********************************************************************************************************/

CREATE PROCEDURE [dbo].[spGR_Requerimiento_BuscarFiltros]
(
    @ptXmlGR Text
)
AS
BEGIN

    SET NOCOUNT ON

    DECLARE
        @idoc                       Int,
        @iCodigoReq                 Int,
        @vTituloReq                 Varchar(500),
        @cFechaInicialReq           Varchar(10),
        @cFechaFinalReq             Varchar(10),
        @sdFechaInicialReq          DateTime,
        @sdFechaFinalReq            DateTime,
        @iCodigoPerSolicitante      Int,
        @iCodUoSolicitante          Int,
        @iCodigoPerResponsable      Int,
        @iCodUoResponsable          Int,
        @siCodigoEst                SmallInt,
        @siVigencia                 SmallInt

    ----------------------------------------------------------------------
    -- Validar XML
    ----------------------------------------------------------------------
    IF @ptXmlGR IS NULL
    BEGIN
        RETURN
    END

    ----------------------------------------------------------------------
    -- Leer XML
    ----------------------------------------------------------------------
    EXEC sp_xml_preparedocument
        @idoc OUTPUT,
        @ptXmlGR

    SELECT
        @iCodigoReq                = iCodigoReq,
        @vTituloReq                = vTituloReq,
        @cFechaInicialReq          = cFechaInicialReq,
        @cFechaFinalReq            = cFechaFinalReq,
        @iCodigoPerSolicitante     = iCodigoPerSolicitante,
        @iCodUoSolicitante         = iCodUoSolicitante,
        @iCodigoPerResponsable     = iCodigoPerResponsable,
        @iCodUoResponsable         = iCodUoResponsable,
        @siCodigoEst               = siCodigoEst,
        @siVigencia                = siVigencia
    FROM OPENXML(
        @idoc,
        '/R/XmlGR',
        1
    )
    WITH
    (
        iCodigoReq                 Int,
        vTituloReq                 Varchar(500),
        cFechaInicialReq           Varchar(10),
        cFechaFinalReq             Varchar(10),
        iCodigoPerSolicitante      Int,
        iCodUoSolicitante          Int,
        iCodigoPerResponsable      Int,
        iCodUoResponsable          Int,
        siCodigoEst     SmallInt,
        siVigencia                 SmallInt
    )

    EXEC sp_xml_removedocument @idoc

    ----------------------------------------------------------------------
    -- Valores por defecto
    ----------------------------------------------------------------------
    SET @iCodigoReq =
        ISNULL(@iCodigoReq, 0)

    SET @vTituloReq =
        ISNULL(
            LTRIM(RTRIM(@vTituloReq)),
            'TODO'
        )

    IF @vTituloReq = ''
        SET @vTituloReq = 'TODO'

    SET @cFechaInicialReq =
        ISNULL(
            LTRIM(RTRIM(@cFechaInicialReq)),
            ''
        )

    SET @cFechaFinalReq =
        ISNULL(
            LTRIM(RTRIM(@cFechaFinalReq)),
            ''
        )

    SET @iCodigoPerSolicitante =
        ISNULL(@iCodigoPerSolicitante, 0)

    SET @iCodUoSolicitante =
        ISNULL(@iCodUoSolicitante, 0)

    SET @iCodigoPerResponsable =
        ISNULL(@iCodigoPerResponsable, 0)

    SET @iCodUoResponsable =
        ISNULL(@iCodUoResponsable, 0)

    SET @siCodigoEst =
        ISNULL(@siCodigoEst, 0)

    -- 1 = Vigente, 2 = No vigente, 3 = Todos (por defecto, si no se envía)
    SET @siVigencia =
        ISNULL(@siVigencia, 3)

    ----------------------------------------------------------------------
    -- Convertir fechas solamente si fueron enviadas
    ----------------------------------------------------------------------
    SET @sdFechaInicialReq = NULL
    SET @sdFechaFinalReq   = NULL

    IF @cFechaInicialReq <> ''
       AND UPPER(@cFechaInicialReq) <> 'TODO'
    BEGIN
        SET @sdFechaInicialReq =
            CONVERT(DateTime, @cFechaInicialReq, 120)
    END

    IF @cFechaFinalReq <> ''
       AND UPPER(@cFechaFinalReq) <> 'TODO'
    BEGIN
        SET @sdFechaFinalReq =
            CONVERT(DateTime, @cFechaFinalReq, 120)
    END

    ----------------------------------------------------------------------
    -- NOTA: aquí antes había un CTE "MovimientoActual" con
    -- ROW_NUMBER() OVER (PARTITION BY iCodigo_Req ORDER BY ...) que
    -- calculaba el último movimiento de TODA la tabla GRMovRequerimiento
    -- antes de poder filtrar por responsable (12s con 5253 filas).
    -- Se quitó de aquí: ahora el "último movimiento vigente" se calcula
    -- por requerimiento con un OUTER APPLY dentro del FROM de la
    -- consulta principal (ver más abajo, sección "Responsable vigente"),
    -- lo que permite usar un índice seek en vez de escanear todo.
    ----------------------------------------------------------------------

    ----------------------------------------------------------------------
    -- CAMINO RÁPIDO: cuando SÍ se filtra por responsable
    --
    -- En vez de recorrer TODO GRMaeRequerimiento con un OUTER APPLY (rama
    -- ELSE) y recién al final quedarnos solo con las filas de este
    -- responsable, arrancamos directo desde GRMovRequerimiento filtrando
    -- por iCodigo_PerDes_ReqMov = @iCodigoPerResponsable (usa el índice
    -- IX_GRMovRequerimiento_003: iCodigo_PerDes_ReqMov, bVigencia_ReqMov,
    -- bActivo_ReqMov). Eso da de entrada solo los movimientos de ESE
    -- responsable. El NOT EXISTS descarta los que ya fueron reasignados
    -- (o sea, nos quedamos solo con el ÚLTIMO movimiento vigente de cada
    -- requerimiento), igual que hacía el OUTER APPLY, pero evaluado solo
    -- sobre ese subconjunto chico en vez de sobre toda la tabla activa.
    ----------------------------------------------------------------------
    IF @iCodigoPerResponsable <> 0
    BEGIN

        ;WITH UltimoMovResponsable AS
        (
            SELECT
                m.iCodigo_Req,
                m.iCodigo_PerDes_ReqMov,
                m.iCodUoDes_ReqMov
            FROM GRMovRequerimiento m (NOLOCK)
            WHERE
                m.iCodigo_PerDes_ReqMov = @iCodigoPerResponsable
                AND m.bActivo_ReqMov = 1
                AND m.bVigencia_ReqMov = 1
                AND NOT EXISTS
                (
                    -- Descarta este movimiento si existe uno posterior
                    -- (más reciente) todavía activo/vigente para el mismo
                    -- requerimiento: ese sería el responsable actual, no
                    -- este. Así solo quedan los movimientos que SÍ son
                    -- "el último".
                    SELECT 1
                    FROM GRMovRequerimiento posterior (NOLOCK)
                    WHERE
                        posterior.iCodigo_Req = m.iCodigo_Req
                        AND posterior.bActivo_ReqMov = 1
                        AND posterior.bVigencia_ReqMov = 1
                        AND
                        (
                            posterior.sdFecha_ReqMov > m.sdFecha_ReqMov
                            OR
                            (
                                posterior.sdFecha_ReqMov = m.sdFecha_ReqMov
                                AND posterior.iCodigo_ReqMov > m.iCodigo_ReqMov
                            )
                        )
                )
        )

        SELECT

            ------------------------------------------------------------------
            -- REQUERIMIENTO
            ------------------------------------------------------------------
            r.iCodigo_Req
                AS codigoRequerimiento,

            ISNULL(
                LTRIM(RTRIM(r.vSumilla_Req)),
                ''
            ) AS titulo,

            r.sdFecha_Req
                AS fechaRegistro,

            ISNULL(
                r.vDescripcion_Req,
                ''
            ) AS descripcion,

            ------------------------------------------------------------------
            -- SOLICITANTE
            ------------------------------------------------------------------
            r.iCodigo_Per
                AS codigoPersonaSolicitante,

            LTRIM(RTRIM(
                ISNULL(perSol.vApePat, '') + ' ' +
                ISNULL(perSol.vApeMat, '') + ' ' +
                ISNULL(perSol.vNombre, '')
            )) AS solicitante,

            sol.iCodUo
                AS codigoUoSolicitante,

            ISNULL(
                uoSol.vDesLUo,
                ''
            ) AS unidadOrganicaSolicitante,

            sol.siCodCar
                AS codigoCargoSolicitante,

            ISNULL(
                carSol.vDesCar,
                ''
            ) AS cargoSolicitante,

            ------------------------------------------------------------------
            -- RESPONSABLE ACTUAL
            ------------------------------------------------------------------
            mov.iCodigo_PerDes_ReqMov
                AS codigoPersonaResponsable,

            LTRIM(RTRIM(
                ISNULL(perResp.vApePat, '') + ' ' +
                ISNULL(perResp.vApeMat, '') + ' ' +
                ISNULL(perResp.vNombre, '')
            )) AS responsable,

            mov.iCodUoDes_ReqMov
                AS codigoUoResponsable,

            ISNULL(
                uoResp.vDesLUo,
                ''
            ) AS unidadOrganicaResponsable,

            resp.siCodCar
                AS codigoCargoResponsable,

            ISNULL(
                carResp.vDesCar,
                ''
            ) AS cargoResponsable,

            ------------------------------------------------------------------
            -- ESTADO
            ------------------------------------------------------------------
            r.siCodigo_Est
                AS codigoEstado,

            ISNULL(
                est.vNombre_Est,
                ''
            ) AS estado,

            ------------------------------------------------------------------
            -- CATEGORÍA
            ------------------------------------------------------------------
            cs.siCodigo_Cat
                AS codigoCategoria,

            ISNULL(
                cat.vNombre_Cat,
                ''
            ) AS categoria,

            ------------------------------------------------------------------
            -- SUBCATEGORÍA
            ------------------------------------------------------------------
            cs.siCodigo_SubCat
                AS codigoSubcategoria,

            ISNULL(
                sub.vNombre_SubCat,
                ''
            ) AS subcategoria,

            ------------------------------------------------------------------
            -- PRIORIDAD
            ------------------------------------------------------------------
            r.iCodigo_Pri
                AS codigoPrioridad,

            ISNULL(
                pri.cDescripcion_Atr,
                ''
            ) AS prioridad,

            ------------------------------------------------------------------
            -- UNIDAD A LA QUE PERTENECE/ATIENDE EL REQUERIMIENTO
            ------------------------------------------------------------------
            uoReq.iCodUo
                AS codigoUoRequerimiento,

            ISNULL(
                orgReq.vDesLUo,
                ''
            ) AS unidadOrganicaRequerimiento,

            ------------------------------------------------------------------
            -- CÓDIGO INTERNO CATEGORÍA-SUBCATEGORÍA
            ------------------------------------------------------------------
            r.iCodigo_CSC
                AS codigoCategoriaSubcategoria

        FROM GRMaeRequerimiento r (NOLOCK)

        ----------------------------------------------------------------------
        -- Responsable vigente: en vez de OUTER APPLY, INNER JOIN directo al
        -- CTE ya filtrado por responsable (arriba). Esto también actúa como
        -- filtro (solo trae los requerimientos de ESE responsable), por eso
        -- ya no hace falta repetir la condición de responsable en el WHERE.
        ----------------------------------------------------------------------
        INNER JOIN UltimoMovResponsable mov
            ON mov.iCodigo_Req = r.iCodigo_Req

        ----------------------------------------------------------------------
        -- Solicitante
        ----------------------------------------------------------------------
        INNER JOIN GRMaePersona sol (NOLOCK)
            ON r.iCodigo_Per = sol.iCodigo_Per

        LEFT JOIN organizacion..EOMAEPER perSol (NOLOCK)
            ON sol.cCodPer = perSol.cCodPer

        LEFT JOIN organizacion..EOMaeUo uoSol (NOLOCK)
            ON sol.iCodUo = uoSol.nCodUo

        LEFT JOIN organizacion..EOMAECAR carSol (NOLOCK)
            ON sol.siCodCar = carSol.nCodCar

        LEFT JOIN GRMaePersona resp (NOLOCK)
            ON mov.iCodigo_PerDes_ReqMov = resp.iCodigo_Per

        LEFT JOIN organizacion..EOMAEPER perResp (NOLOCK)
            ON resp.cCodPer = perResp.cCodPer

        LEFT JOIN organizacion..EOMaeUo uoResp (NOLOCK)
            ON mov.iCodUoDes_ReqMov = uoResp.nCodUo

        LEFT JOIN organizacion..EOMAECAR carResp (NOLOCK)
            ON resp.siCodCar = carResp.nCodCar

        ----------------------------------------------------------------------
        -- Estado
        ----------------------------------------------------------------------
        LEFT JOIN GRTabEstado est (NOLOCK)
            ON r.siCodigo_Est = est.siCodigo_Est

        ----------------------------------------------------------------------
        -- Categoría / Subcategoría
        ----------------------------------------------------------------------
        LEFT JOIN GRMaeCategoriaSubCategoria cs (NOLOCK)
            ON r.iCodigo_CSC = cs.iCodigo_CSC

        LEFT JOIN GRTabCategoria cat (NOLOCK)
            ON cs.siCodigo_Cat = cat.siCodigo_Cat

        LEFT JOIN GRTabSubCategoria sub (NOLOCK)
            ON cs.siCodigo_SubCat = sub.siCodigo_SubCat

        ----------------------------------------------------------------------
        -- Prioridad
        ----------------------------------------------------------------------
        LEFT JOIN GRMaeAtributos pri (NOLOCK)
            ON r.iCodigo_Pri = pri.iCodigo_Atr
            AND pri.cMascara_Atr = 'iCodigo_Pri'

        ----------------------------------------------------------------------
        -- Unidad del requerimiento
        ----------------------------------------------------------------------
        LEFT JOIN GRMaeUnidadOrganica uoReq (NOLOCK)
            ON r.iCodigo_Uo = uoReq.iCodigo_Uo

        LEFT JOIN organizacion..EOMaeUo orgReq (NOLOCK)
            ON uoReq.iCodUo = orgReq.nCodUo

        ----------------------------------------------------------------------
        -- FILTROS (@iCodigoPerResponsable ya queda garantizado por el
        -- INNER JOIN a UltimoMovResponsable, no se repite aquí)
        ----------------------------------------------------------------------
        WHERE
            r.bActivo_Req = 1

            AND
            (
                @iCodigoReq = 0
                OR r.iCodigo_Req = @iCodigoReq
            )

            AND
            (
                UPPER(@vTituloReq) = 'TODO'

                OR

                ISNULL(r.vSumilla_Req, '')
                    COLLATE SQL_Latin1_General_CP1_CI_AI

                LIKE

                ('%' + @vTituloReq + '%')
                    COLLATE SQL_Latin1_General_CP1_CI_AI
            )

            AND
            (
                @iCodigoPerSolicitante = 0
                OR r.iCodigo_Per = @iCodigoPerSolicitante
            )

            AND
            (
                @iCodUoSolicitante = 0
                OR sol.iCodUo = @iCodUoSolicitante
            )

            AND
            (
                @iCodUoResponsable = 0
                OR mov.iCodUoDes_ReqMov = @iCodUoResponsable
            )

            AND
            (
                @siCodigoEst = 0
                OR r.siCodigo_Est = @siCodigoEst
            )

            AND
            (
                @siVigencia = 3
                OR (
                    @siVigencia = 1
                    AND r.bVigencia_Req = 1
                )
                OR (
                    @siVigencia = 2
                    AND r.bVigencia_Req = 0
                )
            )

            AND
            (
                @sdFechaInicialReq IS NULL
                OR r.sdFecha_Req >= @sdFechaInicialReq
            )

            AND
            (
                @sdFechaFinalReq IS NULL
                OR r.sdFecha_Req < DATEADD(
                    DAY,
                    1,
                    @sdFechaFinalReq
                )
            )

        ORDER BY
            r.iCodigo_Req DESC

    END
    ELSE
    BEGIN

        ----------------------------------------------------------------------
        -- CONSULTA (sin filtro de responsable: se mantiene el OUTER APPLY
        -- original, sin cambios)
        ----------------------------------------------------------------------
        SELECT

            ------------------------------------------------------------------
            -- REQUERIMIENTO
            ------------------------------------------------------------------
            r.iCodigo_Req
                AS codigoRequerimiento,

            ISNULL(
                LTRIM(RTRIM(r.vSumilla_Req)),
                ''
            ) AS titulo,

            r.sdFecha_Req
                AS fechaRegistro,

            ISNULL(
                r.vDescripcion_Req,
                ''
            ) AS descripcion,

            ------------------------------------------------------------------
            -- SOLICITANTE
            ------------------------------------------------------------------
            r.iCodigo_Per
                AS codigoPersonaSolicitante,

            LTRIM(RTRIM(
                ISNULL(perSol.vApePat, '') + ' ' +
                ISNULL(perSol.vApeMat, '') + ' ' +
                ISNULL(perSol.vNombre, '')
            )) AS solicitante,

            sol.iCodUo
                AS codigoUoSolicitante,

            ISNULL(
                uoSol.vDesLUo,
                ''
            ) AS unidadOrganicaSolicitante,

            sol.siCodCar
                AS codigoCargoSolicitante,

            ISNULL(
                carSol.vDesCar,
                ''
            ) AS cargoSolicitante,

            ------------------------------------------------------------------
            -- RESPONSABLE ACTUAL
            ------------------------------------------------------------------
            mov.iCodigo_PerDes_ReqMov
                AS codigoPersonaResponsable,

            LTRIM(RTRIM(
                ISNULL(perResp.vApePat, '') + ' ' +
                ISNULL(perResp.vApeMat, '') + ' ' +
                ISNULL(perResp.vNombre, '')
            )) AS responsable,

            mov.iCodUoDes_ReqMov
                AS codigoUoResponsable,

            ISNULL(
                uoResp.vDesLUo,
                ''
            ) AS unidadOrganicaResponsable,

            resp.siCodCar
                AS codigoCargoResponsable,

            ISNULL(
                carResp.vDesCar,
                ''
            ) AS cargoResponsable,

            ------------------------------------------------------------------
            -- ESTADO
            ------------------------------------------------------------------
            r.siCodigo_Est
                AS codigoEstado,

            ISNULL(
                est.vNombre_Est,
                ''
            ) AS estado,

            ------------------------------------------------------------------
            -- CATEGORÍA
            ------------------------------------------------------------------
            cs.siCodigo_Cat
                AS codigoCategoria,

            ISNULL(
                cat.vNombre_Cat,
                ''
            ) AS categoria,

            ------------------------------------------------------------------
            -- SUBCATEGORÍA
            ------------------------------------------------------------------
            cs.siCodigo_SubCat
                AS codigoSubcategoria,

            ISNULL(
                sub.vNombre_SubCat,
                ''
            ) AS subcategoria,

            ------------------------------------------------------------------
            -- PRIORIDAD
            ------------------------------------------------------------------
            r.iCodigo_Pri
                AS codigoPrioridad,

            ISNULL(
                pri.cDescripcion_Atr,
                ''
            ) AS prioridad,

            ------------------------------------------------------------------
            -- UNIDAD A LA QUE PERTENECE/ATIENDE EL REQUERIMIENTO
            ------------------------------------------------------------------
            uoReq.iCodUo
                AS codigoUoRequerimiento,

            ISNULL(
                orgReq.vDesLUo,
                ''
            ) AS unidadOrganicaRequerimiento,

            ------------------------------------------------------------------
            -- CÓDIGO INTERNO CATEGORÍA-SUBCATEGORÍA
            ------------------------------------------------------------------
            r.iCodigo_CSC
                AS codigoCategoriaSubcategoria

        FROM GRMaeRequerimiento r (NOLOCK)

        ----------------------------------------------------------------------
        -- Solicitante
        ----------------------------------------------------------------------
        INNER JOIN GRMaePersona sol (NOLOCK)
            ON r.iCodigo_Per = sol.iCodigo_Per

        LEFT JOIN organizacion..EOMAEPER perSol (NOLOCK)
            ON sol.cCodPer = perSol.cCodPer

        LEFT JOIN organizacion..EOMaeUo uoSol (NOLOCK)
            ON sol.iCodUo = uoSol.nCodUo

        LEFT JOIN organizacion..EOMAECAR carSol (NOLOCK)
            ON sol.siCodCar = carSol.nCodCar

        ----------------------------------------------------------------------
        -- Responsable vigente
        --
        -- CORREGIDO: antes era "LEFT JOIN MovimientoActual mov ON r.iCodigo_Req
        -- = mov.iCodigo_Req AND mov.fila = 1" contra un CTE con ROW_NUMBER()
        -- calculado sobre toda la tabla. Ahora es un OUTER APPLY con TOP 1
        -- correlacionado por r.iCodigo_Req: se resuelve fila por fila de
        -- GRMaeRequerimiento (permite índice seek) en vez de materializar el
        -- último movimiento de TODOS los requerimientos por adelantado.
        -- Mismo resultado, mismo criterio de "vigente" (bActivo_ReqMov = 1 Y
        -- bVigencia_ReqMov = 1, más reciente por fecha y luego por código).
        --
        -- NOTA: esta rama (ELSE) solo corre cuando NO se filtra por
        -- responsable, así que "fila por fila de GRMaeRequerimiento" aquí
        -- significa todas las filas activas (no hay forma de acotar antes,
        -- porque no sabemos qué responsable buscar). Cuando SÍ se filtra
        -- por responsable, se usa la rama IF de más arriba, que arranca
        -- desde GRMovRequerimiento y es mucho más rápida para ese caso.
        ----------------------------------------------------------------------
        OUTER APPLY
        (
            SELECT TOP 1
                m.iCodigo_PerDes_ReqMov,
                m.iCodUoDes_ReqMov
            FROM GRMovRequerimiento m (NOLOCK)
            WHERE
                m.iCodigo_Req = r.iCodigo_Req
                AND m.bActivo_ReqMov = 1
                AND m.bVigencia_ReqMov = 1
            ORDER BY
                m.sdFecha_ReqMov DESC,
                m.iCodigo_ReqMov DESC
        ) mov

        LEFT JOIN GRMaePersona resp (NOLOCK)
            ON mov.iCodigo_PerDes_ReqMov = resp.iCodigo_Per

        LEFT JOIN organizacion..EOMAEPER perResp (NOLOCK)
            ON resp.cCodPer = perResp.cCodPer

        LEFT JOIN organizacion..EOMaeUo uoResp (NOLOCK)
            ON mov.iCodUoDes_ReqMov = uoResp.nCodUo

        LEFT JOIN organizacion..EOMAECAR carResp (NOLOCK)
            ON resp.siCodCar = carResp.nCodCar

        ----------------------------------------------------------------------
        -- Estado
        ----------------------------------------------------------------------
        LEFT JOIN GRTabEstado est (NOLOCK)
            ON r.siCodigo_Est = est.siCodigo_Est

        ----------------------------------------------------------------------
        -- Categoría / Subcategoría
        ----------------------------------------------------------------------
        LEFT JOIN GRMaeCategoriaSubCategoria cs (NOLOCK)
            ON r.iCodigo_CSC = cs.iCodigo_CSC

        LEFT JOIN GRTabCategoria cat (NOLOCK)
            ON cs.siCodigo_Cat = cat.siCodigo_Cat

        LEFT JOIN GRTabSubCategoria sub (NOLOCK)
            ON cs.siCodigo_SubCat = sub.siCodigo_SubCat

        ----------------------------------------------------------------------
        -- Prioridad
        ----------------------------------------------------------------------
        LEFT JOIN GRMaeAtributos pri (NOLOCK)
            ON r.iCodigo_Pri = pri.iCodigo_Atr
            AND pri.cMascara_Atr = 'iCodigo_Pri'

        ----------------------------------------------------------------------
        -- Unidad del requerimiento
        ----------------------------------------------------------------------
        LEFT JOIN GRMaeUnidadOrganica uoReq (NOLOCK)
            ON r.iCodigo_Uo = uoReq.iCodigo_Uo

        LEFT JOIN organizacion..EOMaeUo orgReq (NOLOCK)
            ON uoReq.iCodUo = orgReq.nCodUo

        ----------------------------------------------------------------------
        -- FILTROS
        ----------------------------------------------------------------------
        WHERE
            r.bActivo_Req = 1

            --------------------------------------------------------------
            -- Número de requerimiento
            -- 0 = todos
            --------------------------------------------------------------
            AND
            (
                @iCodigoReq = 0
                OR r.iCodigo_Req = @iCodigoReq
            )

            --------------------------------------------------------------
            -- Título
            -- TODO = todos
            --------------------------------------------------------------
            AND
            (
                UPPER(@vTituloReq) = 'TODO'

                OR

                ISNULL(r.vSumilla_Req, '')
                    COLLATE SQL_Latin1_General_CP1_CI_AI

                LIKE

                ('%' + @vTituloReq + '%')
                    COLLATE SQL_Latin1_General_CP1_CI_AI
            )

            --------------------------------------------------------------
            -- Persona solicitante
            -- 0 = todas
            --------------------------------------------------------------
            AND
            (
                @iCodigoPerSolicitante = 0
                OR r.iCodigo_Per = @iCodigoPerSolicitante
            )

            --------------------------------------------------------------
            -- Unidad orgánica solicitante
            -- 0 = todas
            --------------------------------------------------------------
            AND
            (
                @iCodUoSolicitante = 0
                OR sol.iCodUo = @iCodUoSolicitante
            )

            --------------------------------------------------------------
            -- Persona responsable actual
            -- 0 = todas (en esta rama siempre es 0, ver la rama IF de
            -- arriba para @iCodigoPerResponsable <> 0)
            --------------------------------------------------------------
            AND
            (
                @iCodigoPerResponsable = 0
                OR mov.iCodigo_PerDes_ReqMov = @iCodigoPerResponsable
            )

            --------------------------------------------------------------
            -- Unidad orgánica responsable
            -- 0 = todas
            --------------------------------------------------------------
            AND
            (
                @iCodUoResponsable = 0
                OR mov.iCodUoDes_ReqMov = @iCodUoResponsable
            )

            --------------------------------------------------------------
            -- Estado
            -- 0 = todos
            --------------------------------------------------------------
            AND
            (
                @siCodigoEst = 0
                OR r.siCodigo_Est = @siCodigoEst
            )

            --------------------------------------------------------------
            -- Vigencia (r.bVigencia_Req)
            -- 1 = solo vigentes, 2 = solo no vigentes, 3 = todos
            -- Agregado para el tablero: evita traer requerimientos ya
            -- cerrados/anulados de años atrás y así reduce el conjunto de
            -- filas ANTES del OUTER APPLY y los joins a organizacion, sin
            -- tocar la base de datos organizacion.
            --------------------------------------------------------------
            AND
            (
                @siVigencia = 3
                OR (
                    @siVigencia = 1
                    AND r.bVigencia_Req = 1
                )
                OR (
                    @siVigencia = 2
                    AND r.bVigencia_Req = 0
                )
            )

       --------------------------------------------------------------
            -- Fecha inicial
            --------------------------------------------------------------
            AND
            (
                @sdFechaInicialReq IS NULL
                OR r.sdFecha_Req >= @sdFechaInicialReq
            )

            --------------------------------------------------------------
            -- Fecha final
            -- menor al día siguiente para incluir TODO el día final
            --------------------------------------------------------------
            AND
            (
                @sdFechaFinalReq IS NULL
                OR r.sdFecha_Req < DATEADD(
                    DAY,
                    1,
                    @sdFechaFinalReq
                )
            )

        ORDER BY
            r.iCodigo_Req DESC

    END

    SET NOCOUNT OFF

END
