# Spectral ASM 
![build](https://github.com/spectral-powered/asm/workflows/build/badge.svg)
![GitHub release (latest by date including pre-releases)](https://img.shields.io/github/v/release/spectral-powered/asm?include_prereleases)
![Libraries.io dependency status for GitHub repo](https://img.shields.io/librariesio/github/spectral-powered/asm)
![GitHub](https://img.shields.io/github/license/spectral-powered/asm)

Spectral ASM is a library designed to extend the current ASM library and provide
additional features to be used by various parts of Spectral projects. 

Currently, the core library is a 'extension' or rework of the ASM tree api and provides
a more Object Oriented api and provides out of the box hierarchy support

## Setup

#### Gradle
Add the Spectral maven repository to your **build.gradle** file.
```groovy
repositories {
    maven { url 'https://repo.spectralclient.org/repository/spectral/' }
}
```

Add the following to your dependencies closure.
```groovy
dependencies {
    // For the asm-core module
    implementation "org.spectral.asm:asm-core:0.1.1"
}
```

## Quickstart

#### Core Example
Loading a JAR file into a ClassPool object.
```kotlin
/*
 * Create the empty class pool object.
 */
val pool = ClassPool.create()

/*
 * Add all the class from a JAR file to the
 * class pool object.
 */
pool.addArchive(File("/path/to/my/jar/file.jar"))

// Outputs the number of classes loaded
println("Class count: ${pool.size}")
```

