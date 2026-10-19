package com.example.editor

import com.example.model.ScoreSymbol

class UndoRedoManager(private val maxHistory: Int = 40) {

    private val undoStack = mutableListOf<List<ScoreSymbol>>()
    private val redoStack = mutableListOf<List<ScoreSymbol>>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun recordState(currentSymbols: List<ScoreSymbol>) {
        undoStack.add(currentSymbols.map { it.copy() })
        if (undoStack.size > maxHistory) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
    }

    fun undo(currentSymbols: List<ScoreSymbol>): List<ScoreSymbol>? {
        if (!canUndo) return null
        val previous = undoStack.removeAt(undoStack.size - 1)
        redoStack.add(currentSymbols.map { it.copy() })
        return previous
    }

    fun redo(currentSymbols: List<ScoreSymbol>): List<ScoreSymbol>? {
        if (!canRedo) return null
        val next = redoStack.removeAt(redoStack.size - 1)
        undoStack.add(currentSymbols.map { it.copy() })
        return next
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
