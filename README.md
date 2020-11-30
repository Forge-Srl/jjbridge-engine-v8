# JJBridge V8 Engine

[![javadoc](https://javadoc.io/badge2/srl.forge/jjbridge-engine-v8/javadoc.svg)](https://javadoc.io/doc/srl.forge/jjbridge-engine-v8)

![Build JJBridge V8 Engine for Android](https://github.com/Forge-Srl/jjbridge-engine-v8/workflows/Build%20JJBridge%20V8%20Engine%20for%20Android/badge.svg?branch=main)

![Build JJBridge V8 Engine for Linux](https://github.com/Forge-Srl/jjbridge-engine-v8/workflows/Build%20JJBridge%20V8%20Engine%20for%20Linux/badge.svg?branch=main)

![Build JJBridge V8 Engine for macOS](https://github.com/Forge-Srl/jjbridge-engine-v8/workflows/Build%20JJBridge%20V8%20Engine%20for%20macOS/badge.svg?branch=main)

![Build JJBridge V8 Engine for Windows](https://github.com/Forge-Srl/jjbridge-engine-v8/workflows/Build%20JJBridge%20V8%20Engine%20for%20Windows/badge.svg?branch=main)

JJBridge is a multi-library project which brings JavaScript execution capabilities to Java.

JJBridge V8 Engine allows using [V8](https://v8.dev/) as the underlying engine for [JJBridge Api](https://github.com/Forge-Srl/jjbridge-api).

## Contents

- [Installation](#installation)
- [Usage](#usage)
  - [Initialization](#initialization)
  - [Technical details](#technical-details)
- [Licence](#license)

## Installation
V8 binaries shipped with JJBridge V8 Engine are platform dependent. To keep the packages small (and to avoid loading the 
wrong libraries), we distribute a jar with a different classifier for each platform. Here is how you add it based on 
your target platform:

- For Linux add this to your pom.xml:
  ```xml
  <dependency>
    <groupId>srl.forge</groupId>
    <artifactId>jjbridge-engine-v8</artifactId>
    <version>0.1.2</version>
    <classifier>linux</classifier>
  </dependency>
  ```
- For macOS add this to your pom.xml:
  ```xml
  <dependency>
    <groupId>srl.forge</groupId>
    <artifactId>jjbridge-engine-v8</artifactId>
    <version>0.1.2</version>
    <classifier>macos</classifier>
  </dependency>
  ```
- For Windows add this to your pom.xml:
  ```xml
  <dependency>
    <groupId>srl.forge</groupId>
    <artifactId>jjbridge-engine-v8</artifactId>
    <version>0.1.2</version>
    <classifier>windows</classifier>
  </dependency>
  ```
- For Android add this to your pom.xml:
  ```xml
  <dependency>
    <groupId>srl.forge</groupId>
    <artifactId>jjbridge-engine-v8</artifactId>
    <version>0.1.2</version>
    <classifier>android</classifier>
    <type>aar</type>
  </dependency>
  ```

## Usage
The full javadoc is available at <https://www.javadoc.io/doc/srl.forge/jjbridge-engine-v8/latest/index.html>.

### Initialization
To get a `JSEngine` just use:
```java
JSEngine engine = new V8Engine();
```
You can also pass flags to V8 by calling `V8Engine.setFlags()` before instantiating `V8Engine`.

Then instantiate a `JSRuntime` and use JJBridge as usual:
```java
try (JSRuntime runtime = engine.newRuntime()) {
    // Do JavaScript things here...
} catch (RuntimeException e) {
}
```

### Technical details
JJBridge V8 Engine currently uses V8 version **8.4.371.22**. V8 precompiled binaries are taken from
<https://github.com/tbossi/v8-builder> thus the following platforms are supported:
- Linux (x64)
- Android (x86, x86-64, arm v7, arm v8)
- macOS (x64)
- Windows (x64)

## License

See the [LICENSE](LICENSE.md) file for license rights and limitations (MIT).
