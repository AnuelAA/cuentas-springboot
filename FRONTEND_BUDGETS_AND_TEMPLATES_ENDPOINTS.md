================================================================================
ENDPOINTS PARA PRESUPUESTOS Y PLANTILLAS DE TRANSACCIONES - DOCUMENTACIÓN FRONTEND
================================================================================

Este documento describe los nuevos endpoints disponibles para gestionar 
presupuestos y plantillas de transacciones.

================================================================================
1. PRESUPUESTOS (BUDGETS)
================================================================================

### 1.1 Obtener todos los presupuestos

Endpoint: GET /api/users/{userId}/budgets

Query Parameters (opcionales):
  - startDate: string (formato: "YYYY-MM-DD") - Filtrar presupuestos activos desde esta fecha
  - endDate: string (formato: "YYYY-MM-DD") - Filtrar presupuestos activos hasta esta fecha

Respuesta esperada:
  - 200 OK: Array de presupuestos

Ejemplo de request:
  GET /api/users/1/budgets?startDate=2024-01-01&endDate=2024-12-31

Ejemplo de respuesta:
  [
    {
      "budgetId": 1,
      "userId": 1,
      "categoryId": 5,
      "amount": 500.00,
      "period": "monthly",
      "startDate": "2024-01-01",
      "endDate": null,
      "createdAt": "2024-01-01T10:00:00Z",
      "updatedAt": "2024-01-01T10:00:00Z"
    }
  ]

---

### 1.2 Obtener estado de presupuestos (gastado vs presupuestado)

Endpoint: GET /api/users/{userId}/budgets/status

Descripción: Obtiene el estado de todos los presupuestos con cálculos de gastos.

Query Parameters (opcionales):
  - startDate: string (formato: "YYYY-MM-DD") - Fecha de inicio del período a analizar
  - endDate: string (formato: "YYYY-MM-DD") - Fecha de fin del período a analizar

Nota: Si no se proporcionan fechas, se usa el mes actual por defecto.

Respuesta esperada:
  - 200 OK: Array de estados de presupuestos

Ejemplo de request:
  GET /api/users/1/budgets/status?startDate=2024-01-01&endDate=2024-01-31

Ejemplo de respuesta:
  [
    {
      "budgetId": 1,
      "categoryId": 5,
      "categoryName": "Comida",
      "budgetAmount": 500.00,
      "spentAmount": 350.00,
      "remainingAmount": 150.00,
      "percentageUsed": 70.0,
      "isExceeded": false,
      "period": "monthly",
      "startDate": "2024-01-01",
      "endDate": "2024-01-31"
    }
  ]

Campos importantes:
  - budgetAmount: Monto presupuestado
  - spentAmount: Monto gastado en el período
  - remainingAmount: Monto restante (puede ser negativo si se excedió)
  - percentageUsed: Porcentaje usado (0-100+)
  - isExceeded: true si se excedió el presupuesto

---

### 1.3 Crear presupuesto

Endpoint: POST /api/users/{userId}/budgets

Request Body:
  {
    "categoryId": 5,                    // Obligatorio
    "amount": 500.00,                   // Obligatorio, debe ser > 0
    "period": "monthly",                 // Obligatorio: "monthly" o "yearly"
    "startDate": "2024-01-01",          // Opcional (formato: "YYYY-MM-DD")
    "endDate": null                      // Opcional (formato: "YYYY-MM-DD")
  }

Validaciones:
  - amount debe ser > 0
  - period debe ser "monthly" o "yearly"
  - categoryId debe existir y pertenecer al usuario
  - Si endDate se proporciona, debe ser >= startDate
  - Si startDate no se proporciona:
    * Para "monthly": se usa el primer día del mes actual
    * Para "yearly": se usa el primer día del año actual

Respuesta esperada:
  - 201 Created: Presupuesto creado
  - 400 Bad Request: Datos inválidos

Ejemplo de request:
  POST /api/users/1/budgets
  {
    "categoryId": 5,
    "amount": 500.00,
    "period": "monthly",
    "startDate": "2024-01-01"
  }

