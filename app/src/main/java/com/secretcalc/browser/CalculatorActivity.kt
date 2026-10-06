package com.secretcalc.browser

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.math.BigDecimal
import java.math.RoundingMode

class CalculatorActivity : AppCompatActivity() {

    private lateinit var display: TextView
    private lateinit var expressionView: TextView
    private lateinit var historyList: LinearLayout

    private var currentInput = StringBuilder()
    private var currentOperator: String? = null
    private var firstOperand: Double? = null
    private var lastResult: Double? = null
    private var justCalculated = false
    private var lastFormula = ""
    private val history = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        BrowserActivity.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculator)
        display = findViewById(R.id.tvDisplay)
        expressionView = findViewById(R.id.tvExpression)
        historyList = findViewById(R.id.historyList)
        loadHistory()
        renderHistory()
        updateDisplay()
    }

    fun onDigitClick(view: View) {
        val digit = (view as Button).text.toString()
        if (justCalculated) {
            currentInput.clear()
            firstOperand = null
            currentOperator = null
            lastFormula = ""
            justCalculated = false
        }
        if (digit == "." && currentInput.contains(".")) return
        if (currentInput.length >= 15) return
        currentInput.append(digit)
        updateDisplay()
        tryUnlock()
    }

    fun onOperatorClick(view: View) {
        val op = (view as Button).text.toString()
        if (justCalculated) {
            firstOperand = lastResult
            currentInput.clear()
            lastFormula = ""
            justCalculated = false
        }

        if (currentInput.isEmpty() && firstOperand == null) {
            if (lastResult != null) {
                firstOperand = lastResult
            } else {
                return
            }
        }

        if (currentInput.isNotEmpty()) {
            if (firstOperand == null) {
                firstOperand = currentInput.toString().toDoubleOrNull() ?: return
            } else if (currentOperator != null) {
                calculate(false)
            }
        }

        currentOperator = op
        currentInput.clear()
        updateDisplay()
    }

    fun onEqualsClick(view: View) {
        if (currentOperator != null) {
            calculate(true)
            currentOperator = null
            justCalculated = true
            updateDisplay()
            return
        }
        tryUnlock()
    }

    private fun calculate(recordHistory: Boolean) {
        if (firstOperand == null || currentOperator == null) return
        if (currentInput.isEmpty()) return
        val secondOperand = currentInput.toString().toDoubleOrNull() ?: return
        val formula = "${formatResult(firstOperand!!)}$currentOperator${formatResult(secondOperand)}"
        val result = when (currentOperator) {
            "+" -> firstOperand!! + secondOperand
            "-" -> firstOperand!! - secondOperand
            "×" -> firstOperand!! * secondOperand
            "÷" -> if (secondOperand != 0.0) firstOperand!! / secondOperand else Double.NaN
            else -> return
        }
        lastResult = result
        firstOperand = result
        lastFormula = formula
        currentInput.clear()
        currentInput.append(formatResult(result))
        if (recordHistory) {
            addHistory("$formula = ${formatResult(result)}")
        }
        updateDisplay()
    }

    private fun tryUnlock() {
        if (currentOperator != null) return
        val input = currentInput.toString()
        if (!PinStore.isValid(input)) return
        if (input != PinStore.get(this)) return
        resetState()
        updateDisplay()
        startActivity(Intent(this, BrowserActivity::class.java))
    }

    private fun formatResult(value: Double): String {
        if (value.isNaN()) return "错误"
        if (value.isInfinite()) return "错误"
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            BigDecimal(value.toString()).setScale(8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
        }
    }

    fun onClearClick(view: View) {
        resetState()
        updateDisplay()
    }

    private fun resetState() {
        currentInput.clear()
        currentOperator = null
        firstOperand = null
        lastResult = null
        justCalculated = false
        lastFormula = ""
    }

    fun onPercentClick(view: View) {
        if (justCalculated) {
            justCalculated = false
            lastFormula = ""
        }
        if (currentInput.isEmpty() && firstOperand != null) {
            val value = firstOperand!! / 100
            firstOperand = value
            currentInput.clear()
            currentInput.append(formatResult(value))
            updateDisplay()
        } else if (currentInput.isNotEmpty()) {
            val value = currentInput.toString().toDoubleOrNull() ?: return
            currentInput.clear()
            currentInput.append(formatResult(value / 100))
            updateDisplay()
        }
    }

    fun onNegateClick(view: View) {
        if (justCalculated) {
            justCalculated = false
            lastFormula = ""
        }
        if (currentInput.isEmpty() && firstOperand != null) {
            firstOperand = -firstOperand!!
            currentInput.clear()
            currentInput.append(formatResult(firstOperand!!))
            updateDisplay()
        } else if (currentInput.isNotEmpty()) {
            val value = currentInput.toString().toDoubleOrNull() ?: return
            currentInput.clear()
            currentInput.append(formatResult(-value))
            updateDisplay()
        }
    }

    private fun currentExpression(): String {
        if (justCalculated) return lastFormula
        val first = firstOperand?.let { formatResult(it) } ?: ""
        val op = currentOperator ?: ""
        val second = currentInput.toString()
        return when {
            op.isNotEmpty() -> first + op + second
            second.isNotEmpty() -> second
            first.isNotEmpty() -> first
            else -> ""
        }
    }

    private fun updateDisplay() {
        expressionView.text = currentExpression()
        display.text = when {
            justCalculated && lastResult != null -> formatResult(lastResult!!)
            currentInput.isNotEmpty() -> currentInput.toString()
            firstOperand != null -> formatResult(firstOperand!!)
            else -> "0"
        }
    }

    private fun addHistory(line: String) {
        history.add(0, line)
        while (history.size > 5) history.removeAt(history.lastIndex)
        getSharedPreferences("calc", MODE_PRIVATE)
            .edit()
            .putString("history", history.joinToString("\n"))
            .apply()
        renderHistory()
    }

    private fun loadHistory() {
        history.clear()
        val saved = getSharedPreferences("calc", MODE_PRIVATE).getString("history", "") ?: ""
        if (saved.isNotBlank()) {
            history.addAll(saved.split("\n").filter { it.isNotBlank() }.take(5))
        }
    }

    private fun renderHistory() {
        historyList.removeAllViews()
        if (history.isEmpty()) {
            historyList.addView(historyRow("暂无记录", Color.parseColor("#8E8E93")))
            return
        }
        for (line in history) {
            historyList.addView(historyRow(line, Color.parseColor("#FFFFFF")))
        }
    }

    private fun historyRow(text: String, color: Int): TextView {
        return TextView(this).apply {
            this.text = text
            setTextColor(color)
            textSize = 15f
            gravity = Gravity.END
            setPadding(0, 6, 0, 6)
            maxLines = 1
        }
    }
}
