package com.vs18.crossparser

import android.Manifest
import android.annotation.SuppressLint
import android.os.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.vs18.crossparser.ui.theme.CrossParserTheme
import kotlinx.coroutines.*
import java.io.File

data class ParseResult(
    val totalFiles: Int = 0,
    val totalSize: Long = 0,
    val byType: Map<String, Int> = emptyMap()
)

class MainActivity : ComponentActivity() {

    private val storagePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        storagePermissionLauncher.launch(
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        )

        setContent {
            CrossParserTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF1e1e1e)
                ) {
                    ParserScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParserScreen() {

    var selectedPath by remember { mutableStateOf<File?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    var hasResult by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf(ParseResult()) }

    val scope = rememberCoroutineScope()

    val defaultDirs = remember {
        listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            File(Environment.getExternalStorageDirectory(), "Android/data")
        ).filter { it.exists() }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "🧹 CrossParser",
                        color = Color(0xFF00B7EB),
                        fontSize = 24.sp
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF252526)
                )
            )
        },
        containerColor = Color(0xFF1e1e1e)
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text(
                "Оберіть папку для парсингу",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(defaultDirs) { dir ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedPath == dir,
                            onClick = {
                                selectedPath = dir
                                hasResult = false
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF43a047)
                            )
                        )

                        Text(
                            text = "📂 ${dir.name} (${dir.absolutePath})",
                            color = Color(0xFFd4d4d4),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (selectedPath == null) return@Button

                    isScanning = true
                    hasResult = false
                    result = ParseResult()

                    scope.launch {
                        val newResult = withContext(Dispatchers.IO) {
                            parseDirectory(selectedPath!!)
                        }

                        result = newResult
                        hasResult = true
                        isScanning = false
                    }
                },
                enabled = selectedPath != null && !isScanning,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF43a047)
                )
            ) {
               Text(
                   if (isScanning) "🔄 Сканування..." else "🔍 Парсинг папки",
                   fontSize = 18.sp
               )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (hasResult) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF252526)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {

                        Text(
                            "Результати парсингу",
                            color = Color(0xFF00b7eb),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Файлів: ${result.totalFiles}", color = Color.White)
                        Text(
                            "Загальний розмір: ${formatSize(result.totalSize)}",
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            "За типами:",
                            color = Color(0xFFaaaaaa),
                            fontWeight = FontWeight.Bold
                        )

                        result.byType.forEach { (type, count) ->
                            Text("• $type: $count", color = Color(0xFFd4d4d4))
                        }
                    }
                }
            }
        }
    }
}

fun parseDirectory(root: File): ParseResult {
    val byType = mutableMapOf<String, Int>()
    var totalSize = 0L
    var totalFiles = 0

    val ignoreDirs = setOf(".git", "__pycache__", "node_modules", ".gradle", "build")

    @SuppressLint("NewApi")
    fun recurse(dir: File) {
        if (!dir.isDirectory || ignoreDirs.contains(dir.name)) return

        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                recurse(file)
            } else {
                totalFiles++
                totalSize += file.length()

                val category = when (file.extension.lowercase()) {
                    "jpg", "jpeg", "png", "gif", "webp", "svg" -> "Зображення"
                    "mp4", "avi", "mkv", "mov" -> "Відео"
                    "mp3", "wav", "flac" -> "Аудіо"
                    "pdf", "doc", "docx", "txt" -> "Документи"
                    "java", "kt", "py", "js", "html", "css" -> "Код"
                    else -> "Інше"
                }

                byType[category] = byType.getOrDefault(category, 0) + 1
            }
        }
    }

    recurse(root)
    return ParseResult(totalFiles, totalSize, byType)
}

fun formatSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0

    return when {
        gb >= 1 -> "%.2f GB".format(gb)
        mb >= 1 -> "%.2f MB".format(mb)
        kb >= 1 -> "%.2f KB".format(kb)
        else -> "$bytes B"
    }
}