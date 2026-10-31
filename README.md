# Sistema de Solicitudes de Reembolso de Gastos Corporativos

## 1. Diseño Propuesto (Previo a la Implementación)

| Requisito Funcional | Punto de Variabilidad | Patrón de Diseño Propuesto | Justificación (Test de Explicabilidad en 1 oración) |
| :--- | :--- | :--- | :--- |
| **RF1 — Validación en varias etapas** | La secuencia y cantidad de verificaciones independientes (tope de categoría, documentos soporte, duplicados y futuras reglas) que debe superar una solicitud. | **Chain of Responsibility** | Se elige Chain of Responsibility porque permite encadenar verificaciones desacopladas donde cada eslabón decide si rechaza la solicitud o delega al siguiente, permitiendo agregar nuevas reglas de validación en el futuro sin modificar las existentes. |
| **RF2 — Cálculo distinto por categoría de gasto** | La fórmula matemática de cálculo del monto aprobado según la categoría del gasto (`VIAJE`, `REPRESENTACION`, `CAPACITACION` y futuras categorías). | **Strategy** | Se elige Strategy porque encapsula cada algoritmo de cálculo por categoría en una clase independiente bajo una interfaz común, permitiendo agregar nuevas categorías de gasto sin alterar las fórmulas existentes ni usar bloques condicionales rígidos. |
| **RF3 — Notificación simultánea a varios interesados** | El conjunto de destinatarios (Empleado, Contabilidad, Auditoría y futuros paneles) y la reacción particular de cada uno cuando una solicitud cambia de estado. | **Observer** | Se elige Observer porque permite suscribir múltiples interesados a las transiciones de estado de una solicitud para que reaccionen independientemente sin acoplar el servicio de aplicación a los detalles de cada notificador. |

## COMPILACION DEL PROYECTO
![Captura de consola de la compilacion del proyecto sin que el dominio importe clases](<./evidencias/parte-1/Compillacion del proyecto sin que el dominio importe.png>)

---

## 2. Decisiones de Diseño de la Parte 1

### Decisión de Diseño 1: Orden de encadenamiento en `CadenaValidadores`

#### Orden elegido
$$\text{ValidadorDocumentosSoporte} \longrightarrow \text{ValidadorMontoMaximo} \longrightarrow \text{ValidadorDuplicados}$$

#### Justificación con criterio de eficiencia (*Fail-Fast*)
La cadena de validación se ordenó evaluando el costo computacional de menor a mayor complejidad algorítmica:

1. **`ValidadorDocumentosSoporte` ($\mathcal{O}(1)$ directo):** Evalúa un valor booleano en memoria (`solicitud.isDocumentosSoporte()`). Es una operación casi instantánea a nivel de CPU.
2. **`ValidadorMontoMaximo` ($\mathcal{O}(1)$ con overhead mínimo):** Realiza una búsqueda por clave en un mapa estático en memoria (`Map.get()`) y compara dos objetos `BigDecimal`.
3. **`ValidadorDuplicados` ($\mathcal{O}(n)$, donde $n$ es la cantidad de solicitudes previas):** Es el eslabón más costoso computacionalmente, pues debe recorrer mediante un `Stream` la lista de solicitudes pendientes en memoria cargada desde la base de datos y evaluar comparaciones de fechas y categorías para cada elemento.

#### Trade-offs: ¿Qué se gana y qué se pierde?
* **Qué se gana:** Se maximiza el rendimiento y la eficiencia del servidor bajo el principio *Fail-Fast*. Cualquier solicitud inválida por falta de soportes o por exceder el tope de categoría es rechazada en tiempo constante $\mathcal{O}(1)$, cortocircuitando el flujo de inmediato y evitando por completo la iteración $\mathcal{O}(n)$ sobre colecciones de la base de datos.
* **Qué se pierde:** Si una solicitud incumple múltiples condiciones en simultáneo (por ejemplo, no tiene documentos y además es un duplicado), el usuario solo conocerá primero el error de menor costo computacional (falta de documentos). No obstante, este comportamiento es preferible frente al desperdicio de CPU que implicaría evaluar listas contra registros duplicados para solicitudes que ni siquiera tienen soporte físico adjunto.

---

### Decisión de Diseño 2: Mecanismo de selección de calculadoras (`Map` inyectado vs. `switch` manual)

#### Mecanismo elegido
Construcción de un `Map<CategoriaGasto, CalculadoraReembolso>` dentro de `SelectorCalculadora`, auto-ensamblado mediante la inyección por constructor de la lista de todos los `@Component` que implementan `CalculadoraReembolso` descubiertos por el contenedor de Spring.

#### Justificación considerando RF2 (Principio Abierto/Cerrado — OCP)
El requisito funcional **RF2** declara explícitamente que el área de Finanzas agregará nuevas categorías en los próximos semestres (por ejemplo, "bienestar"), cada una con su fórmula propia y **sin modificar el código existente**:

* **Cumplimiento de OCP:** Con el mapa inyectado automáticamente, para incorporar la categoría `BIENESTAR` solo se requiere crear la nueva clase `CalculadoraBienestar implements CalculadoraReembolso` anotada con `@Component`. Spring la inyecta automáticamente en el mapa en tiempo de arranque; no es necesario modificar ni una sola línea de código en `SelectorCalculadora` ni en `SolicitudReembolsoService`.
* **Descarte del `switch` manual:** Un método basado en `switch (categoria)` violaría de forma directa el Principio Abierto/Cerrado, ya que cada nueva categoría forzaría a abrir y editar la clase selectora, reintroduciendo riesgos de regresión en código previamente probado y validado.

---

## 3. Checkpoints de Verificación de la Parte 1

### 3.1. Pruebas Automatizadas
El proyecto cuenta con una suite que cubre pruebas unitarias para cada eslabón y calculadora, además de una prueba de integración sobre el contexto de Spring:
* **Validadores:** `validadorDocumentosSoporteRechazaCuandoNoHaySoporte`, `validadorMontoMaximoRechazaCuandoSuperaTopeDeCategoria`, `validadorDuplicadosRechazaCuandoExistePendienteEnMismoMesYCategoria`.
* **Calculadoras:** `calculadoraViajeAplicaTopeDiarioMultiplicadoPorDias`, `calculadoraRepresentacionReembolsaMontoCompletoHastaTopeMensual`, `calculadoraCapacitacionReembolsaOchentaPorCientoHastaTopeAnual`.
* **Integración:** `flujoCompletoCrearEnTresCategoriasValidarCalcularYCambiarEstado` (@SpringBootTest).

Para ejecutar la verificación:
```bash
mvn clean test
```
## Pruebas en Verde

![Captura de consola de la correcta ejecucion de las pruebas](<./evidencias/parte-1/Ejecucion de pruebas.png>)


### 3.2. Verificación de Independencia del Validador de Duplicados
Se verifica que `ValidadorDuplicados` recibe la información pre-cargada por el servicio y no importa clases de infraestructura ni de acceso a datos:

* En sistemas UNIX (Linux/macOS):
  ```bash
  grep "import" src/main/java/com/empresa/reembolsos/procesamiento/validacion/ValidadorDuplicados.java
  ```
* En Windows CMD:
  ```cmd
  findstr "import" src\main\java\com\empresa\reembolsos\procesamiento\validacion\ValidadorDuplicados.java
  ```

## Resultado de los imports

![Captura de consola de los imports](<./evidencias/parte-1/Resultados de los Imports del dominio y Spring.png>)