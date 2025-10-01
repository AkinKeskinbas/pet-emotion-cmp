package com.keak.petemotions.platform

import androidx.compose.runtime.*
import kotlinx.cinterop.*
import platform.Foundation.*
import platform.Photos.*
import platform.PhotosUI.*
import platform.UIKit.*
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

actual class CameraNativeService {
    private var currentCameraCallback: ((ByteArray?) -> Unit)? = null
    private var currentGalleryCallback: ((ByteArray?) -> Unit)? = null
    private var currentVideoGalleryCallback: ((ByteArray?) -> Unit)? = null

    actual fun openCamera(onResult: (ByteArray?) -> Unit) {
        currentCameraCallback = onResult
        presentCameraPicker()
    }

    actual fun openGallery(onResult: (ByteArray?) -> Unit) {
        currentGalleryCallback = onResult
        presentGalleryPicker()
    }

    fun openVideoGallery(onResult: (ByteArray?) -> Unit) {
        currentVideoGalleryCallback = onResult
        presentVideoGalleryPicker()
    }

    private fun presentVideoGalleryPicker() {
        when (PHPhotoLibrary.authorizationStatus()) {
            PHAuthorizationStatusAuthorized, PHAuthorizationStatusLimited -> {
                showVideosPicker()
            }
            PHAuthorizationStatusNotDetermined -> {
                PHPhotoLibrary.requestAuthorization { status ->
                    if (status == PHAuthorizationStatusAuthorized || status == PHAuthorizationStatusLimited) {
                        dispatch_async(dispatch_get_main_queue()) {
                            showVideosPicker()
                        }
                    } else {
                        println("iOS Native: Photo library access denied")
                        currentVideoGalleryCallback?.invoke(null)
                        currentVideoGalleryCallback = null
                    }
                }
            }
            else -> {
                println("iOS Native: Photo library access denied or restricted")
                currentVideoGalleryCallback?.invoke(null)
                currentVideoGalleryCallback = null
            }
        }
    }

    private fun showVideosPicker() {
        val presentingController = findTopViewController()
        if (presentingController == null) {
            println("iOS Native: Unable to locate a view controller to present the video picker")
            currentVideoGalleryCallback?.invoke(null)
            currentVideoGalleryCallback = null
            return
        }

        val configuration = PHPickerConfiguration().apply {
            filter = PHPickerFilter.videosFilter()
            selectionLimit = 1
        }

        val picker = PHPickerViewController(configuration)

        // Create delegate
        val delegate = VideoPickerDelegate { videoBytes ->
            currentVideoGalleryCallback?.invoke(videoBytes)
            currentVideoGalleryCallback = null
        }

        // Store delegate to prevent garbage collection
        GlobalDelegateStore.storeVideoDelegate(delegate)
        picker.delegate = delegate

        dispatch_async(dispatch_get_main_queue()) {
            presentingController.presentViewController(picker, animated = true, completion = null)
        }
    }

    actual fun hasCamera(): Boolean {
        return UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)
    }

    actual fun hasGalleryPermission(): Boolean {
        return when (PHPhotoLibrary.authorizationStatus()) {
            PHAuthorizationStatusAuthorized, PHAuthorizationStatusLimited -> true
            else -> false
        }
    }

    actual fun requestCameraPermission(onResult: (Boolean) -> Unit) {
        // iOS camera permission is requested automatically when first used
        onResult(true)
    }

    actual fun requestGalleryPermission(onResult: (Boolean) -> Unit) {
        when (val status = PHPhotoLibrary.authorizationStatus()) {
            PHAuthorizationStatusAuthorized, PHAuthorizationStatusLimited -> onResult(true)
            PHAuthorizationStatusNotDetermined -> {
                PHPhotoLibrary.requestAuthorization { newStatus ->
                    onResult(newStatus == PHAuthorizationStatusAuthorized || newStatus == PHAuthorizationStatusLimited)
                }
            }
            else -> onResult(false)
        }
    }

    private fun presentCameraPicker() {
        if (!hasCamera()) {
            println("iOS Native: Camera not available")
            currentCameraCallback?.invoke(null)
            currentCameraCallback = null
            return
        }

        val presentingController = findTopViewController()
        if (presentingController == null) {
            println("iOS Native: Unable to locate a view controller to present the camera")
            currentCameraCallback?.invoke(null)
            currentCameraCallback = null
            return
        }

        val imagePicker = UIImagePickerController().apply {
            sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
            allowsEditing = false
        }

        // Create delegate
        val delegate = CameraPickerDelegate { imageBytes ->
            currentCameraCallback?.invoke(imageBytes)
            currentCameraCallback = null
        }

        // Store delegate to prevent garbage collection
        GlobalDelegateStore.storeCameraDelegate(delegate)
        imagePicker.delegate = delegate

        dispatch_async(dispatch_get_main_queue()) {
            presentingController.presentViewController(imagePicker, animated = true, completion = null)
        }
    }

    private fun presentGalleryPicker() {
        when (PHPhotoLibrary.authorizationStatus()) {
            PHAuthorizationStatusAuthorized, PHAuthorizationStatusLimited -> {
                showPhotosPicker()
            }
            PHAuthorizationStatusNotDetermined -> {
                PHPhotoLibrary.requestAuthorization { status ->
                    if (status == PHAuthorizationStatusAuthorized || status == PHAuthorizationStatusLimited) {
                        dispatch_async(dispatch_get_main_queue()) {
                            showPhotosPicker()
                        }
                    } else {
                        println("iOS Native: Photo library access denied")
                        currentGalleryCallback?.invoke(null)
                        currentGalleryCallback = null
                    }
                }
            }
            else -> {
                println("iOS Native: Photo library access denied or restricted")
                currentGalleryCallback?.invoke(null)
                currentGalleryCallback = null
            }
        }
    }

    private fun showPhotosPicker() {
        val presentingController = findTopViewController()
        if (presentingController == null) {
            println("iOS Native: Unable to locate a view controller to present the photo picker")
            currentGalleryCallback?.invoke(null)
            currentGalleryCallback = null
            return
        }

        val configuration = PHPickerConfiguration().apply {
            filter = PHPickerFilter.imagesFilter()
            selectionLimit = 1
        }

        val picker = PHPickerViewController(configuration)

        // Create delegate
        val delegate = PhotoPickerDelegate { imageBytes ->
            currentGalleryCallback?.invoke(imageBytes)
            currentGalleryCallback = null
        }

        // Store delegate to prevent garbage collection
        GlobalDelegateStore.storePhotoDelegate(delegate)
        picker.delegate = delegate

        dispatch_async(dispatch_get_main_queue()) {
            presentingController.presentViewController(picker, animated = true, completion = null)
        }
    }
}

