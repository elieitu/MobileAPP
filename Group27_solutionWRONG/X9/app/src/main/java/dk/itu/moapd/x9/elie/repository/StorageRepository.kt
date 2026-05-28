package dk.itu.moapd.x9.elie.repository

import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import dk.itu.moapd.x9.elie.util.FirebaseConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class StorageRepository(
    private val bucketUrl: String = FirebaseConfig.BUCKET_URL
) {

    private val storage: FirebaseStorage = FirebaseStorage.getInstance(bucketUrl)

    fun uploadFile(localUri: Uri, remotePath: String): Task<Uri> {
        val ref: StorageReference = storage.reference.child(remotePath)

        return ref.putFile(localUri).continueWithTask { task ->
            if (!task.isSuccessful) {
                throw (task.exception ?: Exception("Upload failed"))
            }
            ref.downloadUrl
        }
    }

    fun getDownloadUrl(remotePath: String): Task<Uri> {
        return storage.reference.child(remotePath).downloadUrl
    }

    fun deleteFile(remotePath: String): Task<Void> {
        return storage.reference.child(remotePath).delete()
    }

    fun getReference(remotePath: String): StorageReference {
        return storage.reference.child(remotePath)
    }

    suspend fun uploadReportImage(
        userId: String,
        reportId: String,
        imageUri: Uri
    ): Pair<String, String> {
        val extension = imageUri.lastPathSegment
            ?.substringAfterLast('.', "")
            ?.takeIf { it.isNotBlank() }
            ?: "jpg"
        val remotePath = "report_images/$userId/$reportId/image_${System.currentTimeMillis()}.$extension"
        val downloadUri = uploadFile(imageUri, remotePath).await()
        return remotePath to downloadUri.toString()
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result -> continuation.resume(result) }
        addOnFailureListener { exception -> continuation.resumeWithException(exception) }
        addOnCanceledListener { continuation.cancel() }
    }
}
