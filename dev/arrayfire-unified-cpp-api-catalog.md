# ArrayFire Unified C++ API Catalog

This document catalogs the ArrayFire Unified C++ API as defined in the header files in `reference/arrayfire/include/af/`.

Focus: C++ interface (namespace `af`), not C interface.

## Core Data Structures

### array.h - Array Class & Core Operations
**Categories:**
- Array construction and initialization
- Memory management
- Type conversion and casting
- Indexing and slicing
- Array properties query

**Key Functions:**
- **Constructors:** `array()`, `array(dim_t dim0, dtype)`, `array(dim_t dim0, dim_t dim1, dtype)`, etc.
- **From pointer:** `array(dim_t, T*, af_source)`
- **Properties:** `dims()`, `elements()`, `type()`, `numdims()`, `bytes()`, `allocated()`
- **Query:** `isempty()`, `isscalar()`, `isvector()`, `isrow()`, `iscolumn()`, `iscomplex()`, `isreal()`, `isdouble()`, `issingle()`, `ishalf()`, `isinteger()`, `isbool()`, `issparse()`
- **Memory:** `host()`, `device()`, `copy()`, `lock()`, `unlock()`, `isLocked()`, `eval()`
- **Type conversion:** `as(dtype)`
- **Transpose:** `T()`, `H()`
- **Indexing:** `operator()`, `row()`, `rows()`, `col()`, `cols()`, `slice()`, `slices()`

**Operators:**
- `()` - indexing operator
- `=` - assignment
- `+=`, `-=`, `*=`, `/=` - compound assignment (through array_proxy)

### complex.h - Complex Number Support
**Categories:**
- Complex number creation
- Complex arithmetic
- Complex number utilities

**Key Functions:**
- `real(cfloat)`, `real(cdouble)` - extract real part
- `imag(cfloat)`, `imag(cdouble)` - extract imaginary part
- `abs(cfloat)`, `abs(cdouble)` - absolute value
- `conj(cfloat)`, `conj(cdouble)` - conjugate

**Operators:**
- `+`, `-`, `*`, `/` - arithmetic between complex numbers
- `+`, `/`, `-`, `*` - arithmetic between complex and double
- `==`, `!=` - equality comparison
- `<<`, `>>` - stream I/O

### dim4.hpp - Dimension Specification
**Categories:**
- Multi-dimensional size specification

---

## Arithmetic & Mathematical Operations

### arith.h - Arithmetic Operations
**Categories:**
- Basic arithmetic
- Comparison operations
- Trigonometric functions
- Hyperbolic functions
- Exponential & logarithmic functions
- Complex number operations
- Rounding functions
- Special functions
- Bitwise operations
- Logical operations

**Key Functions:**

**Element-wise min/max:**
- `min(array, array)`, `min(array, double)`, `min(double, array)`
- `max(array, array)`, `max(array, double)`, `max(double, array)`
- `clamp(array, array, array)` - clamp between limits

**Arithmetic:**
- `rem(array, array)` - remainder
- `mod(array, array)` - modulus
- `abs(array)` - absolute value
- `arg(array)` - phase angle
- `sign(array)` - sign function
- `hypot(array, array)` - hypotenuse

**Rounding:**
- `round(array)` - round to nearest integer
- `trunc(array)` - truncate
- `floor(array)` - floor
- `ceil(array)` - ceiling

**Trigonometric:**
- `sin(array)`, `cos(array)`, `tan(array)`
- `asin(array)`, `acos(array)`, `atan(array)`, `atan2(array, array)`

**Hyperbolic:**
- `sinh(array)`, `cosh(array)`, `tanh(array)`
- `asinh(array)`, `acosh(array)`, `atanh(array)`

**Complex:**
- `complex(array)`, `complex(array, array)` - create complex array
- `real(array)` - extract real part
- `imag(array)` - extract imaginary part
- `conjg(array)` - complex conjugate

**Exponential & Logarithmic:**
- `exp(array)`, `expm1(array)` - exponential
- `log(array)`, `log1p(array)`, `log10(array)`, `log2(array)` - logarithms
- `pow(array, array)`, `pow2(array)` - power functions
- `root(array, array)` - nth root
- `sqrt(array)`, `rsqrt(array)`, `cbrt(array)` - roots
- `sigmoid(array)` - logistic sigmoid

**Special functions:**
- `erf(array)`, `erfc(array)` - error functions
- `factorial(array)` - factorial
- `tgamma(array)`, `lgamma(array)` - gamma functions