---

### 1.4 Actualizar presupuesto

Endpoint: PUT /api/users/{userId}/budgets/{budgetId}

Request Body (todos los campos opcionales):
  {
    "amount": 600.00,
    "period": "monthly",
    "startDate": "2024-02-01",
    "endDate": "2024-12-31"
  }

Nota: Solo se actualizan los campos proporcionados. Los campos no incluidos 
mantienen su valor actual.

Respuesta esperada:
  - 200 OK: Presupuesto actualizado
  - 400 Bad Request: Datos inválidos
  - 404 Not Found: Presupuesto no encontrado

---

### 1.5 Eliminar presupuesto

Endpoint: DELETE /api/users/{userId}/budgets/{budgetId}

Respuesta esperada:
  - 204 No Content: Presupuesto eliminado
  - 404 Not Found: Presupuesto no encontrado

---

## 2. PLANTILLAS DE TRANSACCIONES (TRANSACTION TEMPLATES)
================================================================================

### 2.1 Obtener todas las plantillas

Endpoint: GET /api/users/{userId}/transaction-templates

Respuesta esperada:
  - 200 OK: Array de plantillas

Ejemplo de respuesta:
  [
    {
      "templateId": 1,
      "userId": 1,
      "name": "Pago de luz mensual",
      "categoryId": 5,
      "categoryName": "Servicios",
      "type": "expense",
      "amount": 80.00,
      "assetId": 2,
      "relatedAssetId": null,
      "liabilityId": null,
      "description": "Factura de electricidad",
      "createdAt": "2024-01-01T10:00:00Z",
      "updatedAt": "2024-01-01T10:00:00Z"
    }
  ]

---

### 2.2 Crear plantilla

Endpoint: POST /api/users/{userId}/transaction-templates

Request Body:
  {
    "name": "Pago de luz mensual",      // Obligatorio
    "categoryId": 5,                     // Opcional (si no se proporciona, usar categoryName)
    "categoryName": "Servicios",         // Opcional (si no hay categoryId, se crea automáticamente)
    "type": "expense",                   // Obligatorio: "income" o "expense"
    "amount": 80.00,                     // Obligatorio, debe ser > 0
    "assetId": 2,                        // Opcional
    "relatedAssetId": null,              // Opcional
    "liabilityId": null,                 // Opcional
    "description": "Factura de electricidad"  // Opcional
  }

Validaciones:
  - name es obligatorio y no puede estar vacío
  - type debe ser "income" o "expense"
  - amount debe ser > 0
  - Si categoryId no se proporciona pero categoryName sí, se crea la categoría automáticamente
  - Los activos y pasivos deben existir y pertenecer al usuario

Respuesta esperada:
  - 201 Created: Plantilla creada
  - 400 Bad Request: Datos inválidos

Ejemplo de request:
  POST /api/users/1/transaction-templates
  {
    "name": "Pago de luz mensual",
    "categoryName": "Servicios",
    "type": "expense",
    "amount": 80.00,
    "assetId": 2,
    "description": "Factura de electricidad"
  }

Nota importante: Si proporcionas categoryName sin categoryId, el backend creará 
automáticamente la categoría si no existe.

---

### 2.3 Actualizar plantilla

Endpoint: PUT /api/users/{userId}/transaction-templates/{templateId}

Request Body (todos los campos opcionales):
  {
    "name": "Pago de luz y agua mensual",
    "amount": 120.00,
    "description": "Facturas de servicios"
  }

Nota: Solo se actualizan los campos proporcionados. Los campos no incluidos 
mantienen su valor actual.

Para actualizar categoría:
  - Puedes proporcionar categoryId (debe existir)
  - O categoryName (se crea automáticamente si no existe)

Respuesta esperada:
  - 200 OK: Plantilla actualizada
  - 400 Bad Request: Datos inválidos
  - 404 Not Found: Plantilla no encontrada

---

### 2.4 Eliminar plantilla

Endpoint: DELETE /api/users/{userId}/transaction-templates/{templateId}

