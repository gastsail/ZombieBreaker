import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

data class ValidationResult(
    val approved: Boolean,
    val reason: String,
    val accuracy: Int,
    val hint: String? = null,
    val understoodCode: String? = null
)

object OllamaService {

    private const val MODEL_NAME = "gemma4"
    private const val OLLAMA_URL = "http://localhost:11434/api/generate"

    suspend fun validateExplanation(
        fileContext: String,
        pastedCode: String,
        userExplanation: String,
        language: String
    ): ValidationResult = withContext(Dispatchers.IO) {
        val promptText = """
    Actúa como un compañero de equipo pragmático haciendo un Code Review rápido. Tu misión es asegurar que el usuario leyó y entendió el código que está pegando, sin ser molesto ni excesivamente estricto.

    REGLAS DE EVALUACIÓN (EL PUNTO DE EQUILIBRIO):
    1. **Proporcionalidad Justa**: Si el código es corto (ej. una función de 10 líneas), una explicación simple y directa es suficiente para el 100%. Si es un bloque grande, el usuario debe mencionar las responsabilidades principales, pero no exijas detalles de implementación.
    2. **Busca la Intención**: El usuario solo necesita demostrar que sabe QUÉ hace el fragmento y PARA QUÉ sirve. No lo penalices por no usar jerga técnica perfecta.
    3. **Contexto Suficiente**: Si lo que explica tiene sentido lógico para estar dentro del archivo actual ($fileContext), dale el visto bueno.
    4. **Escala de Precisión (Accuracy)**:
       - 0-40%: Explicación totalmente incorrecta, vacía, o un resumen inútil (ej. "código para la app", "hace un bucle").
       - 41-80%: Entiende una pequeña parte, pero la explicación es demasiado vaga para el tamaño del código, o se equivocó en el propósito principal.
       - 81-100%: Explicación razonable y al grano. Un desarrollador normal diría "ok, sabes lo que estás pegando".

    REGLAS DE SALIDA:
    - SI ACCURACY > 80: Status "APROBADO" y understood_code es null.
    - SI ACCURACY <= 80: Status "RECHAZADO". En "understood_code" pon solo las líneas exactas que el usuario SÍ logró explicar (si aplica). Si la explicación fue totalmente inútil, devuelve null.

    CONTEXTO DEL ARCHIVO: 
    $fileContext
    
    CÓDIGO A PEGAR: 
    $pastedCode
    
    EXPLICACIÓN DEL USUARIO: 
    "$userExplanation"
    
    IMPORTANT: You MUST write the "reason" and "hint" fields in $language language.
    
    Responde ÚNICAMENTE en formato JSON:
    {
      "status": "APROBADO" o "RECHAZADO",
      "accuracy": <número entero>,
      "reason": "Explicación breve y amigable de tu decisión",
      "hint": "Una pista rápida de qué le faltó mencionar para aprobar (solo si fue rechazado)",
      "understood_code": "Líneas de código validadas (o null)"
    }
""".trimIndent()

        val requestJson = JsonObject().apply {
            addProperty("model", MODEL_NAME)
            addProperty("prompt", promptText)
            addProperty("stream", false)
            addProperty("format", "json")
        }

        val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build()
        val request = HttpRequest.newBuilder()
            .uri(URI.create(OLLAMA_URL))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestJson.toString()))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        val jsonResponse = JsonParser.parseString(response.body()).asJsonObject
        val rawContent = jsonResponse.get("response").asString

        val cleanJsonContent = rawContent.replace(Regex("```(json)?"), "").trim()
        val innerJson = JsonParser.parseString(cleanJsonContent).asJsonObject

        val accuracy =
            if (innerJson.has("accuracy") && !innerJson.get("accuracy").isJsonNull) innerJson.get("accuracy").asInt else 0

        val isApproved = accuracy > 80

        val hint =
            if (innerJson.has("hint") && !innerJson.get("hint").isJsonNull) innerJson.get("hint").asString else null

        var understoodCode: String? = null
        if (innerJson.has("understood_code") && !innerJson.get("understood_code").isJsonNull) {
            val rawCode = innerJson.get("understood_code").asString
            understoodCode = rawCode.replace("```[a-zA-Z]*".toRegex(), "").replace("```", "").trim()
            // Si el LLM devuelve un string vacío, lo tratamos como null para no mostrar la caja vacía en la UI
            if (understoodCode.isEmpty()) understoodCode = null
        }

        return@withContext ValidationResult(
            isApproved,
            innerJson.get("reason").asString,
            accuracy,
            hint,
            understoodCode
        )
    }
}