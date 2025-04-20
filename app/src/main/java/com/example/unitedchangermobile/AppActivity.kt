package com.example.unitedchangermobile

import android.os.Bundle
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.widget.*
import com.github.mikephil.charting.charts.CandleStickChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.CandleData
import com.github.mikephil.charting.data.CandleDataSet
import com.github.mikephil.charting.data.CandleEntry
import android.view.View
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import java.math.BigDecimal
import java.math.RoundingMode
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity


class AppActivity : AppCompatActivity() {
    private lateinit var convertOne: TextView
    private lateinit var convertOneLower: TextView
    private lateinit var convertTwo: TextView
    private lateinit var spinnerFrom: Spinner
    private lateinit var spinnerTo: Spinner
    private var currentAmount = "1"
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateRunnable: Runnable
    private val updateInterval = 1000L
    private lateinit var numberInput: EditText
    private lateinit var convertResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app)

        convertOne = findViewById(R.id.ConvertOne)
        convertOneLower = findViewById(R.id.convertOne)
        convertTwo = findViewById(R.id.ConvertTwo)
        spinnerFrom = findViewById(R.id.spinnerFrom)
        spinnerTo = findViewById(R.id.spinnerTo)

        setCandleStickChart()

        updateRunnable = object : Runnable {
            override fun run() {
                val fromCurrency = spinnerFrom.selectedItem.toString()
                val toCurrency = spinnerTo.selectedItem.toString()
                fetchExchangeRate(fromCurrency, toCurrency)
                handler.postDelayed(this, updateInterval)
            }
        }

        numberInput = findViewById(R.id.numberInput)
        convertResult = findViewById(R.id.ConvertResult)

        convertResult.text = "0.000 ${spinnerTo.selectedItem}"

        numberInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val input = s.toString()
                val toCurrency = spinnerTo.selectedItem.toString()
                val fromCurrency = spinnerFrom.selectedItem.toString()

                if (input.isEmpty()) {
                    currentAmount = "0"
                    convertResult.text = "0 $toCurrency"
                    return
                }

                try {
                    val number = input.toDouble()

                    if (number > 9999) {
                        numberInput.setText("9999")
                        numberInput.setSelection(numberInput.text.length)
                        return
                    }

                    currentAmount = input
                    fetchExchangeRate(fromCurrency, toCurrency)

                } catch (_: NumberFormatException) {
                    convertResult.text = "0.000 $toCurrency"
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        updateConvertOneLabel()
    }

    private fun updateConvertOneLabel() {
        val fromCurrency = spinnerFrom.selectedItem.toString()
        convertOne.setText("1 $fromCurrency")
        convertOneLower.setText(fromCurrency)
    }

    override fun onResume() {
        super.onResume()
        handler.post(updateRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateRunnable)
    }

    fun setCandleStickChart() {
        val candlechart: CandleStickChart = findViewById(R.id.candlechart)
        setupSpinners()
    }

    private fun setupSpinners() {
        val currencies = listOf("USD", "EUR", "GBP", "JPY", "CNY", "PLN", "UAH", "AED", "BYN", "TRY", "RUB", "CAD")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spinnerFrom.adapter = adapter
        spinnerTo.adapter = adapter

        spinnerFrom.setSelection(currencies.indexOf("USD"))
        spinnerTo.setSelection(currencies.indexOf("RUB"))

        val flagFrom: ImageView = findViewById(R.id.flagFrom)
        val flagTo: ImageView = findViewById(R.id.flagTo)

        updateFlagImage(flagFrom, "USD")
        updateFlagImage(flagTo, "RUB")

        fetchExchangeRate("USD", "RUB")

        spinnerFrom.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val fromCurrency = parent?.getItemAtPosition(position).toString()
                val toCurrency = spinnerTo.selectedItem.toString()
                updateConvertOneLabel()
                updateFlagImage(flagFrom, fromCurrency)
                fetchExchangeRate(fromCurrency, toCurrency)
                fetchOHLCData(fromCurrency, toCurrency)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerTo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val fromCurrency = spinnerFrom.selectedItem.toString()
                val toCurrency = parent?.getItemAtPosition(position).toString()
                updateFlagImage(flagTo, toCurrency)
                fetchExchangeRate(fromCurrency, toCurrency)
                fetchOHLCData(fromCurrency, toCurrency)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun fetchExchangeRate(fromCurrency: String, toCurrency: String) {
        val url = "https://api.coinbase.com/v2/prices/$fromCurrency-$toCurrency/spot"
        val queue = Volley.newRequestQueue(this)

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    val data = response.getJSONObject("data")
                    val rate = data.getString("amount")
                    val roundedRate = BigDecimal(rate).setScale(3, RoundingMode.HALF_UP).toString()

                    convertTwo.text = "$roundedRate $toCurrency"

                    val amount = currentAmount.toDoubleOrNull() ?: 0.0
                    val result = BigDecimal(amount * roundedRate.toDouble())
                        .setScale(3, RoundingMode.HALF_UP)
                        .toString()

                    if (numberInput.text.toString() != "") {
                        convertResult.text = "$result $toCurrency"
                    }

                } catch (e: Exception) {
                    Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Error fetching rate: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )

        queue.add(jsonObjectRequest)
        updateConvertOneLabel()
    }

    private fun updateFlagImage(imageView: ImageView, currencyCode: String) {
        val resourceName = when (currencyCode) {
            "TRY" -> "turk"
            else -> currencyCode.toLowerCase()
        }

        val resourceId = resources.getIdentifier(resourceName, "drawable", packageName)
        if (resourceId != 0) {
            imageView.setImageResource(resourceId)
        }
        updateConvertOneLabel()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchOHLCData(fromCurrency: String, toCurrency: String) {
        val apiKey = "YOUR_API_KEY"

        val endDate = java.time.LocalDate.now()
        val startDate = endDate.minusDays(30)
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val startDateStr = startDate.format(formatter)
        val endDateStr = endDate.format(formatter)

        val currencyPair = "$fromCurrency$toCurrency"
        val url = "https://marketdata.tradermade.com/api/v1/timeseries?currency=$currencyPair&api_key=$apiKey&start_date=$startDateStr&end_date=$endDateStr&format=records&interval=daily"

        val queue = Volley.newRequestQueue(this)

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    val quotesArray = response.getJSONArray("quotes")

                    val entries = ArrayList<CandleEntry>()
                    val dates = ArrayList<String>()

                    var index = 0f
                    for (i in 0 until quotesArray.length()) {
                        val quote = quotesArray.getJSONObject(i)

                        if (quote.isNull("open") || quote.isNull("high") || quote.isNull("low") || quote.isNull("close")) continue

                        val open = quote.getDouble("open").toFloat()
                        val high = quote.getDouble("high").toFloat()
                        val low = quote.getDouble("low").toFloat()
                        val close = quote.getDouble("close").toFloat()
                        val date = quote.getString("date")

                        entries.add(CandleEntry(index.toInt(), high, low, open, close))
                        dates.add(date)
                        index += 1
                    }

                    updateCandleChart(entries, dates, fromCurrency, toCurrency)

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error parsing OHLC response", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Error fetching OHLC data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )

        queue.add(jsonObjectRequest)
        updateConvertOneLabel()
    }

    private fun updateCandleChart(entries: ArrayList<CandleEntry>, dates: ArrayList<String>, fromCurrency: String, toCurrency: String) {
        val candlechart: CandleStickChart = findViewById(R.id.candlechart)

        val candleDataSet = CandleDataSet(entries, "$fromCurrency -> $toCurrency")
        candleDataSet.color = Color.rgb(80, 80, 80)
        candleDataSet.shadowColor = Color.rgb(0, 196, 122)
        candleDataSet.shadowWidth = 1f
        candleDataSet.decreasingColor = Color.rgb(231, 49, 6)
        candleDataSet.decreasingPaintStyle = Paint.Style.FILL
        candleDataSet.increasingColor = Color.rgb(0, 196, 122)
        candleDataSet.increasingPaintStyle = Paint.Style.FILL

        val candleData = CandleData(dates, candleDataSet)
        candlechart.data = candleData
        candlechart.setBackgroundColor(Color.rgb(223, 217, 217))
        candlechart.animateXY(3000, 3000)

        val xAxis = candlechart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM



        candlechart.axisLeft.setDrawLabels(true)

        candlechart.invalidate()
        updateConvertOneLabel()
    }
}
