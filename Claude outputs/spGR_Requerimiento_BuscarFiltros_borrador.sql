-- =========================================================================
-- spGR_Requerimiento_BuscarFiltros  (SP CUSTOMIZADA para el microservicio
-- service-requerimiento — NO es un SP legado, es nueva)
-- =========================================================================
-- Por qué existe: spGR_Requerimiento_Consultar (el SP real y viejo) recibe
-- ~16 parámetros porque sirve a toda la pantalla del sistema legado. Este
-- microservicio solo necesita los filtros que expone su propio endpoint
-- (GET /api/requerimiento/buscar), así que en vez de adaptar el XML gigante
-- del SP viejo, se escribe un SP nuevo con solo lo que hace falta.
--
-- Mantiene la misma convención que el resto del sistema (un solo parámetro
-- @ptXmlGR con OpenXML) para que sea consistente con los demás SPs y con el
-- patrón que ya usa RequerimientoRepositoryImpl.java (createStoredProcedureQuery
-- con un solo parámetro de texto).
--
-- Exec spGR_Requerimiento_BuscarFiltros
--   @ptXmlGR = N'<R><XmlGR iCodigoReq="0" vTituloReq="TODO" cFechaInicialReq="TODO"
--     cFechaFinalReq="TODO" iCodigoPerSolicitante="0" iCodUoSolicitante="0"
--     iCodigoPerResponsable="6073" iCodUoResponsable="0" siCodigoEst="0" /></R>'
--
-- NOTA IMPORTANTE sobre "responsable": en este sistema no hay una columna fija
-- "responsable" en GRMaeRequerimiento. El responsable actual es la persona
-- destino del último movimiento vigente en GRMovRequerimiento
-- (iCodigo_PerDes_ReqMov), tal como ya se usa en spGR_Requerimiento_ConsultarCorreos.
-- Si en su modelo real existe otra forma de saber el responsable, ajusten el
-- CTE UltimoMovimiento.
-- =========================================================================

Use GestionRQLD
Go
Set Quoted_identifier On
Go
Set Ansi_nulls On
Go