@Composable
actual fun rememberCameraNativeService(): CameraNativeService {
    return remember { CameraNativeService() }
}

private fun findTopViewController(): UIViewController? {
    val application = UIApplication.sharedApplication

    val applicationWindows = application.windows as? List<*> ?: emptyList<Any?>()

    for (windowAny in applicationWindows) {
        val window = windowAny as? UIWindow ?: continue
        window.rootViewController?.resolveTopMost()?.let { return it }
    }

    return application.keyWindow?.rootViewController?.resolveTopMost()
}

private tailrec fun UIViewController.resolveTopMost(): UIViewController {
    val presented = presentedViewController
    if (presented != null) {
        return presented.resolveTopMost()
    }

    return when (this) {
        is UINavigationController -> visibleViewController?.resolveTopMost() ?: this
        is UITabBarController -> selectedViewController?.resolveTopMost() ?: this
        else -> this
    }
}

// Delegate storage to prevent garbage collection
object GlobalDelegateStore {
    private var cameraDelegate: CameraPickerDelegate? = null
    private var photoDelegate: PhotoPickerDelegate? = null
    private var videoDelegate: VideoPickerDelegate? = null

    fun storeCameraDelegate(delegate: CameraPickerDelegate) {
        cameraDelegate = delegate
    }

    fun storePhotoDelegate(delegate: PhotoPickerDelegate) {
        photoDelegate = delegate
    }

    fun storeVideoDelegate(delegate: VideoPickerDelegate) {
        videoDelegate = delegate
    }
}

// Camera picker delegate
class CameraPickerDelegate(
    private val onResult: (ByteArray?) -> Unit
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>
    ) {
        picker.dismissViewControllerAnimated(true, null)

        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        image?.let { uiImage ->
            val imageData = UIImageJPEGRepresentation(uiImage, 0.8)
            imageData?.let { data ->
                val bytes = data.toByteArray()
                println("iOS Native: Camera photo captured: ${bytes.size} bytes")
                onResult(bytes)
                return
            }
        }

        println("iOS Native: No image captured")
        onResult(null)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, null)
        println("iOS Native: Camera capture cancelled")
        onResult(null)
    }
}

// Photo picker delegate
class PhotoPickerDelegate(
    private val onResult: (ByteArray?) -> Unit
) : NSObject(), PHPickerViewControllerDelegateProtocol {

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, null)

        val results = didFinishPicking.filterIsInstance<PHPickerResult>()
        if (results.isNotEmpty()) {
            val result = results.first()
            val itemProvider = result.itemProvider

            val typeIdentifiers = itemProvider.registeredTypeIdentifiers as? List<*>
            val preferredIdentifier = typeIdentifiers
                ?.firstOrNull { identifier ->
                    identifier is String && identifier.contains("image", ignoreCase = true)
                } as? String ?: "public.image"

            itemProvider.loadDataRepresentationForTypeIdentifier(preferredIdentifier) { data, error ->
                if (error == null && data != null) {
                    val bytes = data.toByteArray()
                    println("iOS Native: Gallery image selected: ${bytes.size} bytes")
                    onResult(bytes)
                } else {
                    println("iOS Native: Error loading gallery image: ${error?.localizedDescription}")
                    onResult(null)
                }
            }
            return
        }

        println("iOS Native: No image selected")
        onResult(null)
    }
}

// Video picker delegate
class VideoPickerDelegate(
    private val onResult: (ByteArray?) -> Unit
) : NSObject(), PHPickerViewControllerDelegateProtocol {

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, null)

        val results = didFinishPicking.filterIsInstance<PHPickerResult>()
        if (results.isNotEmpty()) {
            val result = results.first()
            val itemProvider = result.itemProvider

            val typeIdentifiers = itemProvider.registeredTypeIdentifiers as? List<*>
            val preferredIdentifier = typeIdentifiers
                ?.firstOrNull { identifier ->
                    identifier is String && identifier.contains("movie", ignoreCase = true)
                } as? String ?: "public.movie"

            itemProvider.loadDataRepresentationForTypeIdentifier(preferredIdentifier) { data, error ->
                if (error == null && data != null) {
                    val bytes = data.toByteArray()
                    println("iOS Native: Gallery video selected: ${bytes.size} bytes")
                    onResult(bytes)
                } else {
                    println("iOS Native: Error loading gallery video: ${error?.localizedDescription}")
                    onResult(null)
                }
            }
            return
        }

        println("iOS Native: No video selected")
        onResult(null)
    }
}

// Extension to convert NSData to ByteArray
@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    val bytes = ByteArray(length)
    if (length > 0) {
        bytes.usePinned { pinned ->
            getBytes(pinned.addressOf(0), length.toULong())
        }
    }
    return bytes
}
