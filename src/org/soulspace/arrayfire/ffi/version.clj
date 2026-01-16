(ns org.soulspace.arrayfire.ffi.version
  "Bindings for the ArrayFire version information functions.
   
   Version information is essential for:
   - Ensuring compatibility between application and library
   - Debugging environment-specific issues
   - Feature detection (API version gating)
   - Build reproducibility and tracking
   - Issue reporting and support
   
   ArrayFire Version Numbering:
   
   ArrayFire follows Semantic Versioning (SemVer 2.0):
   
   **Version Format**: MAJOR.MINOR.PATCH
   
   - **MAJOR**: Incremented for incompatible API changes
     * Breaking changes to public API
     * Removal of deprecated features
     * Major architectural changes
   
   - **MINOR**: Incremented for backward-compatible functionality additions
     * New features and functions
     * Performance improvements
     * Non-breaking enhancements
   
   - **PATCH**: Incremented for backward-compatible bug fixes
     * Bug fixes
     * Security patches
     * Minor tweaks
   
   Examples:
   - 3.8.0 → 3.8.1: Bug fix release (patch)
   - 3.8.1 → 3.9.0: New features (minor)
   - 3.9.0 → 4.0.0: Breaking changes (major)
   
   Version Compatibility:
   
   1. **Forward Compatibility**:
      - Code written for 3.8.0 should work with 3.8.x and 3.9.x
      - May not work with 4.0.0 (major version change)
   
   2. **Backward Compatibility**:
      - Features in 3.9.0 may not be available in 3.8.0
      - Check version before using newer features
   
   3. **API Version Guards**:
      - ArrayFire uses AF_API_VERSION for feature gating
      - Allows conditional compilation based on version
      - Enables graceful degradation
   
   Revision Information:
   
   The revision string provides Git commit information:
   - Full Git commit hash (SHA-1)
   - Identifies exact source code state
   - Essential for debugging and issue tracking
   - Distinguishes between releases and development builds
   
   Format: \"v3.8.0-123-g1a2b3c4d\"
   - v3.8.0: Base version tag
   - 123: Commits since tag
   - g1a2b3c4d: Git commit hash (abbreviated)
   
   Use Cases:
   
   1. **Version Checking at Startup**:
      ```clojure
      (let [major (mem/alloc ::mem/int)
            minor (mem/alloc ::mem/int)
            patch (mem/alloc ::mem/int)
            _ (af-get-version major minor patch)
            maj (mem/deref major ::mem/int)
            min (mem/deref minor ::mem/int)
            pat (mem/deref patch ::mem/int)]
        (when (or (< maj 3)
                  (and (= maj 3) (< min 8)))
          (throw (ex-info \"ArrayFire 3.8.0 or higher required\"
                         {:found [maj min pat]
                          :required [3 8 0]})))
        [maj min pat])
      ```
   
   2. **Feature Detection**:
      ```clojure
      (defn has-feature? [required-major required-minor]
        (let [major (mem/alloc ::mem/int)
              minor (mem/alloc ::mem/int)
              patch (mem/alloc ::mem/int)
              _ (af-get-version major minor patch)
              maj (mem/deref major ::mem/int)
              min (mem/deref minor ::mem/int)]
          (or (> maj required-major)
              (and (= maj required-major)
                   (>= min required-minor)))))
      
      (when (has-feature? 3 8)
        ;; Use features introduced in 3.8
        (use-new-api))
      ```
   
   3. **Logging and Diagnostics**:
      ```clojure
      (defn log-arrayfire-info []
        (let [major (mem/alloc ::mem/int)
              minor (mem/alloc ::mem/int)
              patch (mem/alloc ::mem/int)
              _ (af-get-version major minor patch)
              revision (af-get-revision)
              revision-str (mem/read-string revision)]
          (println \"ArrayFire version:\"
                  (format \"%d.%d.%d\"
                         (mem/deref major ::mem/int)
                         (mem/deref minor ::mem/int)
                         (mem/deref patch ::mem/int)))
          (println \"Git revision:\" revision-str)))
      ```
   
   4. **Issue Reporting**:
      ```clojure
      (defn collect-debug-info []
        (let [major (mem/alloc ::mem/int)
              minor (mem/alloc ::mem/int)
              patch (mem/alloc ::mem/int)
              _ (af-get-version major minor patch)
              revision (mem/read-string (mem/read-pointer
                                         (af-get-revision)))]
          {:arrayfire-version
           {:major (mem/deref major ::mem/int)
            :minor (mem/deref minor ::mem/int)
            :patch (mem/deref patch ::mem/int)
            :revision revision}
           :backend (get-active-backend)
           :device-info (get-device-info)
           :platform (get-platform)}))
      ```
   
   5. **Version-Dependent Behavior**:
      ```clojure
      (defn create-array-safe [data dims]
        (let [major (mem/alloc ::mem/int)
              minor (mem/alloc ::mem/int)
              patch (mem/alloc ::mem/int)
              _ (af-get-version major minor patch)
              maj (mem/deref major ::mem/int)
              min (mem/deref minor ::mem/int)]
          (if (or (> maj 3) (and (= maj 3) (>= min 9)))
            ;; Use improved API from 3.9+
            (create-array-v2 data dims)
            ;; Fall back to older API
            (create-array-legacy data dims))))
      ```
   
   6. **Build Information Display**:
      ```clojure
      (defn show-about []
        (let [major (mem/alloc ::mem/int)
              minor (mem/alloc ::mem/int)
              patch (mem/alloc ::mem/int)
              _ (af-get-version major minor patch)
              maj (mem/deref major ::mem/int)
              min (mem/deref minor ::mem/int)
              pat (mem/deref patch ::mem/int)
              revision (mem/read-string
                        (mem/read-pointer (af-get-revision)))]
          (println \"================================================\")
          (println \"ArrayFire Library Information\")
          (println \"================================================\")
          (println (format \"Version: %d.%d.%d\" maj min pat))
          (println (format \"Revision: %s\" revision))
          (println (format \"Backend: %s\" (get-active-backend-name)))
          (println \"================================================\")))
      ```
   
   Integration with Build Systems:
   
   **Compile-Time Version Checking**:
   The version information comes from build-time constants:
   - AF_VERSION_MAJOR: Defined in CMake/build system
   - AF_VERSION_MINOR: Defined in CMake/build system
   - AF_VERSION_PATCH: Defined in CMake/build system
   - AF_REVISION: Git commit hash from build time
   
   **Runtime vs Compile-Time**:
   - af-get-version: Returns runtime library version
   - May differ from compile-time if library upgraded
   - Always check runtime version for safety
   
   Version History (Major Releases):
   
   - **v1.x** (2012-2014): Initial releases, CUDA support
   - **v2.x** (2014-2015): OpenCL support, CPU backend
   - **v3.0** (2015): Unified API, major refactoring
   - **v3.1-3.3** (2015-2016): Performance improvements, new functions
   - **v3.4** (2016): JIT improvements, new visualizations
   - **v3.5** (2017): oneAPI backend preview
   - **v3.6** (2018): Memory manager improvements
   - **v3.7** (2019): New statistics functions
   - **v3.8** (2020): Enhanced deep learning support
   - **v3.9** (2021): Performance optimizations
   
   Common Patterns:
   
   ```clojure
   ;; 1. Defensive version checking
   (defn require-version [maj min pat]
     (let [major (mem/alloc ::mem/int)
           minor (mem/alloc ::mem/int)
           patch (mem/alloc ::mem/int)
           _ (af-get-version major minor patch)
           actual-maj (mem/deref major ::mem/int)
           actual-min (mem/deref minor ::mem/int)
           actual-pat (mem/deref patch ::mem/int)]
       (when (or (< actual-maj maj)
                 (and (= actual-maj maj) (< actual-min min))
                 (and (= actual-maj maj) (= actual-min min)
                      (< actual-pat pat)))
         (throw (ex-info \"ArrayFire version too old\"
                        {:required [maj min pat]
                         :actual [actual-maj actual-min actual-pat]})))))
   
   ;; 2. Version string formatting
   (defn version-string []
     (let [major (mem/alloc ::mem/int)
           minor (mem/alloc ::mem/int)
           patch (mem/alloc ::mem/int)
           _ (af-get-version major minor patch)]
       (format \"%d.%d.%d\"
              (mem/deref major ::mem/int)
              (mem/deref minor ::mem/int)
              (mem/deref patch ::mem/int))))
   
   ;; 3. Full version info
   (defn full-version-info []
     (let [major (mem/alloc ::mem/int)
           minor (mem/alloc ::mem/int)
           patch (mem/alloc ::mem/int)
           _ (af-get-version major minor patch)
           revision-ptr (af-get-revision)
           revision (mem/read-string revision-ptr)]
       {:major (mem/deref major ::mem/int)
        :minor (mem/deref minor ::mem/int)
        :patch (mem/deref patch ::mem/int)
        :revision revision
        :version-string (format \"%d.%d.%d\"
                               (mem/deref major ::mem/int)
                               (mem/deref minor ::mem/int)
                               (mem/deref patch ::mem/int))}))
   ```
   
   Testing and CI/CD:
   
   Version checks are crucial for:
   - Ensuring consistent test environments
   - Detecting library version mismatches
   - Validating deployment configurations
   - Automated compatibility testing
   
   Example test:
   ```clojure
   (deftest version-check-test
     (testing \"ArrayFire version meets requirements\"
       (let [major (mem/alloc ::mem/int)
             minor (mem/alloc ::mem/int)
             patch (mem/alloc ::mem/int)
             err (af-get-version major minor patch)
             maj (mem/deref major ::mem/int)
             min (mem/deref minor ::mem/int)]
         (is (= err AF_SUCCESS))
         (is (>= maj 3))
         (is (or (> maj 3) (>= min 8))))))
   ```
   
   Best Practices:
   
   1. **Always Check Version**:
      - Check version at application startup
      - Log version information for diagnostics
      - Validate against minimum required version
   
   2. **Handle Version Mismatches Gracefully**:
      - Provide clear error messages
      - Suggest required version
      - Include actual version in error
   
   3. **Feature Detection Over Version Comparison**:
      - When possible, detect feature availability
      - More robust than hardcoded version checks
      - Handles custom builds better
   
   4. **Include in Bug Reports**:
      - Always report ArrayFire version
      - Include revision/commit hash
      - Specify backend being used
   
   5. **Document Version Requirements**:
      - Clearly state minimum version
      - Note which features require newer versions
      - Provide upgrade instructions
   
   Related Functions:
   - af-info: Display device and platform information
   - af-get-active-backend: Query current backend
   - af-get-backend-id: Get backend identifier
   - af-get-device-id: Get current device
   
   See also:
   - ArrayFire documentation for version history
   - Release notes for breaking changes
   - Migration guides for major version upgrades"
  (:require [coffi.ffi :as ffi :refer [defcfn]]
            [coffi.mem :as mem]))

