

USE [GestionRQLD]
GO
/****** Object:  StoredProcedure [dbo].[SP_RequerimientosGI_Consulta]    Script Date: 12/10/2026 10:56:23 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--**********************************************************************************************************************
-- Procedimiento para consulta de requerimiento(s)
-- Input             :  @ptXmlGR = <R><XmlGR cCodPer="0295" iDias="90" iTop="30" /></R>                     */
/*                     cCodPer = cCodPer del usuario (codigoPersonaOrganizacion del perfil)              */
/*                     iDias   = ventana hacia atrás (por defecto 90)                                    */
/*                     iTop    = máximo de requerimientos (por defecto 30)
-- Output            :  codigoRequerimiento, fechaClasificacion (más reciente primero) 
-- Creado por        : Carol Estalla 
-- RQ                : 
-- Motivo            : Regla de «clasificó»: spGR_Requerimiento_Registrar (siTipBus = 2) llama a spGR_RequerimientoH_Registrar*/
/* ANTES del UPDATE, con tipo de operación 3, y guarda la foto previa. Una operación del usuario cuenta   */
/* como clasificación cuando la foto estaba en estado 1 (Registrado) y el estado posterior (la foto       */
/* siguiente, o el requerimiento actual si no hay otra) sigue siendo 1. Asignar, atender, anular, etc.    */
/* cambian el estado y quedan fuera. No exige que cambie la categoría: el modal suele confirmarse con     */
/* la categoría que el requerimiento ya traía. */
-- Fecha             : 09/03/2022
----------------------------------------------------------------------------------------------------------------------  
-- Fec Actualización : 
-- Responsable       : 
-- RQ                : 
-- Motivo            : 
--**********************************************************************************************************************

/* EXEC dbo.spGR_Requerimiento_ListarClasificados                                              
          '<R><XmlGR cCodPer="0295" iDias="90" iTop="30" /></R>'  */



IF OBJECT_ID('dbo.spGR_Requerimiento_ListarClasificados', 'P') IS NOT NULL
    DROP PROCEDURE dbo.spGR_Requerimiento_ListarClasificados
GO

CREATE PROCEDURE dbo.spGR_Requerimiento_ListarClasificados
(
    @ptXmlGR Text
)
AS
BEGIN

    SET NOCOUNT ON

    DECLARE
        @idoc     Int,
        @cCodPer  Char(4),
        @iDias    Int,
        @iTop     Int,
        @sdDesde  SmallDateTime

    IF @ptXmlGR IS NULL
    BEGIN
        RETURN
    END

    EXEC sp_xml_preparedocument @idoc OUTPUT, @ptXmlGR

    SELECT
        @cCodPer = cCodPer,
        @iDias   = iDias,
        @iTop    = iTop
    FROM OPENXML(@idoc, '/R/XmlGR', 1)
    WITH
    (
        cCodPer Char(4),
        iDias   Int,
        iTop    Int
    )

    EXEC sp_xml_removedocument @idoc

    IF ISNULL(@cCodPer, '') = ''
    BEGIN
        RETURN
    END

    SET @iDias = ISNULL(NULLIF(@iDias, 0), 90)
    SET @iTop  = ISNULL(NULLIF(@iTop, 0), 30)

    IF @iDias > 365 SET @iDias = 365
    IF @iTop  > 100 SET @iTop  = 100

    SET @sdDesde = DATEADD(DAY, -@iDias, GETDATE())

    SELECT TOP (@iTop)
        x.iCodigo_Req               AS codigoRequerimiento,
        MAX(x.sdFechaOperacion)     AS fechaClasificacion
    FROM
    (
        -- Operaciones tipo 3 del usuario sobre requerimientos que estaban Registrados,
        -- con el número de la operación siguiente sobre el mismo requerimiento.
        SELECT
            h.iCodigo_Req,
            h.sdFechaOperacion,
            (
                SELECT MIN(n.iCodigoOperacion)
                FROM GRMaeRequerimientoH n (NOLOCK)
                WHERE n.iCodigo_Req = h.iCodigo_Req
                  AND n.iCodigoOperacion > h.iCodigoOperacion
            ) AS iSiguiente
        FROM GRMaeRequerimientoH h (NOLOCK)
        WHERE h.cUsuarioOperacion = @cCodPer
          AND h.iCodigo_TipOpe    = 3
          AND h.siCodigo_Est      = 1
          AND h.sdFechaOperacion >= @sdDesde
    ) x
    INNER JOIN GRMaeRequerimiento r (NOLOCK) ON r.iCodigo_Req = x.iCodigo_Req
    LEFT  JOIN GRMaeRequerimientoH s (NOLOCK) ON s.iCodigoOperacion = x.iSiguiente
    WHERE r.bActivo_Req = 1
      -- Estado después de la operación: foto siguiente, o el requerimiento actual si no hay otra.
      AND ISNULL(s.siCodigo_Est, r.siCodigo_Est) = 1
    GROUP BY x.iCodigo_Req
    ORDER BY MAX(x.sdFechaOperacion) DESC, x.iCodigo_Req DESC

    SET NOCOUNT OFF

END
GO


/*********************************************************************************************************/
/* ÍNDICES (a ejecutar por el equipo de base de datos, preferiblemente en ventana de baja carga).        */
/* GRMaeRequerimientoH tiene ~2,1 millones de filas y solo la clave primaria por iCodigoOperacion; sin    */
/* estos índices el SP recorre la tabla completa. GRMaeRequerimientoH también la escribe el sistema       */
/* legado: cada índice añade un costo pequeño a cada operación registrada.                                */
/*********************************************************************************************************/

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_GRMaeRequerimientoH_UsuarioOp'
               AND object_id = OBJECT_ID('dbo.GRMaeRequerimientoH'))
    CREATE NONCLUSTERED INDEX IX_GRMaeRequerimientoH_UsuarioOp
        ON dbo.GRMaeRequerimientoH (cUsuarioOperacion, iCodigo_TipOpe, sdFechaOperacion)
        INCLUDE (iCodigo_Req, siCodigo_Est)
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_GRMaeRequerimientoH_Req'
               AND object_id = OBJECT_ID('dbo.GRMaeRequerimientoH'))
    CREATE NONCLUSTERED INDEX IX_GRMaeRequerimientoH_Req
        ON dbo.GRMaeRequerimientoH (iCodigo_Req, iCodigoOperacion)
        INCLUDE (siCodigo_Est)
GO


/*********************************************************************************************************/
/* PRUEBA: para el usuario 0295 debe aparecer, entre otros, el requerimiento 286982.                      */
/*********************************************************************************************************/
-- EXEC dbo.spGR_Requerimiento_ListarClasificados '<R><XmlGR cCodPer="0295" iDias="90" iTop="30" /></R>'
