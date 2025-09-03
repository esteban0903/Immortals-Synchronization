# Análisis de Deadlock - Estrategias de Sincronización

## Resumen 

La implementación de las estrategias NAIVE y ORDERED demostró exitosamente la diferencia entre un enfoque que causa deadlocks y uno que los previene.

## Resultados Observados

### Estrategia ORDERED (Sin Deadlocks)
- **Comportamiento**: Ejecución fluida y continua
- **Batallas ejecutadas**: 1000+ sin interrupciones
- **Sincronización**: Lock ordering consistente (immortal con ID menor primero)
- **Estado final**: Simulación completada exitosamente

```
[ORDERED] Immortal_0 attacks Immortal_1! (98 HP)
[ORDERED] Immortal_2 attacks Immortal_4! (47 HP)
[ORDERED] Immortal_1 attacks Immortal_3! (91 HP)
...
Simulation completed successfully!
```

### Estrategia NAIVE (Con Deadlocks)
- **Comportamiento**: Aplicación se congela después de ~30 batallas
- **Última acción**: "[NAIVE] Immortal_4 attacks Immortal_3! (50 HP)"
- **Estado final**: Deadlock detectado
- **Threads involucrados**: Virtual Threads en ForkJoinPool

```
[NAIVE] Immortal_0 attacks Immortal_2! (92 HP)
[NAIVE] Immortal_1 attacks Immortal_4! (52 HP)
[NAIVE] Immortal_4 attacks Immortal_3! (50 HP)
[APLICACIÓN CONGELADA]
```

## Análisis Técnico del Deadlock

### Mecanismo del Deadlock NAIVE

```java

private void fightNaive(Immortal opponent) {
    synchronized(this) {          
        synchronized(opponent) {   
            
        }
    }
}
```

**Escenario de Deadlock:**
1. `Immortal_A` adquiere su propio lock
2. `Immortal_B` adquiere su propio lock
3. `Immortal_A` intenta adquirir el lock de `Immortal_B` (BLOQUEADO)
4. `Immortal_B` intenta adquirir el lock de `Immortal_A` (BLOQUEADO)
5. **DEADLOCK**: Ambos threads esperan indefinidamente

### Mecanismo de Prevención ORDERED

```java

private void fightOrdered(Immortal opponent) {
    Immortal first = this.getId() < opponent.getId() ? this : opponent;
    Immortal second = this.getId() < opponent.getId() ? opponent : this;
    
    synchronized(first) {    
        synchronized(second) { 
            
        }
    }
}
```

**Garantía Anti-Deadlock:**
- **Orden consistente**: Siempre se adquieren locks en orden ascendente de ID
- **Elimina ciclos**: Imposible crear dependencias circulares
- **Thread-safe**: Múltiples threads pueden ejecutar sin bloqueos mutuos

## Diagnóstico con Herramientas Java

### Thread Dump Obtenido

```bash
jstack [PID]
```

**Observaciones:**
- 8 ForkJoinPool workers ejecutando Virtual Threads
- Virtual Threads #56, #59, #62, #65, #67, #69, #70, #71 activos
- Deadlock interno a nivel de Virtual Thread (no visible en thread dump tradicional)

### Limitaciones de Diagnóstico en Virtual Threads

1. **jstack**: Muestra ForkJoinPool workers pero no el deadlock interno de Virtual Threads
2. **jcmd VM.find_deadlocks**: No detecta deadlocks de Virtual Threads
3. **Herramientas tradicionales**: Diseñadas para Platform Threads, no Virtual Threads

## Conclusiones 

### 1. **Orden de Locks es Crítico**
- NAIVE: Orden aleatorio → Deadlock inevitable
- ORDERED: Orden consistente → Deadlock imposible

### 2. **Virtual Threads y Deadlocks**
- Los deadlocks persisten en Virtual Threads
- Las herramientas de diagnóstico necesitan actualización
- La aplicación se congela igual que con Platform Threads

### 3. **Estrategias de Prevención**
- **Lock Ordering**: Adquirir locks en orden consistente
- **Timeout**: Usar `tryLock()` con timeout
- **Lock-free programming**: Estructuras de datos sin locks


