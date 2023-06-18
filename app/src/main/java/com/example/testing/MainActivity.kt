import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    LoginScreen()
                }
            }
        }
    }
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

    val showErrorDialogState = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(16.dp)) {
        // UserID field
        TextField(
            value = userID,
            onValueChange = { value ->
                userID = value
                errorUserID = if (validateUserID(value)) "" else "Invalid UserID"
            },
            label = { Text("UserID") },
            isError = errorUserID.isNotEmpty(),
            singleLine = true
        )
        if (errorUserID.isNotEmpty()) {
            Text(text = errorUserID, color = Color.Red)
        }

        // Password field
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
            }
        )
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
                                // implement
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
            enabled = userID.isNotEmpty() && password.isNotEmpty()
        ) {
            Text("Login")
        }
    }

    // TODO
    // Language Button
        // make a drop down button
        // it should have the ability for english and greek
        // if it is in greek change the words and vice versa
    if (showErrorDialogState.value) {
        ShowErrorDialog {
            showErrorDialogState.value = false
        }
    }

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
