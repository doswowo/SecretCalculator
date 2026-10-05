package com.secretcalc.browser

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.math.BigDecimal
import java.math.RoundingMode

class CalculatorActivity : AppCompatActivity() {

    private lateinit var display: TextView
    private var currentInput = StringBuilder()
    private var currentOperator: String? = null
    private var firstOperand: Double? = null
    private var lastResult: Double? = null
    private var justCalculated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculator)
        display = findViewById(R.id.tvDisplay)
        updateDisplay()
    }

    fun onDigitClick(view: View) {
        val digit = (view as Button).text.toString()
        if (justCalculated) {
            currentInput.clear()
            firstOperand = null
            currentOperator = null
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
        justCalculated = false

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
                calculate()
            }
        }

        currentOperator = op
        currentInput.clear()
        updateDisplay()
    }

    fun onEqualsClick(view: View) {
        if (currentOperator != null) {
            calculate()
            currentOperator = null
            justCalculated = true
            return
        }
        tryUnlock()
    }

    private fun calculate() {
        if (firstOperand == null || currentOperator == null) return
        if (currentInput.isEmpty()) return
        val secondOperand = currentInput.toString().toDoubleOrNull() ?: return
        val result = when (currentOperator) {
            "+" -> firstOperand!! + secondOperand
            "-" -> firstOperand!! - secondOperand
            "×" -> firstOperand!! * secondOperand
            "÷" -> if (secondOperand != 0.0) firstOperand!! / secondOperand else Double.NaN
            else -> return
        }
        lastResult = result
        firstOperand = result
        currentInput.clear()
        currentInput.append(formatResult(result))
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
    }

    fun onPercentClick(view: View) {
        justCalculated = false
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
        justCalculated = false
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

    private fun updateDisplay() {
        display.text = when {
            currentInput.isNotEmpty() -> currentInput.toString()
            firstOperand != null -> formatResult(firstOperand!!)
            else -> "0"
        }
    }
}
