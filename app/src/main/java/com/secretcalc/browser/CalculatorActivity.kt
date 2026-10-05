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
    private var justCalculated = false  // 刚计算完，等待输入密码

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculator)
        display = findViewById(R.id.tvDisplay)
    }

    fun onDigitClick(view: View) {
        val btn = view as Button
        val digit = btn.text.toString()

        // 如果刚计算完且用户输入数字，清空输入开始输入密码
        if (justCalculated) {
            currentInput.clear()
            justCalculated = false
        }

        if (digit == "." && currentInput.contains(".")) return
        if (currentInput.length > 15) return

        currentInput.append(digit)
        updateDisplay()
    }

    fun onOperatorClick(view: View) {
        val btn = view as Button
        val op = btn.text.toString()

        if (currentInput.isEmpty() && firstOperand == null) {
            if (lastResult != null) {
                firstOperand = lastResult
                currentInput.clear()
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
        justCalculated = false
        currentInput.clear()
        updateDisplay()
    }

    fun onEqualsClick(view: View) {
        if (currentOperator == null) {
            // 没有运算符，检查是否是密码输入
            if (currentInput.isNotEmpty() && justCalculated) {
                // 用户刚算完结果，现在输入数字后按等号
                checkSecretCode()
                return
            } else if (currentInput.isEmpty() && lastResult != null) {
                // 用户按等号但没有输入，检查是否是直接输入密码后按等号
                // 这种情况是用户直接输入数字后按等号
                return
            }
            return
        }

        calculate()
        currentOperator = null
        justCalculated = true  // 标记：刚计算完，等待输入密码
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

    private fun checkSecretCode() {
        val input = currentInput.toString().toDoubleOrNull() ?: return
        val storedCode = getSharedPreferences("secret", MODE_PRIVATE).getInt("code", 123456)

        if (input.toInt() == storedCode) {
            // 密码正确，启动浏览器
            lastResult = null
            currentInput.clear()
            currentOperator = null
            firstOperand = null
            justCalculated = false
            updateDisplay()
            startActivity(Intent(this, BrowserActivity::class.java))
        } else {
            // 密码错误，清空
            clearAll(view)
        }
    }

    private fun formatResult(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            BigDecimal(value).setScale(8, RoundingMode.HALF_UP).stripTrailingZeros().toString()
        }
    }

    fun onClearClick(view: View) {
        clearAll(view)
    }

    private fun clearAll(view: View) {
        currentInput.clear()
        currentOperator = null
        firstOperand = null
        lastResult = null
        justCalculated = false
        updateDisplay()
    }

    fun onPercentClick(view: View) {
        if (currentInput.isEmpty() && firstOperand != null) {
            val value = firstOperand!! / 100
            firstOperand = value
            currentInput.clear()
            currentInput.append(formatResult(value))
            updateDisplay()
        } else if (currentInput.isNotEmpty()) {
            val value = currentInput.toString().toDoubleOrNull() ?: return
            val result = value / 100
            currentInput.clear()
            currentInput.append(formatResult(result))
            updateDisplay()
        }
    }

    fun onNegateClick(view: View) {
        if (currentInput.isEmpty() && firstOperand != null) {
            firstOperand = -firstOperand!!
            currentInput.clear()
            currentInput.append(formatResult(firstOperand!!))
            updateDisplay()
        } else if (currentInput.isNotEmpty()) {
            val value = currentInput.toString().toDoubleOrNull() ?: return
            val result = -value
            currentInput.clear()
            currentInput.append(formatResult(result))
            updateDisplay()
        }
    }

    private fun updateDisplay() {
        val text = when {
            currentInput.isNotEmpty() -> currentInput.toString()
            firstOperand != null -> formatResult(firstOperand!!)
            else -> "0"
        }
        display.text = text
    }
}