;; Version information

;; af_err af_get_version(int *major, int *minor, int *patch)
(defcfn af-get-version
  "Get the version information of the ArrayFire library.
   
   Returns the semantic version (MAJOR.MINOR.PATCH) of the ArrayFire library
   currently loaded at runtime. This is essential for compatibility checking,
   feature detection, and debugging.
   
   Parameters:
   - major: Pointer to int for major version number (output)
   - minor: Pointer to int for minor version number (output)
   - patch: Pointer to int for patch version number (output)
   
   Version Semantics (SemVer 2.0):
   
   **MAJOR version**:
   - Incremented for incompatible API changes
   - Breaking changes to public API
   - Removal of deprecated functionality
   - Major architectural changes
   
   **MINOR version**:
   - Incremented for backward-compatible new features
   - New functions added to API
   - Performance improvements
   - Non-breaking enhancements
   
   **PATCH version**:
   - Incremented for backward-compatible bug fixes
   - Security patches
   - Documentation updates
   - Minor corrections
   
   Return Value:
   The function writes version numbers to the provided pointers and
   returns AF_SUCCESS (0). This function cannot fail under normal
   circumstances as it returns compile-time constants.
   
   Example (Basic version check):
   ```clojure
   (let [major-ptr (mem/alloc ::mem/int)
         minor-ptr (mem/alloc ::mem/int)
         patch-ptr (mem/alloc ::mem/int)
         err (af-get-version major-ptr minor-ptr patch-ptr)]
     (when (= err AF_SUCCESS)
       (let [major (mem/deref major-ptr ::mem/int)
             minor (mem/deref minor-ptr ::mem/int)
             patch (mem/deref patch-ptr ::mem/int)]
         (println (format \"ArrayFire %d.%d.%d\" major minor patch))
         {:major major :minor minor :patch patch})))
   ```
   
   Example (Require minimum version):
   ```clojure
   (defn require-arrayfire-version [min-major min-minor]
     (let [major-ptr (mem/alloc ::mem/int)
           minor-ptr (mem/alloc ::mem/int)
           patch-ptr (mem/alloc ::mem/int)
           _ (af-get-version major-ptr minor-ptr patch-ptr)
           major (mem/deref major-ptr ::mem/int)
           minor (mem/deref minor-ptr ::mem/int)
           patch (mem/deref patch-ptr ::mem/int)]
       (when (or (< major min-major)
                 (and (= major min-major) (< minor min-minor)))
         (throw (ex-info
                 (format \"ArrayFire version %d.%d or higher required, found %d.%d.%d\"
                        min-major min-minor major minor patch)
                 {:required [min-major min-minor]
                  :found [major minor patch]})))
       [major minor patch]))
   
   ;; Usage
   (require-arrayfire-version 3 8)  ; Requires 3.8.0 or higher
   ```
   
   Example (Feature detection):
   ```clojure
   (defn arrayfire-supports-feature? [feature-major feature-minor]
     \"Check if ArrayFire version supports a feature introduced in given version.\"
     (let [major-ptr (mem/alloc ::mem/int)
           minor-ptr (mem/alloc ::mem/int)
           patch-ptr (mem/alloc ::mem/int)
           _ (af-get-version major-ptr minor-ptr patch-ptr)
           major (mem/deref major-ptr ::mem/int)
           minor (mem/deref minor-ptr ::mem/int)]
       (or (> major feature-major)
           (and (= major feature-major)
                (>= minor feature-minor)))))
   
   ;; Usage
   (when (arrayfire-supports-feature? 3 9)
     (use-new-api-from-3-9))
   ```
   
   Example (Diagnostic logging):
   ```clojure
   (defn log-environment-info []
     \"Log complete ArrayFire environment information.\"
     (let [major-ptr (mem/alloc ::mem/int)
           minor-ptr (mem/alloc ::mem/int)
           patch-ptr (mem/alloc ::mem/int)
           _ (af-get-version major-ptr minor-ptr patch-ptr)
           major (mem/deref major-ptr ::mem/int)
           minor (mem/deref minor-ptr ::mem/int)
           patch (mem/deref patch-ptr ::mem/int)
           revision (mem/read-string (af-get-revision))]
       (println \"=== ArrayFire Environment ===\")
       (println (format \"Version: %d.%d.%d\" major minor patch))
       (println (format \"Revision: %s\" revision))
       (af-info)  ; Display device info
       (println \"=============================\")))
   ```
   
   Example (Version comparison utility):
   ```clojure
   (defn compare-version [major minor patch]
     \"Compare runtime version against specified version.
      Returns: -1 if runtime < specified
                0 if runtime = specified
                1 if runtime > specified\"
     (let [maj-ptr (mem/alloc ::mem/int)
           min-ptr (mem/alloc ::mem/int)
           pat-ptr (mem/alloc ::mem/int)
           _ (af-get-version maj-ptr min-ptr pat-ptr)
           rt-major (mem/deref maj-ptr ::mem/int)
           rt-minor (mem/deref min-ptr ::mem/int)
           rt-patch (mem/deref pat-ptr ::mem/int)]
       (cond
         (not= rt-major major) (compare rt-major major)
         (not= rt-minor minor) (compare rt-minor minor)
         :else (compare rt-patch patch))))
   ```
   
   Example (Build info for support):
   ```clojure
   (defn generate-support-info []
     \"Generate comprehensive information for bug reports.\"
     (let [major-ptr (mem/alloc ::mem/int)
           minor-ptr (mem/alloc ::mem/int)
           patch-ptr (mem/alloc ::mem/int)
           _ (af-get-version major-ptr minor-ptr patch-ptr)
           version-info
           {:arrayfire
            {:version (format \"%d.%d.%d\"
                             (mem/deref major-ptr ::mem/int)
                             (mem/deref minor-ptr ::mem/int)
                             (mem/deref patch-ptr ::mem/int))
             :revision (mem/read-string (af-get-revision))}
            :system
            {:os (System/getProperty \"os.name\")
             :arch (System/getProperty \"os.arch\")
             :java-version (System/getProperty \"java.version\")}}]
       (println (json/write-str version-info :indent true))
       version-info))
   ```
   
   Use Cases:
   1. **Startup Validation**: Ensure compatible library version
   2. **Feature Detection**: Check if specific features available
   3. **Debugging**: Log version in error reports
   4. **Testing**: Validate test environment setup
   5. **CI/CD**: Verify deployment configurations
   
   Notes:
   - Version numbers are compile-time constants
   - Runtime version may differ from compile-time if library upgraded
   - Always check runtime version for production applications
   - Version returned is of the loaded shared library
   
   Compatibility:
   - Available in all ArrayFire versions
   - One of the most stable API functions
   - Signature unchanged across versions
   
   Returns:
   AF_SUCCESS (0) - Always succeeds
   
   See also:
   - af-get-revision: Get Git commit information
   - af-info: Display comprehensive device/platform info
   - af-get-backend-id: Query active backend"
  "af_get_version" [::mem/pointer ::mem/pointer ::mem/pointer] ::mem/int)