**Checks:**
- `iszero(array)` - check for zeros
- `isInf(array)` - check for infinity
- `isNaN(array)` - check for NaN

**Bitwise operations (C interface via af_ functions):**
- `af_bitand`, `af_bitor`, `af_bitxor`, `af_bitnot`
- `af_bitshiftl`, `af_bitshiftr`

**Logical operations (C interface via af_ functions):**
- `af_and`, `af_or`, `af_not`

**Comparison operations (C interface via af_ functions):**
- `af_lt`, `af_gt`, `af_le`, `af_ge`, `af_eq`, `af_neq`

---

## Linear Algebra

### blas.h - Basic Linear Algebra
**Categories:**
- Matrix multiplication
- Dot product
- Matrix transpose

**Key Functions:**
- `matmul(array, array, matProp, matProp)` - matrix multiplication
- `matmulNT(array, array)` - multiply with transposed second matrix
- `matmulTN(array, array)` - multiply with transposed first matrix
- `matmulTT(array, array)` - multiply both transposed
- `matmul(array, array, array)` - chain multiply 3 matrices
- `matmul(array, array, array, array)` - chain multiply 4 matrices
- `gemm(af_array*, matProp, matProp, void*, array, array, void*)` - general matrix multiply (BLAS GEMM)
- `dot(array, array, matProp, matProp)` - dot product
- `transpose(array, bool)` - transpose
- `transposeInPlace(array, bool)` - in-place transpose

### lapack.h - Advanced Linear Algebra
**Categories:**
- Matrix decomposition
- Linear system solvers
- Matrix properties

**Key Functions:**

**Decompositions:**
- `svd(array&, array&, array&, array)` - SVD decomposition
- `svdInPlace(array&, array&, array&, array&)` - in-place SVD
- `lu(array&, array&, array, bool)` - LU decomposition
- `lu(array&, array&, array&, array)` - LU decomposition (separate L,U)
- `luInPlace(array&, array, bool)` - in-place LU
- `qr(array&, array&, array)` - QR decomposition
- `qr(array&, array&, array&, array)` - QR decomposition (separate Q,R)
- `qrInPlace(array&, array)` - in-place QR
- `cholesky(array&, array, bool)` - Cholesky decomposition
- `choleskyInPlace(array, bool)` - in-place Cholesky

**Solvers:**
- `solve(array, array, matProp)` - solve linear system
- `solveLU(array, array, array, matProp)` - solve using LU decomposition
- `inverse(array, matProp)` - matrix inverse
- `pinverse(array, double, matProp)` - pseudo-inverse (Moore-Penrose)

**Properties:**
- `rank(array, double)` - matrix rank
- `det<T>(array)` - determinant
- `norm(array, normType, double, double)` - matrix norm
- `isLAPACKAvailable()` - check LAPACK support

---

## Data Generation & Manipulation

### data.h - Data Generation & Manipulation
**Categories:**
- Array initialization
- Array manipulation
- Indexing operations

**Key Functions:**

**Initialization:**
- `constant<T>(T, dim4, dtype)` - constant array
- `identity(dim4, dtype)` - identity matrix
- `range(dim4, int, dtype)` - range array
- `iota(dim4, dim4, dtype)` - iota array

**Manipulation:**
- `diag(array, int, bool)` - extract/create diagonal
- `join(int, array, array)` - join arrays
- `join(int, array, array, array)` - join 3 arrays
- `join(int, array, array, array, array)` - join 4 arrays
- `tile(array, unsigned, unsigned, unsigned, unsigned)` - tile array
- `tile(array, dim4)` - tile array
- `reorder(array, unsigned, unsigned, unsigned, unsigned)` - reorder dimensions
- `shift(array, int, int, int, int)` - shift array
- `moddims(array, dim4)` - modify dimensions
- `flat(array)` - flatten array
- `flip(array, unsigned)` - flip array
- `lower(array, bool)` - lower triangular
- `upper(array, bool)` - upper triangular
- `select(array, array, array)` - select elements
- `replace(array, array, array)` - replace elements

### index.h - Advanced Indexing
**Categories:**
- Array indexing
- Lookup operations
- Index assignment

**Key Functions:**
- `lookup(array, array, int)` - lookup values by index
- `copy(array&, array, index, index, index, index)` - copy with indexing

**Classes:**
- `index` - wrapper for indexing operations
  - Constructors from `int`, `seq`, `af_seq`, `array`
  - `isspan()` - check if represents span
  - `get()` - get underlying af_index_t

