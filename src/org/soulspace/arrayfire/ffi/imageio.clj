(ns org.soulspace.arrayfire.ffi.imageio
  "Bindings for the ArrayFire image I/O functions.
   
   Image I/O provides functions for loading and saving images in various formats
   using the FreeImage library. Supports both file-based and memory-based I/O.
   
   Supported Formats:
   - **Lossless**: PNG, BMP, TIFF, PNM (PGM, PPM), TGA, EXR, HDR
   - **Lossy**: JPEG, JPEG2000
   - **Vector**: ICO, GIF
   - **RAW**: Various camera RAW formats
   - **High Dynamic Range**: EXR, HDR, PFM
   
   Image Data Format:
   ArrayFire stores images in column-major format with dimensions [height, width, channels]:
   - Grayscale: [H, W, 1]
   - RGB: [H, W, 3]
   - RGBA: [H, W, 4]
   
   Channels are stored separately (planar format), not interleaved:
   - Memory layout: [all R values][all G values][all B values]
   - This differs from typical image libraries using interleaved RGB
   
   Bit Depth Support:
   - **8-bit**: u8 arrays (range 0-255) - most common
   - **16-bit**: u16 arrays (range 0-65535) - high precision
   - **32-bit float**: f32 arrays (range 0-1) - HDR, processing
   - **32-bit int**: u32, s32 for special formats
   
   Color vs Grayscale Loading:
   - isColor=false: Load as grayscale (1 channel)
     * RGB images converted using: 0.2126*R + 0.7152*G + 0.0722*B (ITU-R BT.709)
   - isColor=true: Load as RGB (3 channels)
     * Grayscale images replicated to 3 channels
   
   Memory I/O:
   Uses FreeImage FIMEMORY structures for in-memory image operations:
   - Load from memory buffer (e.g., downloaded images)
   - Save to memory buffer (e.g., for network transmission)
   - Avoid disk I/O overhead
   - Useful for embedded image data
   
   Native I/O (imageio2.cpp):
   Load/save without type conversion, preserving original bit depth:
   - Faster than standard I/O (no conversion)
   - Preserves exact pixel values
   - Limited to u8, u16, f32 types
   
   ArrayFire Configuration:
   These functions require ArrayFire to be compiled WITH_FREEIMAGE=ON.
   Without FreeImage support, all functions return AF_ERR_NOT_CONFIGURED.
   
   Performance Considerations:
   - PNG: Lossless, slower, good compression
   - JPEG: Lossy, fast, smaller files, quality parameter
   - EXR: HDR, excellent for float data, larger files
   - Loading is typically I/O bound, not compute bound
   - Memory I/O faster than file I/O
   - Native I/O faster than standard I/O (no conversion)
   
   Common Workflows:
   
   1. **Load and Process**:
   ```clojure
   (let [img (load-image \"photo.jpg\" true)
         processed (process-image img)]
     (save-image \"output.png\" processed))
   ```
   
   2. **Memory Buffer I/O**:
   ```clojure
   ;; Load from HTTP response
   (let [buffer (download-image url)
         img (load-image-memory buffer)]
     (process img))
   
   ;; Save for network transmission
   (let [buffer (save-image-memory img :jpeg)]
     (send-over-network buffer)
     (delete-image-memory buffer))
   ```
   
   3. **Batch Processing**:
   ```clojure
   (doseq [file image-files]
     (let [img (load-image file false)
           enhanced (enhance img)]
       (save-image (output-path file) enhanced)))
   ```
   
   Type Conversion on Load:
   ArrayFire automatically converts loaded images to float (f32):
   - 8-bit (0-255) → float (0-1): divide by 255
   - 16-bit (0-65535) → float (0-1): divide by 65535
   - Float already in 0-1 range
   - Preserves precision, enables GPU computation
   
   Type Conversion on Save:
   Float arrays (0-1) converted to target format:
   - PNG/BMP: Multiply by 255, convert to u8
   - 16-bit TIFF: Multiply by 65535, convert to u16
   - EXR: Save as float (no conversion)
   
   Error Handling:
   Functions return af_err codes:
   - AF_SUCCESS (0): Operation successful
   - AF_ERR_NOT_CONFIGURED: FreeImage not available
   - AF_ERR_ARG: Invalid arguments
   - AF_ERR_LOAD_LIB: Failed to load image
   - AF_ERR_RUNTIME: File I/O errors
   
   Applications:
   - Computer vision preprocessing
   - Image dataset loading for ML
   - Batch image processing
   - Format conversion
   - Thumbnail generation
   - Image analysis pipelines
   
   Limitations:
   - Requires FreeImage library at runtime
   - Some exotic formats may not be supported
   - Large images may exhaust GPU memory
   - Animated formats (GIF, APNG) load first frame only
   
   See also:
   - Image processing functions in separate namespaces
   - af-load-image-native, af-save-image-native: No type conversion"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; File I/O
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; af_err af_load_image(af_array *out, const char* filename, const bool isColor)
(defcfn af-load-image
  "Load an image from a file.
   
   Loads an image from disk using FreeImage library with automatic format
   detection based on file extension or content.
   
   Parameters:
   - out: out pointer for resulting image array
   - filename: pointer to null-terminated string (file path)
   - is-color: boolean (int) for color mode
     * 0 (false): Load as grayscale (1 channel)
     * 1 (true): Load as RGB (3 channels)
   
   Output Array Format:
   - Dimensions: [height, width, channels]
   - Type: f32 (float) with values in range [0, 1]
   - Grayscale: [H, W, 1]
   - RGB: [H, W, 3]
   - RGBA images: Alpha channel discarded, becomes RGB
   
   Supported Formats:
   Automatically detected from file extension:
   - **.png**: Portable Network Graphics (lossless)
   - **.jpg, .jpeg**: JPEG (lossy, quality depends on file)
   - **.bmp**: Bitmap (uncompressed)
   - **.tif, .tiff**: Tagged Image Format (lossless)
   - **.gif**: Graphics Interchange Format (first frame only)
   - **.ppm, .pgm, .pbm**: Portable pixmap formats
   - **.tga**: Truevision Targa
   - **.exr**: OpenEXR (high dynamic range)
   - **.hdr**: Radiance HDR
   - **.ico**: Icon format
   - **.jp2**: JPEG 2000
   - And many more via FreeImage
   
   Color Space Handling:
   
   When isColor=false (Grayscale):
   - RGB images converted to grayscale using ITU-R BT.709 formula:
     Gray = 0.2126*R + 0.7152*G + 0.0722*B
   - Already grayscale images loaded directly
   - Result: [H, W, 1] array
   
   When isColor=true (Color):
   - RGB images loaded as [H, W, 3]
   - Grayscale images replicated to 3 channels
   - RGBA alpha channel discarded
   - Result: [H, W, 3] array
   
   Bit Depth Conversion:
   Input bit depths automatically converted to float:
   - 8-bit (0-255): value / 255.0 → [0, 1]
   - 16-bit (0-65535): value / 65535.0 → [0, 1]
   - 32-bit float: Used directly, should be in [0, 1]
   - Signed types: Converted to float with appropriate scaling
   
   Memory Layout:
   ArrayFire uses planar (channel-separated) format:
   ```
   Interleaved (typical):  RGBRGBRGB...
   Planar (ArrayFire):     RRR...GGG...BBB...
   ```
   
   This is optimal for GPU computation but differs from most image libraries.
   
   Performance:
   - I/O bound operation (disk read dominates)
   - Decompression (JPEG, PNG) adds overhead
   - Large images may take seconds to load
   - Consider caching for repeated access
   - SSD vs HDD makes significant difference
   
   Example (Grayscale):
   ```clojure
   (let [out-ptr (mem/alloc-pointer ::mem/pointer)
         filename (mem/c-string \"input.png\")
         err (af-load-image out-ptr filename 0)]  ; 0 = grayscale
     (when (zero? err)
       (let [img-handle (mem/read-pointer out-ptr ::mem/pointer)]
         ;; Process img-handle
         (af-release-array img-handle)))
     (mem/release! filename))
   ```
   
   Example (Color):
   ```clojure
   (let [out-ptr (mem/alloc-pointer ::mem/pointer)
         filename (mem/c-string \"photo.jpg\")
         err (af-load-image out-ptr filename 1)]  ; 1 = RGB
     (when (zero? err)
       (let [img-handle (mem/read-pointer out-ptr ::mem/pointer)]
         ;; Process RGB image
         (af-release-array img-handle)))
     (mem/release! filename))
   ```
   
   Error Conditions:
   - File not found: Returns AF_ERR_RUNTIME
   - Unsupported format: Returns AF_ERR_LOAD_LIB
   - Corrupted file: Returns AF_ERR_LOAD_LIB
   - Out of memory: Returns AF_ERR_NO_MEM
   - FreeImage not available: Returns AF_ERR_NOT_CONFIGURED
   
   Common Issues:
   - **File path**: Use absolute paths or correct relative paths
   - **File permissions**: Ensure read access
   - **Format support**: Not all formats available on all platforms
   - **Large files**: May exhaust GPU memory
   - **Color mode**: Verify isColor matches your needs
   
   Use Cases:
   - Load photographs for processing
   - Read medical images (DICOM often saved as TIFF)
   - Import training data for ML
   - Load reference images for comparison
   - Batch processing image datasets
   
   Best Practices:
   - Always check return code before using output
   - Release array with af-release-array when done
   - Use appropriate color mode (grayscale when possible)
   - Consider native loading for speed (af-load-image-native)
   - Handle errors gracefully (file may not exist)
   
   Returns:
   ArrayFire error code (af_err enum)
   - 0 (AF_SUCCESS): Image loaded successfully
   - Non-zero: Error occurred, check error code
   
   See also:
   - af-save-image: Save image to file
   - af-load-image-memory: Load from memory buffer
   - af-load-image-native: Load without type conversion (imageio2.cpp)
   
   Reference:
   src/api/c/imageio.cpp in ArrayFire source"
  "af_load_image" [::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_save_image(const char* filename, const af_array in)
(defcfn af-save-image
  "Save an image array to a file.
   
   Saves an ArrayFire array as an image file using FreeImage library.
   Format is automatically determined from file extension.
   
   Parameters:
   - filename: pointer to null-terminated string (output file path)
   - in: array handle containing image data
   
   Input Array Requirements:
   - Dimensions: [height, width, channels] or [height, width]
   - Channels: 1 (grayscale), 3 (RGB), or 4 (RGBA)
   - Type: Any numeric type (converted to appropriate format)
   - Values: Should be in range [0, 1] for float types
   
   Type Conversion on Save:
   Float arrays (f32, f64) in [0, 1] converted to:
   - **PNG, BMP, JPEG**: u8 (multiply by 255)
   - **16-bit TIFF**: u16 (multiply by 65535)
   - **EXR, HDR**: float (no conversion, preserves HDR)
   
   Integer arrays saved directly:
   - u8, s8: Saved as 8-bit
   - u16, s16: Saved as 16-bit (if format supports)
   - Values outside [0, 255] or [0, 65535] will be clamped
   
   Output Format Selection:
   Determined by file extension:
   
   - **.png**: PNG (lossless, good compression)
     * Best for: Screenshots, graphics, lossless storage
     * Size: Medium
     * Speed: Moderate (compression overhead)
   
   - **.jpg, .jpeg**: JPEG (lossy compression)
     * Best for: Photographs, natural images
     * Size: Small
     * Speed: Fast
     * Note: Quality fixed (no parameter in this API)
     * Loses some detail (compression artifacts)
   
   - **.bmp**: Bitmap (uncompressed)
     * Best for: Quick saves, no compression needed
     * Size: Large
     * Speed: Very fast
     * No quality loss
   
   - **.tif, .tiff**: TIFF (flexible format)
     * Best for: Archival, 16-bit images
     * Size: Variable (compressed or uncompressed)
     * Speed: Moderate
     * Supports 8-bit and 16-bit
   
   - **.exr**: OpenEXR (HDR format)
     * Best for: High dynamic range images
     * Size: Large
     * Speed: Slow (complex format)
     * Preserves float data exactly
     * Used in visual effects, CGI
   
   - **.hdr**: Radiance HDR
     * Best for: HDR images, lighting
     * Size: Medium
     * Speed: Moderate
     * Float-based format
   
   - **.ppm**: Portable Pixmap (simple format)
     * Best for: Testing, simple I/O
     * Size: Large (ASCII or binary)
     * Speed: Fast
     * Easy to parse
   
   Channel Handling:
   - 1 channel: Saved as grayscale
   - 3 channels: Saved as RGB
   - 4 channels: Saved as RGBA (if format supports)
     * PNG, TIFF, EXR support alpha
     * JPEG does not (alpha discarded)
   
   Memory Layout Conversion:
   ArrayFire's planar format automatically converted to interleaved:
   - Input (planar): [RRR...GGG...BBB...]
   - Output (interleaved): [RGBRGBRGB...]
   - This conversion is automatic and transparent
   
   Performance:
   - I/O bound (disk write + compression)
   - PNG: Slower (compression overhead)
   - JPEG: Fast (optimized compression)
   - BMP: Fastest (no compression)
   - Large images take more time
   - SSD significantly faster than HDD
   
   Example (Save PNG):
   ```clojure
   (let [img-handle (create-processed-image)
         filename (mem/c-string \"output.png\")
         err (af-save-image filename img-handle)]
     (when-not (zero? err)
       (println \"Error saving image:\" err))
     (mem/release! filename)
     (af-release-array img-handle))
   ```
   
   Example (Save JPEG):
   ```clojure
   (let [photo (load-and-enhance \"input.jpg\")
         output (mem/c-string \"enhanced.jpg\")
         err (af-save-image output photo)]
     (println (if (zero? err) \"Saved successfully\" \"Save failed\"))
     (mem/release! output)
     (af-release-array photo))
   ```
   
   Example (Save HDR):
   ```clojure
   ;; Save float data as EXR (preserves full precision)
   (let [hdr-img (tone-mapping scene)
         filename (mem/c-string \"render.exr\")
         err (af-save-image filename hdr-img)]
     (mem/release! filename)
     (af-release-array hdr-img))
   ```
   
   Value Range Handling:
   - Float [0, 1]: Ideal, saves correctly
   - Float [0, 255]: Will be clamped to [0, 1] then scaled
   - Negative values: Clamped to 0
   - Values > 1 (for float): Clamped to 1 (except HDR formats)
   
   Error Conditions:
   - Invalid path: Returns AF_ERR_RUNTIME
   - No write permission: Returns AF_ERR_RUNTIME
   - Disk full: Returns AF_ERR_RUNTIME
   - Invalid array dimensions: Returns AF_ERR_SIZE
   - FreeImage not available: Returns AF_ERR_NOT_CONFIGURED
   - Unsupported channel count: Returns AF_ERR_ARG
   
   Common Issues:
   - **Overwrite**: Existing files silently overwritten
   - **Directory**: Parent directory must exist
   - **Permissions**: Ensure write access
   - **Format support**: Some formats platform-specific
   - **Quality**: JPEG quality fixed, use external tools for control
   - **Range**: Verify values in [0, 1] for float arrays
   
   Use Cases:
   - Save processed images
   - Export results for visualization
   - Generate thumbnails
   - Create image datasets
   - Save intermediate results
   - Format conversion (load one, save another)
   
   Best Practices:
   - Check return code for errors
   - Use appropriate format for use case
   - PNG for lossless, JPEG for photos
   - EXR for HDR/float precision
   - Ensure values in correct range before saving
   - Consider native save for speed (af-save-image-native)
   
   Format Recommendations:
   - **Archival/Quality**: PNG or TIFF (16-bit)
   - **Web/Email**: JPEG (small size)
   - **Processing Pipeline**: EXR (preserves precision)
   - **Quick Save/Load**: BMP (fastest)
   - **With Alpha**: PNG or EXR
   
   Returns:
   ArrayFire error code (af_err enum)
   - 0 (AF_SUCCESS): Image saved successfully
   - Non-zero: Error occurred, check error code
   
   See also:
   - af-load-image: Load image from file
   - af-save-image-memory: Save to memory buffer
   - af-save-image-native: Save without type conversion (imageio2.cpp)
   
   Reference:
   src/api/c/imageio.cpp in ArrayFire source"
  "af_save_image" [::mem/pointer ::mem/pointer] ::mem/int)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; Memory I/O
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; af_err af_load_image_memory(af_array *out, const void* ptr)
(defcfn af-load-image-memory
  "Load an image from a memory buffer.
   
   Loads an image from a FreeImage FIMEMORY structure already in memory.
   Useful for loading images from network streams, embedded resources,
   or avoiding disk I/O.
   
   Parameters:
   - out: out pointer for resulting image array
   - ptr: void pointer to FreeImage FIMEMORY structure
   
   Memory Buffer (FIMEMORY):
   The ptr parameter must be a valid FreeImage FIMEMORY pointer created by:
   - FreeImage_OpenMemory() with image data
   - af-save-image-memory() (creating buffer for save)
   - External FreeImage library calls
   
   This is NOT a raw byte buffer - it must be a FreeImage memory object.
   
   Output Array Format:
   Same as af-load-image:
   - Dimensions: [height, width, channels]
   - Type: f32 (float) with values in [0, 1]
   - Channels based on image content (1, 3, or 4)
   - Bit depth converted to float
   
   Format Detection:
   Image format automatically detected from memory buffer content:
   - Examines magic bytes at start of buffer
   - Supports all formats that FreeImage recognizes
   - Does not rely on file extension
   
   Color Handling:
   Unlike af-load-image, no isColor parameter:
   - Color images loaded as RGB (3 channels)
   - Grayscale images loaded as grayscale (1 channel)
   - RGBA images loaded with alpha (4 channels)
   
   The function preserves the image's natural channel count.
   
   Performance:
   - Faster than file I/O (no disk access)
   - Still has decompression overhead for JPEG, PNG
   - Ideal for images already in memory
   - Network streaming use case
   - Embedded image data
   
   Example (Load from HTTP):
   ```clojure
   ;; Conceptual example - actual FreeImage integration needed
   (let [http-data (download-image \"https://example.com/image.jpg\")
         ;; Create FreeImage memory object from data
         fi-mem (create-freeimage-memory http-data)
         out-ptr (mem/alloc-pointer ::mem/pointer)
         err (af-load-image-memory out-ptr fi-mem)]
     (when (zero? err)
       (let [img (mem/read-pointer out-ptr ::mem/pointer)]
         (process-image img)
         (af-release-array img)))
     (free-freeimage-memory fi-mem))
   ```
   
   Example (Embedded Image):
   ```clojure
   ;; Load image compiled into application
   (let [embedded-data (get-embedded-image-data)
         fi-mem (bytes->freeimage-memory embedded-data)
         out-ptr (mem/alloc-pointer ::mem/pointer)
         err (af-load-image-memory out-ptr fi-mem)]
     (when (zero? err)
       (let [img (mem/read-pointer out-ptr ::mem/pointer)]
         ;; Use embedded image
         (af-release-array img))))
   ```
   
   Memory Management:
   - Input FIMEMORY: Caller must manage (FreeImage_CloseMemory)
   - Output array: Managed by ArrayFire (af-release-array)
   - No automatic cleanup of FIMEMORY
   - Must explicitly free FIMEMORY after loading
   
   Error Conditions:
   - Invalid pointer: Returns AF_ERR_ARG
   - Corrupted data: Returns AF_ERR_LOAD_LIB
   - Unsupported format: Returns AF_ERR_LOAD_LIB
   - Out of memory: Returns AF_ERR_NO_MEM
   - FreeImage not available: Returns AF_ERR_NOT_CONFIGURED
   
   Use Cases:
   - Load images from HTTP responses
   - Read images from zip archives
   - Decode images from databases
   - Process embedded resources
   - Avoid disk I/O overhead
   - Stream processing of images
   
   Advantages over File I/O:
   - No file system access needed
   - Faster (no disk seek/read)
   - Works with in-memory data
   - Network streaming friendly
   - Embedded resource support
   
   Integration Notes:
   Requires FreeImage library integration:
   ```c
   // C example of creating FIMEMORY
   FIMEMORY *mem = FreeImage_OpenMemory(data_ptr, data_size);
   af_array img;
   af_load_image_memory(&img, mem);
   FreeImage_CloseMemory(mem);
   ```
   
   From Clojure, you need to:
   1. Use FFI to call FreeImage functions
   2. Create FIMEMORY from byte buffer
   3. Pass to af-load-image-memory
   4. Free FIMEMORY when done
   
   Returns:
   ArrayFire error code (af_err enum)
   - 0 (AF_SUCCESS): Image loaded successfully
   - Non-zero: Error occurred
   
   See also:
   - af-load-image: Load from file
   - af-save-image-memory: Save to memory buffer
   - af-delete-image-memory: Free memory buffer
   - FreeImage library documentation for FIMEMORY
   
   Reference:
   src/api/c/imageio.cpp in ArrayFire source"
  "af_load_image_memory" [::mem/pointer ::mem/pointer] ::mem/int)

;; af_err af_save_image_memory(void** ptr, const af_array in, const af_image_format format)
(defcfn af-save-image-memory
  "Save an image array to a memory buffer.
   
   Encodes an image and stores it in a FreeImage FIMEMORY structure.
   Returns a pointer to the memory buffer which can be used for
   network transmission, storage, or further processing.
   
   Parameters:
   - ptr: out pointer to receive FIMEMORY pointer (void**)
   - in: array handle containing image data
   - format: image format enum (af_image_format)
   
   Image Formats (af_image_format enum):
   - AF_FIF_BMP (0): Windows Bitmap
   - AF_FIF_ICO (1): Windows Icon
   - AF_FIF_JPEG (2): JPEG (lossy)
   - AF_FIF_JNG (3): JPEG Network Graphics
   - AF_FIF_PNG (13): Portable Network Graphics (lossless)
   - AF_FIF_TIFF (18): Tagged Image Format
   - AF_FIF_GIF (25): Graphics Interchange Format
   - AF_FIF_EXR (29): OpenEXR (HDR)
   - AF_FIF_JP2 (30): JPEG 2000
   - Many others - see af/defines.h for complete list
   
   Most commonly used:
   - PNG (13): Lossless, good compression
   - JPEG (2): Lossy, small size, photos
   - BMP (0): Uncompressed, fast
   - EXR (29): HDR, float precision
   
   Output Buffer:
   - Returns FIMEMORY pointer via ptr parameter
   - Buffer contains encoded image data
   - Must be freed with af-delete-image-memory
   - Can be written to network, file, database
   
   Input Array Requirements:
   Same as af-save-image:
   - Dimensions: [height, width, channels] or [height, width]
   - Channels: 1 (gray), 3 (RGB), or 4 (RGBA)
   - Type: Any numeric type
   - Values: [0, 1] range for float types
   
   Type Conversion:
   Same as file save:
   - Float [0, 1] → u8 (multiply by 255) for PNG, JPEG, BMP
   - Float [0, 1] → u16 for 16-bit TIFF
   - Float preserved for EXR
   - Integer types saved directly
   
   Memory Buffer Access:
   After saving, access buffer data:
   ```c
   // C example
   void *mem_ptr;
   af_save_image_memory(&mem_ptr, img, AF_FIF_PNG);
   
   // Get buffer size and data
   FIMEMORY *fimem = (FIMEMORY*)mem_ptr;
   BYTE *data;
   DWORD size;
   FreeImage_AcquireMemory(fimem, &data, &size);
   
   // Use data (e.g., send over network)
   send_bytes(data, size);
   
   // Cleanup
   af_delete_image_memory(mem_ptr);
   ```
   
   Performance:
   - Faster than file I/O (no disk)
   - Compression overhead still present
   - JPEG: Fast encoding
   - PNG: Moderate (compression)
   - BMP: Fastest (no compression)
   - EXR: Slow (complex format)
   
   Example (Save to PNG Buffer):
   ```clojure
   (let [img-handle (create-image)
         ptr-ptr (mem/alloc-pointer ::mem/pointer)
         ;; AF_FIF_PNG = 13
         err (af-save-image-memory ptr-ptr img-handle 13)]
     (when (zero? err)
       (let [fimem-ptr (mem/read-pointer ptr-ptr ::mem/pointer)]
         ;; Extract data from FIMEMORY and use it
         (let [img-bytes (fimem->bytes fimem-ptr)]
           (send-to-network img-bytes)
           ;; Must free the memory
           (af-delete-image-memory fimem-ptr))))
     (af-release-array img-handle))
   ```
   
   Example (Save to JPEG Buffer):
   ```clojure
   (let [photo (process-photo original)
         ptr-ptr (mem/alloc-pointer ::mem/pointer)
         ;; AF_FIF_JPEG = 2
         err (af-save-image-memory ptr-ptr photo 2)]
     (when (zero? err)
       (let [jpeg-buffer (mem/read-pointer ptr-ptr ::mem/pointer)]
         ;; Upload to server
         (http-post-image jpeg-buffer)
         ;; Cleanup
         (af-delete-image-memory jpeg-buffer)))
     (af-release-array photo))
   ```
   
   Format Selection Guidelines:
   - **Network transmission**: JPEG (small) or PNG (quality)
   - **Database storage**: PNG or JPEG
   - **Temporary cache**: BMP (fast)
   - **HDR data**: EXR
   - **Lossless need**: PNG or TIFF
   
   Memory Management:
   Critical: Always free the returned buffer!
   ```clojure
   (let [ptr-ptr (mem/alloc-pointer ::mem/pointer)
         err (af-save-image-memory ptr-ptr img format)]
     (when (zero? err)
       (let [mem-ptr (mem/read-pointer ptr-ptr ::mem/pointer)]
         (try
           ;; Use memory buffer
           (use-buffer mem-ptr)
           (finally
             ;; MUST call delete-image-memory
             (af-delete-image-memory mem-ptr))))))
   ```
   
   Buffer Lifetime:
   - Created by af-save-image-memory
   - Owned by caller after creation
   - Persists until af-delete-image-memory
   - Not managed by ArrayFire's resource tracking
   - Manual cleanup required
   
   Error Conditions:
   - Invalid format: Returns AF_ERR_ARG
   - Invalid array: Returns AF_ERR_ARG
   - Out of memory: Returns AF_ERR_NO_MEM
   - Encoding failure: Returns AF_ERR_RUNTIME
   - FreeImage not available: Returns AF_ERR_NOT_CONFIGURED
   
   Use Cases:
   - Upload images to servers
   - Store in databases as BLOBs
   - Send over network sockets
   - In-memory image caching
   - IPC (inter-process communication)
   - Embedding in documents
   
   Integration with FreeImage:
   The returned pointer is a FreeImage FIMEMORY:
   ```c
   // Access buffer data
   FIMEMORY *mem = (FIMEMORY*)ptr;
   BYTE *data;
   DWORD size;
   FreeImage_AcquireMemory(mem, &data, &size);
   
   // Copy data
   memcpy(destination, data, size);
   
   // Cleanup via ArrayFire
   af_delete_image_memory(ptr);
   ```
   
   Common Patterns:
   ```clojure
   ;; Pattern 1: Send over HTTP
   (defn upload-image [img url]
     (let [ptr-ptr (mem/alloc-pointer ::mem/pointer)
           err (af-save-image-memory ptr-ptr img 2)] ; JPEG
       (when (zero? err)
         (let [mem-ptr (mem/read-pointer ptr-ptr ::mem/pointer)]
           (try
             (http-post url (fimem->bytes mem-ptr))
             (finally
               (af-delete-image-memory mem-ptr)))))))
   
   ;; Pattern 2: Cache in memory
   (defn cache-image [img]
     (let [ptr-ptr (mem/alloc-pointer ::mem/pointer)
           err (af-save-image-memory ptr-ptr img 13)] ; PNG
       (when (zero? err)
         (let [mem-ptr (mem/read-pointer ptr-ptr ::mem/pointer)]
           ;; Store mem-ptr in cache
           ;; Remember to delete when evicted
           mem-ptr))))
   ```
   
   Returns:
   ArrayFire error code (af_err enum)
   - 0 (AF_SUCCESS): Buffer created successfully
   - Non-zero: Error occurred
   
   See also:
   - af-save-image: Save to file
   - af-load-image-memory: Load from memory buffer
   - af-delete-image-memory: Free the returned buffer (REQUIRED!)
   
   Reference:
   src/api/c/imageio.cpp in ArrayFire source"
  "af_save_image_memory" [::mem/pointer ::mem/pointer ::mem/int] ::mem/int)

;; af_err af_delete_image_memory(void* ptr)
(defcfn af-delete-image-memory
  "Free a memory buffer created by af-save-image-memory.
   
   Releases the FreeImage FIMEMORY structure allocated by
   af-save-image-memory. This must be called for every buffer
   created to prevent memory leaks.
   
   Parameters:
   - ptr: FIMEMORY pointer returned by af-save-image-memory
   
   Critical Notes:
   - MUST be called for every af-save-image-memory call
   - Only use with pointers from af-save-image-memory
   - Do NOT use with other types of pointers
   - Do NOT call multiple times on same pointer
   - Do NOT use pointer after deletion
   
   Memory Management:
   This function:
   - Frees the FreeImage FIMEMORY structure
   - Releases all associated image data
   - Invalidates the pointer (do not use after)
   - Does NOT affect the original ArrayFire array
   
   Usage Pattern:
   Always pair with af-save-image-memory:
   ```clojure
   (let [ptr-ptr (mem/alloc-pointer ::mem/pointer)
         err (af-save-image-memory ptr-ptr img format)]
     (when (zero? err)
       (let [mem-ptr (mem/read-pointer ptr-ptr ::mem/pointer)]
         (try
           ;; Use the memory buffer
           (process-buffer mem-ptr)
           (finally
             ;; ALWAYS cleanup
             (af-delete-image-memory mem-ptr))))))
   ```
   
   Example (Network Upload):
   ```clojure
   (defn upload-processed-image [img]
     (let [ptr-ptr (mem/alloc-pointer ::mem/pointer)
           err (af-save-image-memory ptr-ptr img 2)] ; JPEG
       (when (zero? err)
         (let [buffer-ptr (mem/read-pointer ptr-ptr ::mem/pointer)]
           (try
             ;; Extract bytes and send
             (let [bytes (extract-buffer-bytes buffer-ptr)]
               (http-post \"https://api.example.com/upload\" bytes))
             (finally
               ;; Critical: free the buffer
               (af-delete-image-memory buffer-ptr)))))))
   ```
   
   Example (Database Storage):
   ```clojure
   (defn store-image-in-db [img record-id]
     (let [ptr-ptr (mem/alloc-pointer ::mem/pointer)
           err (af-save-image-memory ptr-ptr img 13)] ; PNG
       (when (zero? err)
         (let [buffer-ptr (mem/read-pointer ptr-ptr ::mem/pointer)]
           (try
             (let [blob-data (fimem->blob buffer-ptr)]
               (db-insert! :images {:id record-id :data blob-data}))
             (finally
               ;; Don't leak memory
               (af-delete-image-memory buffer-ptr)))))))
   ```
   
   Memory Leak Prevention:
   Without calling this function:
   ```clojure
   ;; BAD - Memory leak!
   (defn leak-memory [img]
     (let [ptr-ptr (mem/alloc-pointer ::mem/pointer)]
       (af-save-image-memory ptr-ptr img 2)
       ;; Memory never freed - leaks!
       nil))
   
   ;; GOOD - Proper cleanup
   (defn no-leak [img]
     (let [ptr-ptr (mem/alloc-pointer ::mem/pointer)
           err (af-save-image-memory ptr-ptr img 2)]
       (when (zero? err)
         (let [mem-ptr (mem/read-pointer ptr-ptr ::mem/pointer)]
           (try
             (use-buffer mem-ptr)
             (finally
               (af-delete-image-memory mem-ptr)))))))
   ```
   
   Error Handling:
   Function is quite forgiving:
   - NULL pointer: Safely ignored
   - Invalid pointer: May crash (avoid!)
   - Double free: Undefined behavior (avoid!)
   
   Always ensure:
   - Pointer came from af-save-image-memory
   - Called exactly once per buffer
   - Not used after deletion
   
   Common Mistakes:
   1. **Forgetting to call**: Memory leak
   2. **Calling twice**: Crash or corruption
   3. **Using after delete**: Undefined behavior
   4. **Wrong pointer**: Crash
   
   Best Practices:
   ```clojure
   ;; Use try-finally for guaranteed cleanup
   (let [ptr-ptr (mem/alloc-pointer ::mem/pointer)]
     (when (zero? (af-save-image-memory ptr-ptr img format))
       (let [mem-ptr (mem/read-pointer ptr-ptr ::mem/pointer)]
         (try
           (do-something-with mem-ptr)
           (finally
             (af-delete-image-memory mem-ptr))))))
   
   ;; Or use with-open style macro
   (defmacro with-image-memory [[binding img format] & body]
     `(let [ptr-ptr# (mem/alloc-pointer ::mem/pointer)]
        (when (zero? (af-save-image-memory ptr-ptr# ~img ~format))
          (let [~binding (mem/read-pointer ptr-ptr# ::mem/pointer)]
            (try
              ~@body
              (finally
                (af-delete-image-memory ~binding)))))))
   ```
   
   Integration Note:
   Internally calls FreeImage_CloseMemory:
   ```c
   // ArrayFire implementation
   void af_delete_image_memory(void* ptr) {
       if (ptr) {
           FIMEMORY* mem = (FIMEMORY*)ptr;
           FreeImage_CloseMemory(mem);
       }
   }
   ```
   
   Performance:
   - Very fast operation
   - No significant overhead
   - Immediate memory release
   - No background cleanup needed
   
   Use Cases:
   - Every af-save-image-memory call
   - Cleanup after network transmission
   - Cache eviction
   - Error recovery paths
   - Normal execution paths
   
   Returns:
   ArrayFire error code (af_err enum)
   - 0 (AF_SUCCESS): Buffer freed successfully
   - Non-zero: Error occurred (rare)
   
   See also:
   - af-save-image-memory: Creates buffer (must pair with this)
   - af-load-image-memory: Loads from memory buffer
   
   Reference:
   src/api/c/imageio.cpp in ArrayFire source"
  "af_delete_image_memory" [::mem/pointer] ::mem/int)