;; const char* af_get_revision()
(defcfn af-get-revision
  "Get the Git revision (commit hash) of the ArrayFire library.
   
   Returns a pointer to a constant string containing the Git commit
   information from when the library was built. This provides exact
   source code traceability for debugging and issue tracking.
   
   Return Value:
   Returns a const char* pointer to a null-terminated string containing
   the Git revision information. The string is a compile-time constant
   and should NOT be freed by the caller.
   
   Revision String Format:
   
   The revision string typically follows this pattern:
   \"v3.8.0-123-g1a2b3c4d\"
   
   Components:
   - v3.8.0: Last Git tag (version)
   - 123: Number of commits since that tag
   - g1a2b3c4d: Abbreviated Git commit hash (7-8 characters)
   
   Special Cases:
   - \"v3.8.0\": Exact release (no commits after tag)
   - \"v3.8.0-dirty\": Uncommitted changes in build
   - \"unknown\": Git information not available during build
   
   Example (Basic usage):
   ```clojure
   (let [revision-ptr (af-get-revision)
         revision-str (mem/read-string revision-ptr)]
     (println \"ArrayFire built from commit:\" revision-str)
     revision-str)
   ```
   
   Example (Parse revision information):
   ```clojure
   (defn parse-revision [revision-str]
     \"Parse ArrayFire revision string into components.\"
     (let [pattern #\"v([0-9]+)\\.([0-9]+)\\.([0-9]+)(?:-([0-9]+)-g([0-9a-f]+))?\"
           matcher (re-matcher pattern revision-str)]
       (when (.find matcher)
         {:tag-major (Integer/parseInt (.group matcher 1))
          :tag-minor (Integer/parseInt (.group matcher 2))
          :tag-patch (Integer/parseInt (.group matcher 3))
          :commits-since-tag (when (.group matcher 4)
                              (Integer/parseInt (.group matcher 4)))
          :commit-hash (.group matcher 5)})))
   
   ;; Usage
   (let [rev-str (mem/read-string (af-get-revision))
         parsed (parse-revision rev-str)]
     (println \"Base version:\"
             (format \"%d.%d.%d\"
                    (:tag-major parsed)
                    (:tag-minor parsed)
                    (:tag-patch parsed)))
     (when (:commits-since-tag parsed)
       (println \"Commits since release:\" (:commits-since-tag parsed))
       (println \"Commit hash:\" (:commit-hash parsed))))
   ```
   
   Example (Full version report):
   ```clojure
   (defn full-version-report []
     \"Generate complete version information report.\"
     (let [major-ptr (mem/alloc ::mem/int)
           minor-ptr (mem/alloc ::mem/int)
           patch-ptr (mem/alloc ::mem/int)
           _ (af-get-version major-ptr minor-ptr patch-ptr)
           major (mem/deref major-ptr ::mem/int)
           minor (mem/deref minor-ptr ::mem/int)
           patch (mem/deref patch-ptr ::mem/int)
           revision (mem/read-string (af-get-revision))]
       {:version {:major major
                  :minor minor
                  :patch patch
                  :string (format \"%d.%d.%d\" major minor patch)}
        :revision revision
        :is-release (not (re-find #\"-\" revision))
        :is-dirty (boolean (re-find #\"dirty\" revision))}))
   ```
   
   Example (Issue reporting):
   ```clojure
   (defn create-bug-report [error-description]
     \"Create structured bug report with version info.\"
     (let [major-ptr (mem/alloc ::mem/int)
           minor-ptr (mem/alloc ::mem/int)
           patch-ptr (mem/alloc ::mem/int)
           _ (af-get-version major-ptr minor-ptr patch-ptr)
           version (format \"%d.%d.%d\"
                          (mem/deref major-ptr ::mem/int)
                          (mem/deref minor-ptr ::mem/int)
                          (mem/deref patch-ptr ::mem/int))
           revision (mem/read-string (af-get-revision))]
       {:bug-report
        {:description error-description
         :timestamp (java.time.Instant/now)
         :environment
         {:arrayfire-version version
          :arrayfire-revision revision
          :backend (get-active-backend-name)
          :device (get-device-name)
          :os (System/getProperty \"os.name\")
          :java-version (System/getProperty \"java.version\")}}}))
   ```
   
   Example (Development vs Release detection):
   ```clojure
   (defn is-development-build? []
     \"Check if running a development (non-release) build.\"
     (let [revision (mem/read-string (af-get-revision))]
       (or (re-find #\"-[0-9]+-g\" revision)  ; Has commits after tag
           (re-find #\"dirty\" revision)       ; Has uncommitted changes
           (re-find #\"unknown\" revision))))  ; No Git info
   
   (when (is-development-build?)
     (println \"WARNING: Running development build of ArrayFire\")
     (println \"         Use release build for production\"))
   ```
   
   Example (Logging with full context):
   ```clojure
   (defn log-with-version [level message]
     \"Log message with ArrayFire version context.\"
     (let [major-ptr (mem/alloc ::mem/int)
           minor-ptr (mem/alloc ::mem/int)
           patch-ptr (mem/alloc ::mem/int)
           _ (af-get-version major-ptr minor-ptr patch-ptr)
           version (format \"%d.%d.%d\"
                          (mem/deref major-ptr ::mem/int)
                          (mem/deref minor-ptr ::mem/int)
                          (mem/deref patch-ptr ::mem/int))
           revision (mem/read-string (af-get-revision))]
       (log level
           (format \"[ArrayFire %s (%s)] %s\"
                  version revision message))))
   ```
   
   Use Cases:
   1. **Exact Build Identification**: Pinpoint exact source code
   2. **Bug Reports**: Include precise version in reports
   3. **Build Verification**: Confirm correct library loaded
   4. **Development Tracking**: Identify development vs release
   5. **Reproducibility**: Enable exact build reproduction
   
   Memory Management:
   - Returned string is a compile-time constant
   - DO NOT call free() on the returned pointer
   - String lifetime is entire program duration
   - Safe to cache the pointer or string value
   
   Debugging Tips:
   - Include revision in all bug reports
   - Helps maintainers locate exact code state
   - Essential when reporting issues on GitHub
   - Distinguishes official releases from custom builds
   
   Build System Integration:
   - Revision populated by CMake/build system
   - Uses Git commands at build time
   - Requires Git repository for full info
   - Tarball builds may show \"unknown\"
   
   Availability:
   - Available since ArrayFire 3.3 (AF_API_VERSION >= 33)
   - For older versions, use af-get-version only
   - Check API version before calling if supporting older versions
   
   Returns:
   const char* - Pointer to constant string (do not free)
   
   See also:
   - af-get-version: Get semantic version numbers
   - af-info: Display comprehensive environment info
   - af-get-backend-id: Query active backend"
  "af_get_revision" [] ::mem/pointer)