---

## Signal Processing

### signal.h - Signal Processing
**Categories:**
- Interpolation
- FFT operations
- Filtering
- Convolution

**Key Functions:**

**Interpolation:**
- `approx1(array, array, interpType, float)` - 1D interpolation
- `approx2(array, array, array, interpType, float)` - 2D interpolation
- `approx1(array, array, int, double, double, interpType, float)` - 1D with uniform spacing
- `approx2(array, array, int, double, double, array, int, double, double, interpType, float)` - 2D with uniform spacing

**FFT:**
- `fft(array, dim_t)` - 1D FFT
- `fft2(array, dim_t, dim_t)` - 2D FFT
- `fft3(array, dim_t, dim_t, dim_t)` - 3D FFT
- `fftNorm(array, double, dim_t)` - 1D FFT with normalization
- `fft2Norm(array, double, dim_t, dim_t)` - 2D FFT with normalization
- `fft3Norm(array, double, dim_t, dim_t, dim_t)` - 3D FFT with normalization
- `fftInPlace(array, double)` - in-place 1D FFT
- `fft2InPlace(array, double)` - in-place 2D FFT
- `fft3InPlace(array, double)` - in-place 3D FFT
- `dft(array)`, `dft(array, dim4)`, `dft(array, double, dim4)` - discrete Fourier transform

**Inverse FFT:**
- `ifft(array, dim_t)` - 1D inverse FFT
- `ifft2(array, dim_t, dim_t)` - 2D inverse FFT
- `ifft3(array, dim_t, dim_t, dim_t)` - 3D inverse FFT
- `ifftNorm(array, double, dim_t)` - 1D inverse FFT with normalization
- `ifft2Norm(array, double, dim_t, dim_t)` - 2D inverse FFT with normalization
- `ifft3Norm(array, double, dim_t, dim_t, dim_t)` - 3D inverse FFT with normalization
- `ifftInPlace(array, double)` - in-place 1D inverse FFT
- `ifft2InPlace(array, double)` - in-place 2D inverse FFT
- `ifft3InPlace(array, double)` - in-place 3D inverse FFT

**Convolution:**
- `convolve(array, array, convMode, convDomain)`
- `convolve1(array, array, convMode, convDomain)`
- `convolve2(array, array, convMode, convDomain)`
- `convolve3(array, array, convMode, convDomain)`
- `fftConvolve(array, array, convMode)`
- `fftConvolve1(array, array, convMode)`
- `fftConvolve2(array, array, convMode)`
- `fftConvolve3(array, array, convMode)`

**Filtering:**
- `fir(array, array)`
- `iir(array, array, array)`
- `medfilt(array, dim_t, dim_t, borderType)`
- `medfilt1(array, dim_t, borderType)`
- `medfilt2(array, dim_t, dim_t, borderType)`

---

## Image Processing

### image.h - Image Processing
**Categories:**
- Image I/O
- Geometric transformations
- Filtering
- Feature extraction
- Histogram operations

**Key Functions:**

**Image I/O:**
- `loadImage(char*, bool)` - load image
- `saveImage(char*, array)` - save image
- `loadImageMem(void*)` - load from memory
- `saveImageMem(array, imageFormat)` - save to memory
- `deleteImageMem(void*)` - free memory
- `loadImageNative(char*)` - load preserving type
- `saveImageNative(char*, array)` - save preserving type
- `isImageIOAvailable()` - check support

**Geometric Transformations:**
- `resize(array, dim_t, dim_t, interpType)` - resize to dimensions
- `resize(float, float, array, interpType)` - resize by scale
- `resize(float, array, interpType)` - resize by single scale
- `rotate(array, float, bool, interpType)` - rotate
- `transform(array, array, dim_t, dim_t, interpType, bool)` - transform
- `transformCoordinates(array, float, float)` - transform coordinates
- `translate(array, float, float, dim_t, dim_t, interpType)` - translate
- `scale(array, float, float, dim_t, dim_t, interpType)` - scale
- `skew(array, float, float, dim_t, dim_t, bool, interpType)` - skew

**Filters:**
- `bilateral(array, float, float, bool)` - bilateral filter
- `meanShift(array, float, float, unsigned, bool)` - mean shift
- `minfilt(array, dim_t, dim_t, borderType)` - minimum filter
- `maxfilt(array, dim_t, dim_t, borderType)` - maximum filter
- `dilate(array, array)` - morphological dilation
- `dilate3(array, array)` - 3D dilation
- `erode(array, array)` - morphological erosion
- `erode3(array, array)` - 3D erosion

