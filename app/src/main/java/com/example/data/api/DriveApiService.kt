package com.example.data.api

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

data class DriveFile(
    val id: String,
    val name: String,
    val mimeType: String,
    val modifiedTime: String? = null,
    val size: String? = null
)

data class DriveFileListResponse(
    val files: List<DriveFile>
)

interface DriveApiService {
    @GET("files")
    suspend fun listFiles(
        @Header("Authorization") authHeader: String,
        @Query("q") query: String? = null,
        @Query("fields") fields: String = "files(id, name, mimeType, modifiedTime, size)",
        @Query("pageSize") pageSize: Int = 100
    ): DriveFileListResponse
}
