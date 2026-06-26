package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.WorkspaceViewModel

data class CodeSnippet(
    val title: String,
    val category: String,
    val code: String,
    val description: String
)

data class ColabTemplate(
    val title: String,
    val url: String,
    val description: String,
    val category: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    viewModel: WorkspaceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Templates, 1: ML Cheatsheet

    val templates = listOf(
        ColabTemplate(
            title = "Introduction to Google Colab",
            url = "https://colab.research.google.com/notebooks/intro.ipynb",
            description = "Learn markdown cell basics, python execution, and runtime setup within the Google workspace.",
            category = "Guides",
            icon = Icons.Default.Info
        ),
        ColabTemplate(
            title = "PyTorch Training Walkthrough",
            url = "https://colab.research.google.com/github/pytorch/tutorials/blob/gh-pages/_downloads/623da16a3a78912e96d7448375e8efca/tensor_tutorial.ipynb",
            description = "Train a basic image recognition network utilizing PyTorch Tensors and Autograd on GPU acceleration.",
            category = "Deep Learning",
            icon = Icons.Default.PlayCircleFilled
        ),
        ColabTemplate(
            title = "TensorFlow Beginner Quickstart",
            url = "https://colab.research.google.com/github/tensorflow/docs/blob/master/site/en/tutorials/quickstart/beginner.ipynb",
            description = "Import MNIST handwritten digits dataset, compile a neural network using Keras, and evaluate model loss.",
            category = "Deep Learning",
            icon = Icons.Default.Science
        ),
        ColabTemplate(
            title = "Hugging Face Transformers Intro",
            url = "https://colab.research.google.com/github/huggingface/notebooks/blob/main/transformers_doc/en/pytorch/quicktour.ipynb",
            description = "Load a pre-trained BERT or LLM tokenizer and pipeline for text classification and sentiment parsing.",
            category = "NLP & LLMs",
            icon = Icons.Default.AutoAwesome
        ),
        ColabTemplate(
            title = "Interactive Data Charts",
            url = "https://colab.research.google.com/notebooks/charts.ipynb",
            description = "Render beautiful plots using matplotlib, seaborn, and interactive altair maps within Colab cells.",
            category = "Data Science",
            icon = Icons.Default.BarChart
        )
    )

    val snippets = listOf(
        CodeSnippet(
            title = "Mount Google Drive in Notebook",
            category = "System & Drive",
            code = """from google.colab import drive
drive.mount('/content/drive')""",
            description = "Integrate your Google Drive files directly into the active Colab virtual machine instance storage directory."
        ),
        CodeSnippet(
            title = "Check GPU Cuda Availability",
            category = "Hardware & Drivers",
            code = """import torch
print("CUDA Active:", torch.cuda.is_available())
if torch.cuda.is_available():
    print("Active Device ID:", torch.cuda.current_device())
    print("Device Name:", torch.cuda.get_device_name(0))""",
            description = "Query active PyTorch CUDA runtime drivers to confirm hardware GPU training acceleration works."
        ),
        CodeSnippet(
            title = "TensorFlow GPU Hardware Status",
            category = "Hardware & Drivers",
            code = """import tensorflow as tf
print("TF Version:", tf.__version__)
print("GPUs available:", tf.config.list_physical_devices('GPU'))""",
            description = "Verify local tensor execution backend engine has recognized virtual graphic processors."
        ),
        CodeSnippet(
            title = "Download Large Files with Progress",
            category = "Data Pipeline",
            code = """!wget -q --show-progress -O dataset.zip "https://example.com/dataset.zip"
!unzip -q dataset.zip -d ./data/""",
            description = "Pull large ML training corpora and unpack them silently to current sandbox working volume."
        ),
        CodeSnippet(
            title = "Custom PyTorch Training Loop",
            category = "Deep Learning",
            code = """import torch.nn as nn
import torch.optim as optim

criterion = nn.CrossEntropyLoss()
optimizer = optim.Adam(model.parameters(), lr=0.001)

for epoch in range(epochs):
    model.train()
    running_loss = 0.0
    for inputs, labels in train_loader:
        inputs, labels = inputs.to(device), labels.to(device)
        optimizer.zero_grad()
        outputs = model(inputs)
        loss = criterion(outputs, labels)
        loss.backward()
        optimizer.step()
        running_loss += loss.item()
    print(f"Epoch {epoch+1}/{epochs} - Loss: {running_loss/len(train_loader):.4f}")""",
            description = "Standard boiler training loop architecture with backprop gradient updates."
        ),
        CodeSnippet(
            title = "Download Weights from Hugging Face",
            category = "NLP & LLMs",
            code = """from transformers import pipeline, AutoModelForSequenceClassification, AutoTokenizer

model_name = "distilbert-base-uncased-finetuned-sst-2-english"
tokenizer = AutoTokenizer.from_pretrained(model_name)
model = AutoModelForSequenceClassification.from_pretrained(model_name)
classifier = pipeline("sentiment-analysis", model=model, tokenizer=tokenizer)""",
            description = "Fetch pre-trained transformers and run simple pipeline inference."
        )
    )

    Column(modifier = modifier.fillMaxSize().background(Color(0xFFF8F9FF))) {
        // App bar
        TopAppBar(
            title = { Text("Code & Template Hub", fontWeight = FontWeight.Bold, color = Color(0xFF1C1B1F)) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8F9FF))
        )

        // Navigation Tabs (Templates vs ML Cheatsheet)
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFFF3F3FA),
            contentColor = Color(0xFF005AC1)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Starter Notebooks", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                selectedContentColor = Color(0xFF005AC1),
                unselectedContentColor = Color(0xFF44474F)
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("ML Snippets Library", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                selectedContentColor = Color(0xFF005AC1),
                unselectedContentColor = Color(0xFF44474F)
            )
        }

        if (selectedTab == 0) {
            // Templates List
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(templates) { template ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE1E2EC)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.openNotebookUrl(template.url)
                                viewModel.addNotebook(template.title, template.url, template.category)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFD8E2FF))
                            ) {
                                Icon(
                                    imageVector = template.icon,
                                    contentDescription = null,
                                    tint = Color(0xFF005AC1)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFD8E2FF))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = template.category.uppercase(),
                                        color = Color(0xFF001A41),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = template.title,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1C1B1F),
                                    fontSize = 15.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Text(
                                    text = template.description,
                                    color = Color(0xFF44474F),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            IconButton(onClick = {
                                viewModel.openNotebookUrl(template.url)
                                viewModel.addNotebook(template.title, template.url, template.category)
                            }) {
                                Icon(Icons.Default.Launch, contentDescription = "Open", tint = Color(0xFF005AC1))
                            }
                        }
                    }
                }
            }
        } else {
            // ML Snippets library
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(snippets) { snippet ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE1E2EC)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text(
                                        text = snippet.category.uppercase(),
                                        color = Color(0xFF005AC1),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = snippet.title,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1C1B1F),
                                        fontSize = 15.sp
                                    )
                                }
                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Colab Snippet", snippet.code)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Snippet copied! Paste in Colab cell.", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005AC1)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Text(
                                text = snippet.description,
                                color = Color(0xFF44474F),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )

                            // Code block block rendering
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F3FA)),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE1E2EC)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = snippet.code,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF001A41),
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