**Gradients:**
- `grad(array&, array&, array)` - compute gradients

**Histogram:**
- `histogram(array, unsigned)` - compute histogram
- `histogram(array, unsigned, double, double)` - histogram with range
- `histEqual(array, array)` - histogram equalization

**Color Space:**
- `colorSpace(array, cspace, cspace)` - color space conversion
- `gray2rgb(array, float, float, float)` - grayscale to RGB
- `rgb2gray(array, float, float, float)` - RGB to grayscale
- `rgb2hsv(array)` - RGB to HSV
- `hsv2rgb(array)` - HSV to RGB
- `rgb2ycbcr(array, ycbcrStd)` - RGB to YCbCr
- `ycbcr2rgb(array, ycbcrStd)` - YCbCr to RGB

**Regions:**
- `regions(array, connectivity)` - connected components
- `confidence_cc(array, array, array, float, unsigned, float)` - confidence connected

**Edge Detection:**
- `sobel(array&, array&, array, unsigned)` - Sobel operator
- `canny(array, cannyThreshold, float, float, unsigned, bool)` - Canny edge detector
- `anisotropicDiffusion(array, float, float, unsigned, diffusionEq, diffusion)` - anisotropic diffusion

**Morphological Operations:**
- `regions(array, connectivity)` - find regions

---

## Computer Vision

### vision.h - Computer Vision Features
**Categories:**
- Feature detection
- Feature description
- Feature matching
- Image filtering

**Key Functions:**

**Feature Detectors:**
- `fast(array, float, unsigned, bool, float, unsigned)` - FAST corner detector
- `harris(array, unsigned, float, float, unsigned, float)` - Harris corner detector
- `susan(array, unsigned, float, float, float, unsigned)` - SUSAN corner detector

**Feature Descriptors:**
- `orb(features&, array&, array, float, unsigned, float, unsigned, bool)` - ORB descriptor
- `sift(features&, array&, array, unsigned, float, float, float, bool, float, float)` - SIFT descriptor
- `gloh(features&, array&, array, unsigned, float, float, float, bool, float, float)` - GLOH descriptor

**Feature Matching:**
- `hammingMatcher(array&, array&, array, array, dim_t, unsigned)` - Hamming distance matcher
- `nearestNeighbour(array&, array&, array, array, dim_t, unsigned, matchType)` - nearest neighbor matcher
- `matchTemplate(array, array, matchType)` - template matching

**Filters:**
- `dog(array, int, int)` - Difference of Gaussians

### features.h - Feature Objects
**Classes:**
- `features` - container for detected features
  - `getNumFeatures()` - get count
  - `getX()`, `getY()` - get positions
  - `getScore()` - get scores
  - `getOrientation()` - get orientations
  - `getSize()` - get sizes

---

## Statistics & Reductions

### statistics.h - Statistical Functions
**Categories:**
- Central tendency
- Dispersion measures
- Correlation

**Key Functions:**

**Mean:**
- `mean(array, dim_t)` - mean along dimension
- `mean(array, array, dim_t)` - weighted mean
- `mean<T>(array)` - mean of all elements
- `mean<T>(array, array)` - weighted mean of all elements

**Variance & Standard Deviation:**
- `var(array, af_var_bias, dim_t)` - variance
- `var(array, array, dim_t)` - weighted variance
- `var<T>(array, af_var_bias)` - variance of all elements
- `var<T>(array, array)` - weighted variance of all elements
- `stdev(array, af_var_bias, dim_t)` - standard deviation
- `stdev<T>(array, af_var_bias)` - standard deviation of all elements
- `meanvar(array&, array&, array, array, af_var_bias, dim_t)` - compute both mean and variance

**Median:**
- `median(array, dim_t)` - median along dimension
- `median<T>(array)` - median of all elements

**Covariance & Correlation:**
- `cov(array, array, af_var_bias)` - covariance
- `corrcoef<T>(array, array)` - correlation coefficient

### algorithm.h - Reduction & Scan Operations
**Categories:**
- Reductions
- Scans (prefix sums)
- Sorting
- Set operations

**Key Functions:**

