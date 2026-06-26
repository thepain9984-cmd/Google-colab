package com.example.data.repository

import com.example.data.database.NotebookDao
import com.example.data.database.NotebookEntity
import com.example.data.database.ServerDao
import com.example.data.database.ServerEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AppRepository(
    private val serverDao: ServerDao,
    private val notebookDao: NotebookDao
) {
    val allServers: Flow<List<ServerEntity>> = serverDao.getAllServers()
    val allNotebooks: Flow<List<NotebookEntity>> = notebookDao.getAllNotebooks()

    suspend fun getServerById(id: Int): ServerEntity? = serverDao.getServerById(id)

    suspend fun insertServer(server: ServerEntity): Long = serverDao.insertServer(server)

    suspend fun updateServer(server: ServerEntity) = serverDao.updateServer(server)

    suspend fun deleteServer(server: ServerEntity) = serverDao.deleteServer(server)

    suspend fun insertNotebook(notebook: NotebookEntity): Long = notebookDao.insertNotebook(notebook)

    suspend fun updateNotebook(notebook: NotebookEntity) = notebookDao.updateNotebook(notebook)

    suspend fun deleteNotebook(notebook: NotebookEntity) = notebookDao.deleteNotebook(notebook)

    suspend fun updateNotebookLastOpened(id: Int, timestamp: Long) = notebookDao.updateLastOpened(id, timestamp)

    suspend fun seedDefaultNotebooksIfNeeded() {
        val current = allNotebooks.first()
        if (current.isEmpty()) {
            val defaults = listOf(
                NotebookEntity(
                    title = "Welcome to Google Colab Mobile",
                    url = "https://colab.research.google.com/notebooks/intro.ipynb",
                    category = "Guides",
                    isPinned = true
                ),
                NotebookEntity(
                    title = "PyTorch Training Basics",
                    url = "https://colab.research.google.com/github/pytorch/tutorials/blob/gh-pages/_downloads/623da16a3a78912e96d7448375e8efca/tensor_tutorial.ipynb",
                    category = "Deep Learning",
                    isPinned = false
                ),
                NotebookEntity(
                    title = "TensorFlow Quickstart",
                    url = "https://colab.research.google.com/github/tensorflow/docs/blob/master/site/en/tutorials/quickstart/beginner.ipynb",
                    category = "Deep Learning",
                    isPinned = false
                ),
                NotebookEntity(
                    title = "Hugging Face Quick Tour",
                    url = "https://colab.research.google.com/github/huggingface/notebooks/blob/main/transformers_doc/en/pytorch/quicktour.ipynb",
                    category = "NLP & Transformers",
                    isPinned = false
                ),
                NotebookEntity(
                    title = "Interactive Data Visualizations",
                    url = "https://colab.research.google.com/notebooks/charts.ipynb",
                    category = "Data Science",
                    isPinned = false
                )
            )
            for (notebook in defaults) {
                notebookDao.insertNotebook(notebook)
            }
        }
    }
}
