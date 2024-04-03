import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.weather.R
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.InternalAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.*


class WeatherFragment : Fragment() {
    private lateinit var outputText: TextView
    private val GEOCODE_API_KEY = "enter_Api_key"
    private val WEATHER_API_KEY = "enter_api_key"
    private var currentCity = "Minsk"
    private lateinit var searchCityButton: ImageButton
    private lateinit var cityNameView: TextView;


    private var wind_speed: Double = 0.0
    private var temperature: Double = 0.0
    private var humidity: Int = 0
    private var pressure: Int = 0

    private val client: HttpClient = HttpClient(Android) {
        install(DefaultRequest) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_weather, container, false)
        outputText = view.findViewById(R.id.textView)
        searchCityButton = view.findViewById(R.id.imageButton)
        cityNameView = view.findViewById(R.id.textView2)
        searchCityButton.setOnClickListener {
            showSearchDialog()
        }
        CoroutineScope(Dispatchers.IO).launch {
            updateWeather()
        }
        return view
    }

    private fun showSearchDialog() {
        val dialogBuilder = AlertDialog.Builder(context)
        val view = layoutInflater.inflate(R.layout.dialog_layout, null)

        val cityNameEdit = view.findViewById<EditText>(R.id.editTextText)
        cityNameEdit.setText(currentCity)

        dialogBuilder.setView(view)
        dialogBuilder.setTitle("Enter City Name")

        val dialog = dialogBuilder.create()

        val saveButton = view.findViewById<Button>(R.id.button)
        saveButton.text = "Save"
        saveButton.setOnClickListener {
            if (cityNameEdit.text.isNotEmpty()) {
                currentCity = cityNameEdit.text.toString()
                cityNameView.text = currentCity
                CoroutineScope(Dispatchers.IO).launch {
                    updateWeather()
                }
                dialog.dismiss()
            } else {
                Toast.makeText(context, "Please enter a city name", Toast.LENGTH_SHORT).show()
            }
        }

        val cancelButton = view.findViewById<Button>(R.id.button2)
        cancelButton.text = "Cancel"
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private suspend fun updateWeather() {
        var latitude: String = "0"
        var longitude: String = "0"
        cityNameView.text = currentCity
        try {
            val response1: HttpResponse =
                client.get("https://geocode.maps.co/search?q=$currentCity&api_key=$GEOCODE_API_KEY") {
                    contentType(ContentType.Application.FormUrlEncoded)
                }
            val responseBody1 = response1.bodyAsText()
            withContext(Dispatchers.IO) {
                val jsonArray = JSONArray(responseBody1)
                val firstLocation = jsonArray.getJSONObject(0)
                latitude = firstLocation.getString("lat")
                longitude = firstLocation.getString("lon")
            }
            val response2: HttpResponse =
                client.get("https://api.openweathermap.org/data/2.5/forecast?lat=${latitude}&lon=${longitude}&appid=${WEATHER_API_KEY}") {
                    contentType(ContentType.Application.FormUrlEncoded)
                }
            val responseBody2 = response2.bodyAsText()
            withContext(Dispatchers.IO) {
                val jsonObject = JSONObject(responseBody2)
                val parametersList = jsonObject.getJSONArray("list")
                val res = parametersList.getJSONObject(0)
                wind_speed = res.getJSONObject("wind").getDouble("speed")
                humidity = res.getJSONObject("main").getInt("humidity")
                temperature = res.getJSONObject("main").getDouble("temp") - 273.15
                pressure = res.getJSONObject("main").getInt("pressure")
                withContext(Dispatchers.Main) {
                    outputText.text =
                        "Wind speed: ${wind_speed} m/s\nHumidity: ${humidity}%\nTemperature: ${
                            Math.round(
                                temperature * 100
                            ) / 100
                        } °C\nPressure: ${pressure} hPa"
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }



    //implement getting from local memory city name(room)
    /*
        override fun onSaveInstanceState(outState: Bundle) {
            super.onSaveInstanceState(outState)
            outState.putString("cityName", "Minsk")
        }

        override fun onViewStateRestored(savedInstanceState: Bundle?) {
            super.onViewStateRestored(savedInstanceState)
            currentCity = savedInstanceState?.getString("cityName") ?: "Minsk";
        }*/
    override fun onDestroy() {
        super.onDestroy()
        client.close()
    }
}