**Reductions:**
- `sum(array, int)` - sum along dimension
- `sum(array, int, double)` - sum with NaN replacement
- `sumByKey(array&, array&, array, array, int)` - sum by key
- `product(array, int)` - product along dimension
- `product(array, int, double)` - product with NaN replacement
- `productByKey(array&, array&, array, array, int)` - product by key
- `min(array, int)` - minimum along dimension
- `minByKey(array&, array&, array, array, int)` - minimum by key
- `max(array, int)` - maximum along dimension
- `max(array&, array&, array, array, int)` - ragged maximum
- `maxByKey(array&, array&, array, array, int)` - maximum by key
- `allTrue(array, int)` - check all true
- `allTrueByKey(array&, array&, array, array, int)` - all true by key
- `anyTrue(array, int)` - check any true
- `anyTrueByKey(array&, array&, array, array, int)` - any true by key
- `count(array, int)` - count non-zero elements
- `countByKey(array&, array&, array, array, int)` - count by key

**Scans (Cumulative Operations):**
- `accum(array, int)` - cumulative sum
- `scan(array, int, binaryOp, bool)` - generalized scan
- `scanByKey(array, array, int, binaryOp, bool)` - scan by key

**Finding elements:**
- `where(array)` - find indices of non-zero elements

**Difference:**
- `diff1(array, int)` - first order difference
- `diff2(array, int)` - second order difference

**Sorting:**
- `sort(array, unsigned, bool)` - sort array
- `sort(array&, array&, array, unsigned, bool)` - sort with indices
- `sortByKey(array&, array&, array, array, unsigned, bool)` - sort by key
- `sortIndex(array, unsigned, bool)` - get sort indices
- `setUnique(array, bool)` - unique elements
- `setUnion(array, array, bool)` - set union
- `setIntersect(array, array, bool)` - set intersection

---

## Random Number Generation

### random.h - Random Numbers
**Categories:**
- Random number generators
- Random engine management

**Key Functions:**
- `randu(dim4, dtype)` - uniform random
- `randu(dim4, dtype, randomEngine&)` - uniform with engine
- `randu(dim_t, dtype)` - 1D uniform
- `randu(dim_t, dim_t, dtype)` - 2D uniform
- `randu(dim_t, dim_t, dim_t, dtype)` - 3D uniform
- `randu(dim_t, dim_t, dim_t, dim_t, dtype)` - 4D uniform
- `randn(dim4, dtype)` - normal random
- `randn(dim4, dtype, randomEngine&)` - normal with engine
- `randn(dim_t, dtype)` - 1D normal
- `randn(dim_t, dim_t, dtype)` - 2D normal
- `randn(dim_t, dim_t, dim_t, dtype)` - 3D normal
- `randn(dim_t, dim_t, dim_t, dim_t, dtype)` - 4D normal
- `setDefaultRandomEngineType(randomEngineType)` - set default engine
- `getDefaultRandomEngineType()` - get default engine
- `setSeed(unsigned long long)` - set random seed
- `getSeed()` - get random seed

**Classes:**
- `randomEngine` - random number generator engine
  - `randomEngine(randomEngineType, unsigned long long)` - constructor
  - `setType(randomEngineType)` - set engine type
  - `getType()` - get engine type
  - `setSeed(unsigned long long)` - set seed
  - `getSeed()` - get seed

---

## Sparse Arrays

### sparse.h - Sparse Matrix Operations
**Categories:**
- Sparse array creation
- Sparse array conversion
- Sparse array information

**Key Functions:**

**Creation:**
- `sparse(dim_t, dim_t, array, array, array, storage)` - from arrays
- `sparse(dim_t, dim_t, dim_t, void*, int*, int*, dtype, storage, source)` - from pointers
- `sparse(array, storage)` - from dense array

**Conversion:**
- `sparseConvertTo(array, storage)` - convert storage format
- `dense(array)` - convert to dense

**Information:**
- `sparseGetInfo(array&, array&, array&, storage&, array)` - get all info
- `sparseGetValues(array)` - get values
- `sparseGetRowIdx(array)` - get row indices
- `sparseGetColIdx(array)` - get column indices
- `sparseGetNNZ(array)` - get number of non-zeros
- `sparseGetStorage(array)` - get storage format

---

## Device Management

### device.h - Device & Memory Management
**Categories:**
- Device information
- Device selection
- Memory allocation
- Memory management

**Key Functions:**

**Device Information:**
- `info()` - display device info
- `infoString(bool)` - get device info as string
- `deviceInfo(char*, char*, char*, char*)` - get device properties
- `getDeviceCount()` - get number of devices
- `getDevice()` - get current device
- `isDoubleAvailable(int)` - check double support
- `isHalfAvailable(int)` - check half support
- `setDevice(int)` - set current device
- `sync(int)` - synchronize device

