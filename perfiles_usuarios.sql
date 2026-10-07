-- Perfiles (roles) de los usuarios del sistema de Gestión de Requerimientos.
-- Base: GestionRQLD (usa organizacion..EOMAEPER para el usuario de Windows).
USE GestionRQLD;
GO

-- 1) Un renglón por usuario y rol activo
SELECT
    UPPER(e.VUSUWIN)        AS UsuarioWindows,     -- lo que se escribe en el login
    p.iCodigo_Per           AS CodigoPersonaGr,
    p.cCodPer               AS CodigoPersonal,
    r.vNombre_Rol           AS Rol,
    pr.iCodigo_PerRol       AS CodigoPersonaRol,
    pr.bActivo_PerRol       AS RolActivo
FROM GRMovPersonaRol pr
INNER JOIN GRTabRol      r ON r.siCodigo_Rol = pr.siCodigo_Rol
INNER JOIN GRMaePersona  p ON p.iCodigo_Per  = pr.iCodigo_Per
INNER JOIN organizacion..EOMAEPER e ON e.CCODPER = p.cCodPer AND e.NCODEST NOT IN (6, 7)
WHERE pr.bActivo_PerRol = 1
  AND r.bActivo_Rol = 1
  -- AND r.vNombre_Rol = 'Operador'        -- filtra por rol
  -- AND UPPER(e.VUSUWIN) = 'TARIAS'       -- filtra por usuario
ORDER BY e.VUSUWIN, r.vNombre_Rol;
GO

-- 2) Un renglón por usuario con todos sus roles juntos
SELECT
    UPPER(e.VUSUWIN) AS UsuarioWindows,
    p.iCodigo_Per    AS CodigoPersonaGr,
    STUFF((SELECT ', ' + r2.vNombre_Rol
           FROM GRMovPersonaRol pr2
           INNER JOIN GRTabRol r2 ON r2.siCodigo_Rol = pr2.siCodigo_Rol
           WHERE pr2.iCodigo_Per = p.iCodigo_Per AND pr2.bActivo_PerRol = 1 AND r2.bActivo_Rol = 1
           ORDER BY r2.vNombre_Rol
           FOR XML PATH('')), 1, 2, '') AS Roles
FROM GRMaePersona p
INNER JOIN organizacion..EOMAEPER e ON e.CCODPER = p.cCodPer AND e.NCODEST NOT IN (6, 7)
WHERE LTRIM(RTRIM(ISNULL(e.VUSUWIN, ''))) <> ''
  AND EXISTS (SELECT 1 FROM GRMovPersonaRol x WHERE x.iCodigo_Per = p.iCodigo_Per AND x.bActivo_PerRol = 1)
ORDER BY e.VUSUWIN;
GO

-- 3) Cuántos usuarios hay por rol
SELECT r.vNombre_Rol AS Rol, COUNT(DISTINCT pr.iCodigo_Per) AS Usuarios
FROM GRMovPersonaRol pr
INNER JOIN GRTabRol r ON r.siCodigo_Rol = pr.siCodigo_Rol
WHERE pr.bActivo_PerRol = 1 AND r.bActivo_Rol = 1
GROUP BY r.vNombre_Rol
ORDER BY Usuarios DESC;
GO
