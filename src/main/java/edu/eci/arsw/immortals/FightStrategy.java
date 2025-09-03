package edu.eci.arsw.immortals;

/*
 * ENUM PARA ESTRATEGIAS DE PELEA ENTRE INMORTALES
 *    - NAIVE: Sin orden, puede causar deadlocks
 *    - ORDERED: Orden alfabético por nombre, evita deadlocks
 */
public enum FightStrategy {
    NAIVE,    
    ORDERED   
}