**Memory Management:**
- `alloc(size_t, dtype)` - allocate device memory (deprecated)
- `allocV2(size_t)` - allocate device memory
- `alloc<T>(size_t)` - allocate typed memory (deprecated)
- `free(void*)` - free device memory (deprecated)
- `freeV2(void*)` - free device memory
- `pinned(size_t, dtype)` - allocate pinned memory
- `pinned<T>(size_t)` - allocate typed pinned memory
- `freePinned(void*)` - free pinned memory
- `allocHost(size_t, dtype)` - allocate host memory
- `allocHost<T>(size_t)` - allocate typed host memory
- `freeHost(void*)` - free host memory
- `deviceMemInfo(size_t*, size_t*, size_t*, size_t*)` - get memory info
- `printMemInfo(char*, int)` - print memory info
- `deviceGC()` - garbage collection
- `setMemStepSize(size_t)` - set memory step size
- `getMemStepSize()` - get memory step step size

---

## Backend-Specific

### cuda.h - CUDA Backend
Functions for CUDA-specific operations (stream management, device properties, etc.)

### opencl.h - OpenCL Backend
Functions for OpenCL-specific operations (context, queue, program management, etc.)

### oneapi.h - OneAPI Backend
Functions for OneAPI-specific operations

---

## Utilities

### seq.h - Sequence Objects
**Classes:**
- `seq` - represents a sequence for indexing
  - Constructors: `seq()`, `seq(double)`, `seq(double, double)`, `seq(double, double, double)`
  - Operators: `+`, `-`, `*`, `/` with scalar
  - `operator-(seq)` - negate sequence

### exception.h - Error Handling
**Classes:**
- `exception` - ArrayFire exception class

### defines.h - Type Definitions & Constants
- Data types (dtype enum)
- Matrix properties (matProp)
- Interpolation types (interpType)
- Border types (borderType)
- Match types (matchType)
- Storage formats (storage)
- And many other enums and constants

---

## Summary Statistics

**Total Major Categories:**
- Array Management & Core Operations
- Arithmetic & Mathematical Operations (60+ functions)
- Linear Algebra (20+ functions)
- Data Generation & Manipulation (30+ functions)
- Signal Processing (40+ functions)
- Image Processing (50+ functions)
- Computer Vision (10+ functions)  
- Statistics & Reductions (40+ functions)
- Random Number Generation (15+ functions)
- Sparse Arrays (10+ functions)
- Device Management (20+ functions)

**Key Operators Defined:**
At the C++ level, arithmetic and comparison operators are implemented through the C interface functions. The main operators available on arrays through operator overloading are:
- Indexing: `operator()` 
- Assignment: `=`, `+=`, `-=`, `*=`, `/=` (via array_proxy)
- Transpose: `T()`, conjugate transpose: `H()`
- Type conversion: `as(dtype)`

Most arithmetic operators (+, -, *, /, %, etc.) and comparison operators (<, >, <=, >=, ==, !=) are implemented as standalone functions or through the C interface rather than as C++ operators on the array class.

**Complex Number Operators:**
The complex number types (cfloat, cdouble) do have full operator support:
- Arithmetic: `+`, `-`, `*`, `/`
- Comparison: `==`, `!=`
- Stream I/O: `<<`, `>>`

---

## Notes on API Versioning

Many functions are conditionally compiled based on AF_API_VERSION:
- `>= 31`: Added functions like `fftInPlace`, `sigmoid`, `harris`, etc.
- `>= 32`: Added `loadImageNative`, `saveImageNative`, `gloh`
- `>= 33`: Added `isImageIOAvailable`, `isLAPACKAvailable`, memory management improvements
- `>= 34`: Added sparse array support, `clamp`, `randomEngine` class
- `>= 35`: Added typed `dot` function
- `>= 37`: Added functions with `ByKey` suffix, `pinverse`, `rsqrt`, `approx1/2` variants, `meanvar`
- `>= 38`: Added `allocV2`, `freeV2`, variance functions with `af_var_bias`, ragged max, `bitnot`

---

## Comparison with Clojure Integration Layer

To assess completeness of the Clojure integration layer, compare:
1. The functions listed in this catalog
2. The functions in the `src/org/soulspace/arrayfire/integration/` directory

Look for:
- Missing function categories
- Incomplete wrappers for major functionality
- Functions that should be exposed to users
- Complex number support completeness
- Operator equivalents (may need wrapper functions in Clojure)