Respuesta esperada:
  - 204 No Content: Plantilla eliminada
  - 404 Not Found: Plantilla no encontrada

---

## 3. CASOS DE USO Y EJEMPLOS
================================================================================

### 3.1 Mostrar estado de presupuestos en dashboard

```javascript
// Obtener estado de presupuestos del mes actual
async function loadBudgetStatus(userId) {
  const response = await fetch(`/api/users/${userId}/budgets/status`);
  if (response.ok) {
    const budgets = await response.json();
    
    budgets.forEach(budget => {
      console.log(`${budget.categoryName}: ${budget.percentageUsed}% usado`);
      if (budget.isExceeded) {
        console.warn(`⚠️ Presupuesto excedido por ${Math.abs(budget.remainingAmount)}`);
      }
    });
  }
}
```

### 3.2 Crear presupuesto mensual

```javascript
async function createMonthlyBudget(userId, categoryId, amount) {
  const budget = {
    categoryId: categoryId,
    amount: amount,
    period: "monthly"
    // startDate se establece automáticamente al primer día del mes actual
  };
  
  const response = await fetch(`/api/users/${userId}/budgets`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(budget)
  });
  
  if (response.ok) {
    return await response.json();
  }
}
```

### 3.3 Usar plantilla para crear transacción

```javascript
async function createTransactionFromTemplate(userId, templateId) {
  // 1. Obtener la plantilla
  const templateResponse = await fetch(
    `/api/users/${userId}/transaction-templates/${templateId}`
  );
  const template = await templateResponse.json();
  
  // 2. Crear transacción usando los datos de la plantilla
  const transaction = {
    categoryId: template.categoryId,
    type: template.type,
    amount: template.amount,
    assetId: template.assetId,
    relatedAssetId: template.relatedAssetId,
    liabilityId: template.liabilityId,
    description: template.description,
    transactionDate: new Date().toISOString().split('T')[0] // Fecha actual
  };
  
  const response = await fetch(`/api/users/${userId}/transactions`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(transaction)
  });
  
  return await response.json();
}
```

### 3.4 Prellenar formulario de transacción con plantilla

```javascript
// Al seleccionar una plantilla en el formulario
function applyTemplateToForm(template) {
  setFormData({
    categoryId: template.categoryId,
    categoryName: template.categoryName,
    type: template.type,
    amount: template.amount,
    assetId: template.assetId,
    relatedAssetId: template.relatedAssetId,
    liabilityId: template.liabilityId,
    description: template.description,
    // transactionDate se establece a la fecha actual o la que el usuario elija
  });
}
```

---

## 4. NOTAS IMPORTANTES
================================================================================

### 4.1 Presupuestos

1. **Períodos:**
   - "monthly": Calcula gastos del mes especificado (o mes actual)
   - "yearly": Calcula gastos del año especificado (o año actual)

2. **Fechas:**
   - Si startDate no se proporciona al crear, se usa:
     * Primer día del mes actual para "monthly"
     * Primer día del año actual para "yearly"
   - endDate puede ser null (presupuesto indefinido)

3. **Cálculo de gastos:**
   - Solo cuenta transacciones de tipo "expense"
   - Filtra por categoryId exacto (no incluye subcategorías)
   - El período de cálculo depende del tipo de presupuesto (monthly/yearly)

4. **Múltiples presupuestos:**
   - Un usuario puede tener múltiples presupuestos para la misma categoría
   - Deben tener diferentes períodos o fechas de inicio
   - La restricción UNIQUE previene duplicados exactos

### 4.2 Plantillas de Transacciones

1. **Categorías:**
   - Puedes usar categoryId (si ya existe)
   - O categoryName (se crea automáticamente si no existe)
   - Si proporcionas ambos, se usa categoryId

2. **Validaciones:**
   - Todos los activos y pasivos deben existir y pertenecer al usuario
   - El backend valida la propiedad de todos los recursos

3. **Uso:**
   - Las plantillas solo prellenan el formulario
   - El usuario puede modificar cualquier campo antes de crear la transacción
   - No se crean transacciones automáticamente desde plantillas

---

