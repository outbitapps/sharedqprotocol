// Copyright 2023 Skip
//
// This is free software: you can redistribute and/or modify it
// under the terms of the GNU Lesser General Public License 3.0
// as published by the Free Software Foundation https://fsf.org

package skip.foundation

import skip.lib.*
import skip.lib.Array


private val logger: SkipLogger = SkipLogger(subsystem = "skip", category = "URLSession")

class URLSession {
    var configuration: URLSessionConfiguration

    constructor(configuration: URLSessionConfiguration) {
        this.configuration = configuration
        this.delegate = null
        this.delegateQueue = null
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    constructor(configuration: URLSessionConfiguration, delegate: URLSessionDelegate?, delegateQueue: OperationQueue?) {
        this.configuration = configuration
        this.delegate = delegate.sref()
        this.delegateQueue = delegateQueue
    }

    val delegate: URLSessionDelegate?
    val delegateQueue: OperationQueue?

    private fun connection(for_: URLRequest): Tuple2<URL, java.net.URLConnection> {
        val request = for_
        val url_0 = request.url.sref()
        if (url_0 == null) {
            throw NoURLInRequestError()
        }
        val config = this.configuration

        // note that `openConnection` does not actually connect()
        val connection = url_0.platformValue.openConnection()

        when (request.cachePolicy) {
            URLRequest.CachePolicy.useProtocolCachePolicy -> connection.setUseCaches(true)
            URLRequest.CachePolicy.returnCacheDataElseLoad -> connection.setUseCaches(true)
            URLRequest.CachePolicy.returnCacheDataDontLoad -> connection.setUseCaches(true)
            URLRequest.CachePolicy.reloadRevalidatingCacheData -> connection.setUseCaches(true)
            URLRequest.CachePolicy.reloadIgnoringLocalCacheData -> connection.setUseCaches(false)
            URLRequest.CachePolicy.reloadIgnoringLocalAndRemoteCacheData -> connection.setUseCaches(false)
        }

        (connection as? java.net.HttpURLConnection).sref()?.let { httpConnection ->
            request.httpMethod?.let { httpMethod ->
                httpConnection.setRequestMethod(httpMethod)
            }

            httpConnection.connectTimeout = if (request.timeoutInterval > 0) (request.timeoutInterval * 1000.0).toInt() else (config.timeoutIntervalForRequest * 1000.0).toInt()
            httpConnection.readTimeout = config.timeoutIntervalForResource.toInt()
        }

        for ((headerKey, headerValue) in (request.allHTTPHeaderFields ?: dictionaryOf()).sref()) {
            connection.setRequestProperty(headerKey, headerValue)
        }

        request.httpBody.sref()?.let { httpBody ->
            connection.setDoOutput(true)
            val os = connection.getOutputStream()
            os.write(httpBody.platformValue)
            os.flush()
            os.close()
        }

        return Tuple2(url_0.sref(), connection.sref())
    }

    private fun response(for_: URL, with: java.net.URLConnection): HTTPURLResponse {
        val url = for_
        val connection = with
        var statusCode = -1
        (connection as? java.net.HttpURLConnection).sref()?.let { httpConnection ->
            statusCode = httpConnection.getResponseCode()
        }

        val headerFields = connection.getHeaderFields()

        val httpVersion: String? = null // TODO: extract version from response
        var headers: Dictionary<String, String> = dictionaryOf()
        for ((key, values) in headerFields.sref()) {
            if ((key != null) && (values != null)) {
                for (value in values.sref()) {
                    if (value != null) {
                        headers[key] = value
                    }
                }
            }
        }
        val response = (try { HTTPURLResponse(url = url, statusCode = statusCode, httpVersion = httpVersion, headerFields = headers) } catch (_: NullReturnException) { null })
        return response!!
    }

    suspend fun data(for_: URLRequest): Tuple2<Data, URLResponse> {
        val request = for_
        val job = kotlinx.coroutines.Job()
        return kotlinx.coroutines.withContext(job + kotlinx.coroutines.Dispatchers.IO) l@{ ->
            val (url, connection) = connection(for_ = request)
            var inputStream: java.io.InputStream? = null
            return@l withTaskCancellationHandler(operation = { -> Async.run l@{
                val response = response(for_ = url, with = connection)

                inputStream = connection.getInputStream()
                val outputStream = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(1024)
                inputStream.sref()?.let { stableInputStream ->
                    var bytesRead: Int
                    while ((stableInputStream.read(buffer).also { it -> bytesRead = it } != -1)) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                }
                cleanup(connection = connection, inputStream = inputStream)

                val bytes = outputStream.toByteArray()
                return@l Tuple2(Data(platformValue = bytes), response)
            } }, onCancel = { ->
                cleanup(connection = connection, inputStream = inputStream)
                job.cancel()
            })
        }
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    suspend fun data(for_: URLRequest, delegate: URLSessionTaskDelegate?): Tuple2<Data, URLResponse> = Async.run {
        val request = for_
        fatalError()
    }

    suspend fun data(from: URL): Tuple2<Data, URLResponse> {
        val url = from
        return this.data(for_ = URLRequest(url = url))
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    suspend fun data(from: URL, delegate: URLSessionTaskDelegate?): Tuple2<Data, URLResponse> = Async.run {
        val url = from
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    suspend fun download(for_: URLRequest): Tuple2<URL, URLResponse> {
        val request = for_
        val url_1 = request.url.sref()
        if (url_1 == null) {
            throw NoURLInRequestError()
        }

        // seems to be the typical way of converting from java.net.URL into android.net.Uri (which is needed by the DownloadManager)
        val uri = android.net.Uri.parse(url_1.description)

        val ctx = ProcessInfo.processInfo.androidContext.sref()
        val downloadManager = (ctx.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager).sref()

        val downloadRequest = android.app.DownloadManager.Request(uri)
            .setAllowedOverMetered(this.configuration.allowsExpensiveNetworkAccess)
            .setAllowedOverRoaming(this.configuration.allowsConstrainedNetworkAccess)
            .setShowRunningNotification(true)

        for ((headerKey, headerValue) in (request.allHTTPHeaderFields ?: dictionaryOf()).sref()) {
            downloadRequest.addRequestHeader(headerKey, headerValue)
        }

        val downloadId = downloadManager.enqueue(downloadRequest)
        val query = android.app.DownloadManager.Query()
            .setFilterById(downloadId)

        // Query the DownloadManager for the response, which returns a SQLite cursor with the current download status of all the outstanding downloads.
        fun queryDownload(): Result<Tuple2<URL, URLResponse>, Error>? {
            var deferaction_0: (() -> Unit)? = null
            try {
                val cursor = downloadManager.query(query)

                deferaction_0 = {
                    cursor.close()
                }

                if (!cursor.moveToFirst()) {
                    // download not found
                    val error = UnableToStartDownload()
                    return Result.failure(error)
                }

                val status = cursor.getInt(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_STATUS))
                val uri = cursor.getString(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_URI)) // URI to be downloaded.

                // STATUS_FAILED, STATUS_PAUSED, STATUS_PENDING, STATUS_RUNNING, STATUS_SUCCESSFUL
                if (status == android.app.DownloadManager.STATUS_PAUSED) {
                    return null
                }
                if (status == android.app.DownloadManager.STATUS_PENDING) {
                    return null
                }

                //let desc = cursor.getString(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_DESCRIPTION)) // The client-supplied description of this download // NPE
                //let id = cursor.getString(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_ID)) // An identifier for a particular download, unique across the system. // NPE
                // let lastModified = cursor.getLong(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP)) // Timestamp when the download was last modified, in System.currentTimeMillis() (wall clock time in UTC).

                // Error: java.lang.SecurityException: COLUMN_LOCAL_FILENAME is deprecated; use ContentResolver.openFileDescriptor() instead
                // let localFilename = cursor.getString(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_LOCAL_FILENAME)) // Path to the downloaded file on disk.

                //let localURI = cursor.getString(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_LOCAL_URI)) // Uri where downloaded file will be stored. // NPE
                // let mediaproviderURI = cursor.getString(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_MEDIAPROVIDER_URI)) // The URI to the corresponding entry in MediaProvider for this downloaded entry. // NPE
                //let mediaType = cursor.getString(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_MEDIA_TYPE)) // Internet Media Type of the downloaded file.
                val reason = cursor.getString(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_REASON)) // Provides more detail on the status of the download.
                //let title = cursor.getString(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_TITLE)) // The client-supplied title for this download.
                val totalSizeBytes = cursor.getLong(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_TOTAL_SIZE_BYTES)) // Total size of the download in bytes.
                val bytesDownloaded = cursor.getLong(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)) // Number of bytes download so far.

                if (status == android.app.DownloadManager.STATUS_RUNNING) {
                    // TODO: update progress
                    //  if let progress = Progress.current() {
                    //  }
                    return null
                } else if (status == android.app.DownloadManager.STATUS_SUCCESSFUL) {
                    val httpVersion: String? = null // TODO: extract version from response
                    var headers: Dictionary<String, String> = dictionaryOf()
                    val statusCode = 200 // TODO: extract status code
                    headers["Content-Length"] = totalSizeBytes?.description
                    //headers["Last-Modified"] = lastModified // TODO: convert to Date
                    val response = (try { HTTPURLResponse(url = url_1, statusCode = statusCode, httpVersion = httpVersion, headerFields = headers) } catch (_: NullReturnException) { null })
                    //let localURL = URL(fileURLWithPath: localFilename)

                    // Type mismatch: inferred type is String! but Uri was expected
                    //                guard let pfd = ctx.getContentResolver().openFileDescriptor(uri, "r") else {
                    //                    // TODO: create error from error
                    //                    let error = FailedToDownloadURLError()
                    //                    return Result.failure(error)
                    //                }

                    // unfortunately we cannot get a file path from a descriptor, so we need to copy the contents to a temporary file, and then return that one
                    return Result.failure(DownloadUnimplentedError())
                    // TODO: return Result.success((localURL as URL, response as URLResponse))
                } else if (status == android.app.DownloadManager.STATUS_FAILED) {
                    // File download failed
                    // TODO: create error from error
                    val error = FailedToDownloadURLError()
                    return Result.failure(error)
                } else {
                    // no known android.app.DownloadManager.STATUS_*
                    // this can happen with Robolectric tests, since ShadowDownloadManager is just a stub and it sets 0 for the status
                    val error = DownloadUnsupportedWithRobolectric(status = status)
                    return Result.failure(error)
                }

                return null
            } finally {
                deferaction_0?.invoke()
            }
        }

