package it.polito.extgol;

/**
 * Defines the type of a cell, which may influence its behavior and interactions.
 */
public enum CellType {
    BASIC,      // Default cell type with standard behavior
    HIGHLANDER, // Special cell type, e.g. higher life points or different rules
    LONER,      // Prefers solitude, less interactions
    SOCIAL      // Prefers interactions and exchanges
}