## 5. MANEJO DE ERRORES
================================================================================

### Códigos de estado HTTP

- **200 OK**: Operación exitosa
- **201 Created**: Recurso creado exitosamente
- **204 No Content**: Recurso eliminado exitosamente
- **400 Bad Request**: Datos inválidos (amount <= 0, period inválido, etc.)
- **401 Unauthorized**: Token inválido o expirado
- **403 Forbidden**: Usuario no tiene permiso
- **404 Not Found**: Recurso no encontrado (budgetId, templateId, etc.)
- **409 Conflict**: Conflicto (ej: presupuesto duplicado)
- **500 Internal Server Error**: Error del servidor

### Ejemplo de manejo de errores

```javascript
async function createBudget(userId, budgetData) {
  try {
    const response = await fetch(`/api/users/${userId}/budgets`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(budgetData)
    });
    
    if (response.status === 201) {
      return await response.json();
    } else if (response.status === 400) {
      const error = await response.text();
      throw new Error(`Datos inválidos: ${error}`);
    } else if (response.status === 404) {
      throw new Error('Categoría no encontrada');
    } else {
      throw new Error('Error al crear presupuesto');
    }
  } catch (error) {
    console.error('Error:', error);
    // Mostrar mensaje al usuario
  }
}
```

---

## 6. INTEGRACIÓN CON OTROS ENDPOINTS
================================================================================

### 6.1 Presupuestos + Categorías

Para mostrar presupuestos con información de categorías:

```javascript
// 1. Obtener presupuestos
const budgets = await fetch(`/api/users/${userId}/budgets`).then(r => r.json());

// 2. Obtener categorías
const categories = await fetch(`/api/users/${userId}/categories`).then(r => r.json());

// 3. Combinar datos
const budgetsWithCategories = budgets.map(budget => {
  const category = categories.find(c => c.categoryId === budget.categoryId);
  return { ...budget, category };
});
```

### 6.2 Plantillas + Activo Principal

Para usar el activo principal por defecto en plantillas:

```javascript
// 1. Obtener activo principal
const primaryAsset = await fetch(`/api/users/${userId}/assets/primary`).then(r => r.json());

// 2. Crear plantilla con activo principal
const template = {
  name: "Gasto mensual",
  type: "expense",
  amount: 100.00,
  assetId: primaryAsset.assetId  // Usar activo principal
};
```

---

## 7. FORMATO DE FECHAS
================================================================================

Todas las fechas deben estar en formato ISO 8601:
- Fecha solamente: `"YYYY-MM-DD"` (ej: "2024-01-15")
- Fecha y hora: `"YYYY-MM-DDTHH:mm:ssZ"` (ej: "2024-01-15T10:30:00Z")

Ejemplos:
- `startDate: "2024-01-01"`
- `endDate: "2024-12-31"`
- `createdAt: "2024-01-01T10:00:00Z"`

---

## 8. PREGUNTAS FRECUENTES
================================================================================

**P: ¿Puedo tener múltiples presupuestos para la misma categoría?**
R: Sí, pero deben tener diferentes períodos o fechas de inicio.

**P: ¿Qué pasa si una categoría tiene presupuesto mensual y anual?**
R: Ambos se calculan independientemente. El frontend puede mostrar ambos estados.

**P: ¿Las plantillas crean transacciones automáticamente?**
R: No, solo prellenan el formulario. El usuario debe confirmar y crear la transacción.

**P: ¿Qué pasa si uso categoryName sin categoryId?**
R: El backend crea la categoría automáticamente si no existe.

**P: ¿Puedo actualizar solo algunos campos de un presupuesto?**
R: Sí, en el PUT solo incluye los campos que quieres actualizar.

**P: ¿Cómo calculo el porcentaje usado si es > 100%?**
R: El backend calcula `percentageUsed = (spentAmount / budgetAmount) * 100`, 
   que puede ser > 100 si se excedió el presupuesto.

---

**Fecha de creación:** 2024-01-XX
**Versión del backend:** Compatible con presupuestos y plantillas v1.0

================================================================================