        val isRobolectric = (try { Class.forName("org.robolectric.Robolectric") } catch (_: Throwable) { null }) != null

        val response: Result<Tuple2<URL, URLResponse>, Error> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) l@{ ->
            if (isRobolectric) {
                // Robolectric's ShadowDownloadManager doesn't actually download anything, so we fake it for testing by just getting the data in-memory (hoping it isn't too large!) and saving it to a temporary file
                try {
                    val (data, response) = data(for_ = request)
                    val outputFileURL: URL = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
                    data.write(to = outputFileURL)
                    return@l Result.success(Tuple2(outputFileURL.sref(), response))
                } catch (error: Throwable) {
                    @Suppress("NAME_SHADOWING") val error = error.aserror()
                    return@l Result.failure(error)
                }
            } else {
                // initiate using android.app.DownloadManager
                // TODO: rather than polling in a loop, we could do android.registerBroadcastReceiver(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE, handleDownloadEvent)
                while (true) {
                    queryDownload()?.let { downloadResult ->
                        return@l downloadResult
                    }
                    kotlinx.coroutines.delay(250) // wait and poll again
                }
            }
            return@l Result.failure(FailedToDownloadURLError()) // needed for Kotlin type checking
        }

        when (response) {
            is Result.FailureCase -> {
                val error = response.associated0
                throw error as Throwable
            }
            is Result.SuccessCase -> {
                val urlResponseTuple = response.associated0
                return urlResponseTuple.sref()
            }
        }
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    suspend fun download(for_: URLRequest, delegate: URLSessionTaskDelegate?): Tuple2<URL, URLResponse> = Async.run {
        val request = for_
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    suspend fun download(from: URL): Tuple2<URL, URLResponse> {
        val url = from
        // return self.download(for: URLRequest(url: url))
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    suspend fun download(from: URL, delegate: URLSessionTaskDelegate?): Tuple2<URL, URLResponse> = Async.run {
        val url = from
        // return self.download(for: URLRequest(url: url))
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    internal fun download(resumeFrom: Data, delegate: URLSessionTaskDelegate?): Tuple2<URL, URLResponse> {
        fatalError()
    }

    suspend fun upload(for_: URLRequest, fromFile: URL): Tuple2<Data, URLResponse> {
        val request = for_
        val fileURL = fromFile
        val job = kotlinx.coroutines.Job()
        return kotlinx.coroutines.withContext(job + kotlinx.coroutines.Dispatchers.IO) l@{ ->
            val data = Data(contentsOfFile = fileURL.absoluteString)
            request.httpBody = data
            return@l upload(for_ = request, job = job)
        }
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    suspend fun upload(for_: URLRequest, fromFile: URL, delegate: URLSessionTaskDelegate?): Tuple2<Data, URLResponse> = Async.run {
        val request = for_
        val fileURL = fromFile
        fatalError()
    }

    suspend fun upload(for_: URLRequest, from: Data): Tuple2<Data, URLResponse> {
        val request = for_
        val bodyData = from
        val job = kotlinx.coroutines.Job()
        return kotlinx.coroutines.withContext(job + kotlinx.coroutines.Dispatchers.IO) l@{ ->
            request.httpBody = bodyData
            return@l upload(for_ = request, job = job)
        }
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    suspend fun upload(for_: URLRequest, from: Data, delegate: URLSessionTaskDelegate?): Tuple2<Data, URLResponse> = Async.run {
        val request = for_
        val bodyData = from
        fatalError()
    }

    private suspend fun upload(for_: URLRequest, job: kotlinx.coroutines.Job): Tuple2<Data, URLResponse> {
        val request = for_
        val (url, connection) = connection(for_ = request)
        var inputStream: java.io.InputStream? = null
        return withTaskCancellationHandler(operation = { -> Async.run l@{
            val response = response(for_ = url, with = connection)

            inputStream = connection.getInputStream()
            val responseData = java.io.BufferedInputStream(inputStream).readBytes()
            cleanup(connection = connection, inputStream = inputStream)

            return@l Tuple2(Data(platformValue = responseData), response as URLResponse)
        } }, onCancel = { ->
            cleanup(connection = connection, inputStream = inputStream)
            job.cancel()
        })
    }

    suspend fun bytes(for_: URLRequest): Tuple2<URLSession.AsyncBytes, URLResponse> {
        val request = for_
        val job = kotlinx.coroutines.Job()
        return kotlinx.coroutines.withContext(job + kotlinx.coroutines.Dispatchers.IO) { ->
            val (url, connection) = connection(for_ = request)
            withTaskCancellationHandler(operation = { -> Async.run l@{
                val response = response(for_ = url, with = connection)
                val inputStream = connection.getInputStream()
                val stream = AsyncBytes(connection = connection, inputStream = inputStream)
                return@l Tuple2(stream.sref(), response)
            } }, onCancel = { ->
                cleanup(connection = connection, inputStream = null)
                job.cancel()
            })
        }
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    suspend fun bytes(for_: URLRequest, delegate: URLSessionTaskDelegate?): Tuple2<URLSession.AsyncBytes, URLResponse> = Async.run {
        val request = for_
        fatalError()
    }

    suspend fun bytes(from: URL): Tuple2<URLSession.AsyncBytes, URLResponse> {
        val url = from
        return bytes(for_ = URLRequest(url = url))
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    suspend fun bytes(from: URL, delegate: URLSessionTaskDelegate?): Tuple2<URLSession.AsyncBytes, URLResponse> = Async.run {
        val url = from
        fatalError()
    }

    class AsyncBytes: AsyncSequence<UByte>, MutableStruct {

        internal val connection: java.net.URLConnection
        internal var inputStream: java.io.InputStream? = null
            get() = field.sref({ this.inputStream = it })
            set(newValue) {
                @Suppress("NAME_SHADOWING") val newValue = newValue.sref()
                willmutate()
                field = newValue
                didmutate()
            }

        internal constructor(connection: java.net.URLConnection, inputStream: java.io.InputStream) {
            this.connection = connection.sref()
            this.inputStream = inputStream
        }

        fun finalize(): Unit = close()

        override fun makeAsyncIterator(): URLSession.AsyncBytes.Iterator = Iterator(bytes = this)

        internal fun close() {
            cleanup(connection = connection, inputStream = inputStream)
            inputStream = null
        }

        class Iterator: AsyncIteratorProtocol<UByte> {
            private val bytes: URLSession.AsyncBytes

            internal constructor(bytes: URLSession.AsyncBytes) {
                this.bytes = bytes.sref()
            }

            override suspend fun next(): UByte? = Async.run l@{
                val byte_0 = try { bytes.inputStream?.read() } catch (_: Throwable) { null }
                if ((byte_0 == null) || (byte_0 == -1)) {
                    bytes.close()
                    return@l null
                }
                return@l UByte(byte_0)
            }

            companion object {
            }
        }

        private constructor(copy: MutableStruct) {
            @Suppress("NAME_SHADOWING", "UNCHECKED_CAST") val copy = copy as URLSession.AsyncBytes
            this.connection = copy.connection
            this.inputStream = copy.inputStream
        }

        override var supdate: ((Any) -> Unit)? = null
        override var smutatingcount = 0
        override fun scopy(): MutableStruct = URLSession.AsyncBytes(this as MutableStruct)

        companion object {
        }
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun dataTask(with: URL): URLSessionDataTask {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun dataTask(with: URL, completionHandler: (Data?, URLResponse?, Error?) -> Unit): URLSessionDataTask {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun dataTask(with: URLRequest): URLSessionDataTask {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun dataTask(with: URLRequest, completionHandler: (Data?, URLResponse?, Error?) -> Unit): URLSessionDataTask {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun downloadTask(with: URL): URLSessionDownloadTask {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun downloadTask(with: URL, completionHandler: (URL?, URLResponse?, Error?) -> Unit): URLSessionDownloadTask {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun downloadTask(with: URLRequest): URLSessionDownloadTask {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun downloadTask(with: URLRequest, completionHandler: (URL?, URLResponse?, Error?) -> Unit): URLSessionDownloadTask {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun downloadTask(withResumeData: Data): URLSessionDownloadTask {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun downloadTask(withResumeData: Data, completionHandler: (URL?, URLResponse?, Error?) -> Unit): URLSessionDownloadTask {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun uploadTask(with: URLRequest, from: Data): URLSessionUploadTask {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun uploadTask(with: URLRequest, from: Data?, completionHandler: (Data?, URLResponse?, Error?) -> Unit): URLSessionUploadTask {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun uploadTask(with: URLRequest, fromFile: URL): URLSessionUploadTask {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun uploadTask(with: URLRequest, fromFile: URL, completionHandler: (Data?, URLResponse?, Error?) -> Unit): URLSessionUploadTask {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun uploadTask(withStreamedRequest: URLRequest): URLSessionUploadTask {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    internal fun uploadTask(withResumeData: Data): URLSessionUploadTask {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    internal fun uploadTask(withResumeData: Data, completionHandler: (Data?, URLResponse?, Error?) -> Unit): URLSessionUploadTask {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun streamTask(withHostName: String, port: Int): URLSessionStreamTask {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun webSocketTask(with: URL): URLSessionWebSocketTask {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun webSocketTask(with: URLRequest): URLSessionWebSocketTask {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun webSocketTask(with: URL, protocols: Array<String>): URLSessionWebSocketTask {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun finishTasksAndInvalidate() = Unit

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun flush(completionHandler: () -> Unit) = Unit

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun getTasksWithCompletionHandler(handler: (Array<URLSessionDataTask>, Array<URLSessionUploadTask>, Array<URLSessionDownloadTask>) -> Unit) = Unit

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun getAllTasks(completionHandler: (Array<URLSessionTask>) -> Unit) = Unit

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun invalidateAndCancel() = Unit

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun reset(completionHandler: () -> Unit) = Unit

    var sessionDescription: String? = null

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun dataTaskPublisher(for_: URLRequest): Any {
        fatalError()
    }

    @Deprecated("This API is not yet available in Skip. Consider placing it within a #if !SKIP block. You can file an issue against the owning library at https://github.com/skiptools, or see the library README for information on adding support", level = DeprecationLevel.ERROR)
    fun dataTaskPublisher(for_: URL): Any {
        fatalError()
    }

    enum class DelayedRequestDisposition(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): Sendable, RawRepresentable<Int> {
        continueLoading(0),
        useNewRequest(1),
        cancel(2);

        companion object {
        }
    }

    enum class AuthChallengeDisposition(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): Sendable, RawRepresentable<Int> {
        useCredential(0),
        performDefaultHandling(1),
        cancelAuthenticationChallenge(2),
        rejectProtectionSpace(3);

        companion object {
        }
    }

    enum class ResponseDisposition(override val rawValue: Int, @Suppress("UNUSED_PARAMETER") unusedp: Nothing? = null): Sendable, RawRepresentable<Int> {
        cancel(0),
        allow(1),
        becomeDownload(2),
        becomeStream(3);

        companion object {
        }
    }

    companion object {

        val shared = URLSession(configuration = URLSessionConfiguration.default)

        fun DelayedRequestDisposition(rawValue: Int): URLSession.DelayedRequestDisposition? {
            return when (rawValue) {
                0 -> DelayedRequestDisposition.continueLoading
                1 -> DelayedRequestDisposition.useNewRequest
                2 -> DelayedRequestDisposition.cancel
                else -> null
            }
        }

        fun AuthChallengeDisposition(rawValue: Int): URLSession.AuthChallengeDisposition? {
            return when (rawValue) {
                0 -> AuthChallengeDisposition.useCredential
                1 -> AuthChallengeDisposition.performDefaultHandling
                2 -> AuthChallengeDisposition.cancelAuthenticationChallenge
                3 -> AuthChallengeDisposition.rejectProtectionSpace
                else -> null
            }
        }

        fun ResponseDisposition(rawValue: Int): URLSession.ResponseDisposition? {
            return when (rawValue) {
                0 -> ResponseDisposition.cancel
                1 -> ResponseDisposition.allow
                2 -> ResponseDisposition.becomeDownload
                3 -> ResponseDisposition.becomeStream
                else -> null
            }
        }
    }
}

private fun cleanup(connection: java.net.URLConnection, inputStream: java.io.InputStream?) {
    try {
        inputStream?.close()
    } catch (error: Throwable) {
        @Suppress("NAME_SHADOWING") val error = error.aserror()
    }
    (connection as? java.net.HttpURLConnection).sref()?.let { httpConnection ->
        run {
            httpConnection.disconnect()
        }
    }
}

interface URLSessionTask {
}

interface URLSessionDataTask: URLSessionTask {
}

interface URLSessionDownloadTask: URLSessionTask {
}

interface URLSessionUploadTask: URLSessionTask {
}

interface URLSessionStreamTask: URLSessionTask {
}

interface URLSessionWebSocketTask: URLSessionTask {
}

interface URLSessionDelegate {
}

interface URLSessionTaskDelegate: URLSessionDelegate {
}

interface URLSessionDataDelegate: URLSessionTaskDelegate {
}

class NoURLInRequestError: Exception(), Error {

    companion object {
    }
}

class FailedToDownloadURLError: Exception(), Error {

    companion object {
    }
}

class DownloadUnimplentedError: Exception(), Error {

    companion object {
    }
}

class UnableToStartDownload: Exception(), Error {

    companion object {
    }
}

class DownloadUnsupportedWithRobolectric: Exception, Error {
    internal val status: Int

    internal constructor(status: Int): super() {
        this.status = status
    }

    companion object {
    }
}

