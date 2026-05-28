package dk.itu.moapd.x9.elie.model

sealed class ImageUploadState {
    data object Idle : ImageUploadState()
    data object Uploading : ImageUploadState()

    data class Success(
        val downloadUrl: String,
        val storagePath: String
    ) : ImageUploadState()

    data class Error(
        val message: String
    ) : ImageUploadState()
}