Alter Procedure dbo.spGR_Requerimiento_BuscarFiltros
(
    @ptXmlGR Varchar(max)
)
As
Begin
    Set NoCount On

    Declare
        @idoc                     Int,
        @iCodigoReq               Int,
        @vTituloReq               Varchar(200),
        @cFechaInicialReq         Varchar(10),
        @cFechaFinalReq           Varchar(10),
        @iCodigoPerSolicitante    Int,
        @iCodUoSolicitante        Int,
        @iCodigoPerResponsable    Int,
        @iCodUoResponsable        Int,
        @siCodigoEst              SmallInt

    If @ptXmlGR Is Null
    Begin
        Return
    End

    Exec sp_xml_preparedocument @idoc OutPut, @ptXmlGR
    Select
        @iCodigoReq            = iCodigoReq,
        @vTituloReq            = vTituloReq,
        @cFechaInicialReq      = cFechaInicialReq,
        @cFechaFinalReq        = cFechaFinalReq,
        @iCodigoPerSolicitante = iCodigoPerSolicitante,
        @iCodUoSolicitante     = iCodUoSolicitante,
        @iCodigoPerResponsable = iCodigoPerResponsable,
        @iCodUoResponsable     = iCodUoResponsable,
        @siCodigoEst           = siCodigoEst
    From OpenXML (@idoc, '/R/XmlGR', 1)
    With
    (
        iCodigoReq              Int,
        vTituloReq              Varchar(200),
        cFechaInicialReq        Varchar(10),
        cFechaFinalReq          Varchar(10),
        iCodigoPerSolicitante   Int,
        iCodUoSolicitante       Int,
        iCodigoPerResponsable   Int,
        iCodUoResponsable       Int,
        siCodigoEst             SmallInt
    )
    Exec sp_xml_removedocument @idoc

    -- Valores por defecto ("sin filtro"), igual que el resto del sistema
    Set @iCodigoReq            = IsNull(@iCodigoReq, 0)
    Set @vTituloReq            = IsNull(@vTituloReq, 'TODO')
    Set @cFechaInicialReq      = IsNull(@cFechaInicialReq, 'TODO')
    Set @cFechaFinalReq        = IsNull(@cFechaFinalReq, 'TODO')
    Set @iCodigoPerSolicitante = IsNull(@iCodigoPerSolicitante, 0)
    Set @iCodUoSolicitante     = IsNull(@iCodUoSolicitante, 0)
    Set @iCodigoPerResponsable = IsNull(@iCodigoPerResponsable, 0)
    Set @iCodUoResponsable     = IsNull(@iCodUoResponsable, 0)
    Set @siCodigoEst           = IsNull(@siCodigoEst, 0)

    ;With UltimoMovimiento As
    (
        Select
            iCodigo_Req,
            iCodigo_PerDes_ReqMov,
            Row_Number() Over (Partition By iCodigo_Req Order By iCodigo_ReqMov Desc) As rn
        From GRMovRequerimiento (NoLock)
        Where bActivo_ReqMov = 1
    )

    Select
        r.iCodigo_Req                                                                          As codigoRequerimiento,
        r.vSumilla_Req                                                                          As titulo,
        r.sdFecha_Req                                                                           As fechaRegistro,
        r.vDescripcion_Req                                                                      As descripcion,

        -- Solicitante
        r.iCodigo_Per                                                                           As codigoPersonaSolicitante,
        IsNull(perSol.VAPEPAT,'') +' '+ IsNull(perSol.VAPEMAT,'') +' '+ IsNull(perSol.VNOMBRE,'') As solicitante,
        perSolGR.iCodUo                                                                         As codigoUoSolicitante,
        uoSol.vDesLUo                                                                           As unidadOrganicaSolicitante,
        perSol.nCodCar                                                                          As codigoCargoSolicitante,
        carSol.vDesCar                                                                          As cargoSolicitante,

        -- Responsable actual = destino del último movimiento vigente
        um.iCodigo_PerDes_ReqMov                                                                As codigoPersonaResponsable,
        IsNull(perResp.VAPEPAT,'') +' '+ IsNull(perResp.VAPEMAT,'') +' '+ IsNull(perResp.VNOMBRE,'') As responsable,
        perRespGR.iCodUo                                                                        As codigoUoResponsable,
        uoResp.vDesLUo                                                                          As unidadOrganicaResponsable,
        perResp.nCodCar                                                                         As codigoCargoResponsable,
        carResp.vDesCar                                                                         As cargoResponsable,

        -- Estado
        r.siCodigo_Est                                                                          As codigoEstado,
        est.vNombre_Est                                                                         As estado,

        -- Categoría / Subcategoría
        csc.siCodigo_Cat                                                                        As codigoCategoria,
        cat.vNombre_Cat                                                                         As categoria,
        csc.siCodigo_SubCat                                                                     As codigoSubcategoria,
        subCat.vNombre_SubCat                                                                   As subcategoria,

        -- Prioridad (catálogo EAV GRMaeAtributos)
        r.iCodigo_Pri                                                                           As codigoPrioridad,
        atrPri.cDescripcion_Atr                                                                 As prioridad,

        -- Unidad orgánica del requerimiento
        r.iCodigo_Uo                                                                            As codigoUoRequerimiento,
        uoReq.vDesLUo                                                                           As unidadOrganicaRequerimiento,

        r.iCodigo_CSC                                                                           As codigoCategoriaSubcategoria

    From GRMaeRequerimiento r (NoLock)

    -- Solicitante
    Inner Join GRMaePersona perSolGR (NoLock) On (perSolGR.iCodigo_Per = r.iCodigo_Per)
    Inner Join organizacion..EOMAEPER perSol (NoLock) On (perSol.CCODPER = perSolGR.CCODPER)
    Left Join organizacion..eoMaeUo uoSol (NoLock) On (uoSol.nCodUo = perSolGR.iCodUo)
    Left Join organizacion..EOMaeCar carSol (NoLock) On (carSol.nCodCar = perSol.nCodCar)

    -- Responsable actual (último movimiento)
    Left Join UltimoMovimiento um On (um.iCodigo_Req = r.iCodigo_Req And um.rn = 1)
    Left Join GRMaePersona perRespGR (NoLock) On (perRespGR.iCodigo_Per = um.iCodigo_PerDes_ReqMov)
    Left Join organizacion..EOMAEPER perResp (NoLock) On (perResp.CCODPER = perRespGR.CCODPER)
    Left Join organizacion..eoMaeUo uoResp (NoLock) On (uoResp.nCodUo = perRespGR.iCodUo)
    Left Join organizacion..EOMaeCar carResp (NoLock) On (carResp.nCodCar = perResp.nCodCar)

    -- Estado
    Inner Join GRTabEstado est (NoLock) On (est.siCodigo_Est = r.siCodigo_Est)

    -- Categoría / Subcategoría
    Left Join GRMaeCategoriaSubCategoria csc (NoLock) On (csc.iCodigo_CSC = r.iCodigo_CSC)
    Left Join GRTabCategoria cat (NoLock) On (cat.siCodigo_Cat = csc.siCodigo_Cat)
    Left Join GRTabSubCategoria subCat (NoLock) On (subCat.siCodigo_SubCat = csc.siCodigo_SubCat)

    -- Prioridad
    Left Join GRMaeAtributos atrPri (NoLock) On (atrPri.iCodigo_Atr = r.iCodigo_Pri And atrPri.cMascara_Atr = 'iCodigo_Pri')

    -- Unidad orgánica del requerimiento
    Left Join GRMaeUnidadOrganica uoReqLocal (NoLock) On (uoReqLocal.iCodigo_Uo = r.iCodigo_Uo)
    Left Join organizacion..eoMaeUo uoReq (NoLock) On (uoReq.nCodUo = uoReqLocal.iCodUo)

    Where (@iCodigoReq = 0 Or r.iCodigo_Req = @iCodigoReq)
      And (@vTituloReq = 'TODO' Or r.vSumilla_Req Like '%' + @vTituloReq + '%')
      And (@cFechaInicialReq = 'TODO' Or r.sdFecha_Req >= Convert(SmallDateTime, @cFechaInicialReq))
      And (@cFechaFinalReq = 'TODO' Or r.sdFecha_Req < DateAdd(Day, 1, Convert(SmallDateTime, @cFechaFinalReq)))
      And (@iCodigoPerSolicitante = 0 Or r.iCodigo_Per = @iCodigoPerSolicitante)
      And (@iCodUoSolicitante = 0 Or perSolGR.iCodUo = @iCodUoSolicitante)
      And (@iCodigoPerResponsable = 0 Or um.iCodigo_PerDes_ReqMov = @iCodigoPerResponsable)
      And (@iCodUoResponsable = 0 Or perRespGR.iCodUo = @iCodUoResponsable)
      And (@siCodigoEst = 0 Or r.siCodigo_Est = @siCodigoEst)

    Order By r.iCodigo_Req Desc

    Set NoCount Off
End
Go
Set Quoted_identifier Off
Go
Set Ansi_nulls On
Go
