# Sistema de Solicitudes de Reembolso de Gastos Corporativos

## Diseño Propuesto

| Requisito Funcional | Punto de Variabilidad | Patrón de Diseño Propuesto | Justificación (Test de Explicabilidad en 1 oración) |
| :--- | :--- | :--- | :--- |
| **RF1 — Validación en varias etapas** | La secuencia y cantidad de verificaciones independientes (tope de categoría, documentos soporte, duplicados y futuras reglas) que debe superar una solicitud. | **Chain of Responsibility** | Se elige Chain of Responsibility porque permite encadenar verificaciones desacopladas donde cada eslabón decide si rechaza la solicitud o delega al siguiente, permitiendo agregar nuevas reglas de validación en el futuro sin modificar las existentes. |
| **RF2 — Cálculo distinto por categoría de gasto** | La fórmula matemática de cálculo del monto aprobado según la categoría del gasto (`VIAJE`, `REPRESENTACION`, `CAPACITACION` y futuras categorías). | **Strategy** | Se elige Strategy porque encapsula cada algoritmo de cálculo por categoría en una clase independiente bajo una interfaz común, permitiendo agregar nuevas categorías de gasto sin alterar las fórmulas existentes ni usar bloques condicionales rígidos. |
| **RF3 — Notificación simultánea a varios interesados** | El conjunto de destinatarios (Empleado, Contabilidad, Auditoría y futuros paneles) y la reacción particular de cada uno cuando una solicitud cambia de estado. | **Observer** | Se elige Observer porque permite suscribir múltiples interesados a las transiciones de estado de una solicitud para que reaccionen independientemente sin acoplar el servicio de aplicación a los detalles de cada notificador. |

## COMPILACION DEL PROYECTO
![Captura de consola de la compilacion del proyecto sin que el dominio importe clases](<./evidencias/parte-1/Compillacion del proyecto sin que el dominio importe.png>)