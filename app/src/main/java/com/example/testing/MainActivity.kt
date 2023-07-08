import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier
                        .size(width = 375.dp, height = 667.dp),
                ) {
                    LoginScreen()
                }
            }
        }
    }
}

var tk = ""// token
data class Book(
    val id: Int,
    val title: String,
    val imgUrl: String,
    val dateReleased: String,
    val pdfUrl: String
)

private fun fetchBooks(
    token: String,
    onSuccess: (List<Book>) -> Unit,
    onError: (String) -> Unit
) {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("https://3nt-demo-backend.azurewebsites.net/Access/Books")
        .header("Authorization", "Bearer $token")
        .get()
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            val errorMessage = "API call failed with error: ${e.message}"
            onError(errorMessage)
        }

        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                val json = response.body?.string()
                val books = parseBooks(json)
                val sortedByDate = books.sortedBy { it.dateReleased } // Sort books by date
                onSuccess(sortedByDate)
            } else {
                val errorMessage = "API call failed with response code: ${response.code}"
                onError(errorMessage)
            }
        }
    })
}

private fun parseBooks(json: String?): List<Book> {
    val books = mutableListOf<Book>()
    val jsonArray = JSONArray(json)

    for (i in 0 until jsonArray.length()) {
        val jsonObject = jsonArray.getJSONObject(i)
        val id = jsonObject.getInt("id")
        val title = jsonObject.getString("title")
        val imgUrl = jsonObject.getString("img_url")
        val dateReleased = jsonObject.getString("date_released")
        val pdfUrl = jsonObject.getString("pdf_url")

        val book = Book(id, title, imgUrl, dateReleased, pdfUrl)
        books.add(book)
    }

    return books
}

object AuthService {
    fun login(username: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val client = OkHttpClient()
        val requestBody = """
            {
                "UserName": "$username",
                "Password": "$password"
            }
        """.trimIndent()
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url("https://3nt-demo-backend.azurewebsites.net/Access/Login")
            .post(requestBody.toRequestBody(mediaType))
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val json = response.body?.string()
                    tk = parseToken(json)
                    onSuccess()
                } else {
                    val errorMessage = "API call failed with response code: ${response.code}"
                    onError(errorMessage)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                val errorMessage = "API call failed with error: ${e.message}"
                onError(errorMessage)
            }
        })
    }
    private fun parseToken(json: String?): String {
        // Parse the JSON response and extract the token
        val jsonObject = JSONObject(json)
        return jsonObject.getString("token")
    }
}



private fun validatePassword(password: String): Boolean {
    val regex = Regex("(?=.*[A-Z])(?=.*\\d)(?=.*[a-z])(?=.*[^A-Za-z0-9]).{8}")
    return regex.matches(password)
}

private fun validateUserID(userID: String): Boolean {
    val regex = Regex("^[A-Z]{2}[0-9]{4}$")
    // Unsure if it should be val regex = Regex("^(?=.*[A-Z])(?=.*[0-9])[A-Z]{2}[0-9]{4}$")
    // as to have the letters be in any order rather than the first two being capitals and the rest numbers
    return regex.matches(userID)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen() {
    var userID by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorUserID by remember { mutableStateOf("") }
    var errorPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoggedIn by remember { mutableStateOf(false) }
    var sortedBooks by remember { mutableStateOf(emptyList<Book>()) }

    val showErrorDialogState = remember { mutableStateOf(false) }
    val infoButtonClicked = remember { mutableStateOf(false)}
    val coroutineScope = rememberCoroutineScope()

    if (isLoggedIn){
        BottomTabBar()
        BookList(books = sortedBooks)
    } else {
        Column(modifier = Modifier.padding(16.dp)) {
            // UserID field
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = userID,
                    onValueChange = { value ->
                        userID = value
                        errorUserID = if (validateUserID(value)) "" else "Invalid UserID"
                    },
                    label = { Text(text = "UserID") },
                    isError = errorUserID.isNotEmpty(),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                // info icon
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    tint = Color.Gray,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { infoButtonClicked.value = true }
                )
            }
            if (errorUserID.isNotEmpty()) {
                Text(text = errorUserID, color = Color.Red)
            }

            // Password field
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = password,
                    onValueChange = { value ->
                        password = value
                        errorPassword = if (validatePassword(value)) "" else "Invalid Password"
                    },
                    label = { Text("Password") },
                    isError = errorPassword.isNotEmpty(),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        Text(
                            text = if (passwordVisible) "Hide" else "Show",
                            modifier = Modifier.clickable { passwordVisible = !passwordVisible }
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Password Info",
                    tint = Color.Gray,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { infoButtonClicked.value = true }
                )
            }
            if (errorPassword.isNotEmpty()) {
                Text(text = errorPassword, color = Color.Red)
            }

            // Login button
            Button(
                onClick = {
                    if (validateUserID(userID) && validatePassword(password)) {
                        coroutineScope.launch {
                            AuthService.login(
                                userID,
                                password,
                                onSuccess = {
                                    isLoggedIn = true
                                        // Use the token to fetch books
                                        fetchBooks(
                                        token = tk,
                                onSuccess = { fetchedBooks ->
                                    sortedBooks = fetchedBooks
                                            },
                                onError = { errorMessage ->
                                    // Handle error
                                }
                            )
                        },
                                onError = {
                                    showErrorDialogState.value = true
                                }
                            )
                        }
                    } else {
                        showErrorDialogState.value = true
                    }
                },
                enabled = userID.isNotEmpty() && password.isNotEmpty(),
            ) {
                Text("Login")
            }
        }
    }

    // TODO: Language Button
    if (showErrorDialogState.value) {
        ShowErrorDialog {
            showErrorDialogState.value = false
        }
    }

    //info button dialog
    if (infoButtonClicked.value) {
        DimOverlay { infoButtonClicked.value = false}
        ShowInfoDialog {infoButtonClicked.value = false}
    }
}

@Composable
fun BookList(books: List<Book>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(books.size) { index ->
            val book = books[index]
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(130.dp, 170.dp)
                    .background(Color.LightGray)
            ) {
                // Display the book content within the Box
                // You can customize the content based on your book data
                // For example, displaying the book title or image
                // Replace the placeholder content with your own book data
                Text(
                    text = book.title,
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// tab
@Composable
fun BottomTabBar() {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        TabIcon(Icons.Default.Home, "Home")
        Spacer(Modifier.width(16.dp))
        TabIcon(Icons.Default.Settings, "Settings")
        Spacer(Modifier.width(16.dp))
        TabIcon(Icons.Default.Person, "Profile")
        Spacer(Modifier.width(16.dp))
        TabIcon(Icons.Default.Info, "Info")
    }
}

@Composable
fun TabIcon(icon: ImageVector, contentDescription: String) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = Modifier
            .height(56.dp)
            .fillMaxHeight()
            .width(56.dp)
            .clickable { /* Handle tab click */ }
    )
}


@Composable
fun ShowErrorDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Error") },
        text = { Text("Validation error") },
        confirmButton = {
            Button(
                onClick = { onDismiss() }
            ) {
                Text("Back")
            }
        }
    )
}
@Composable
fun DimOverlay(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() }
    )
}

@Composable
fun ShowInfoDialog(onDismiss: () -> Unit) {
    // TODO: fix
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Info") },
        text = { Text("Some information about the login process.") },
        confirmButton = {
            Button(
                onClick = { onDismiss() }
            ) {
                Text("OK")
            }
        }
    )
}

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    MaterialTheme {
        content()
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MyApplicationTheme {
        LoginScreen()
    }
}