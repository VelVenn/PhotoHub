

# PhotoHub — *Efficient, modern, and extensible local image management for everyone.*

## Introduction

**PhotoHub** is a modern, **open-source**, **multi-system**, high-performance local image viewer and manager for desktop, featuring asynchronous loading, smart caching, batch operations, and a responsive UI. It is designed to provide a smooth, efficient, and extensible experience for browsing and managing your photo collections.

---

## Features

- **Asynchronous Image Loading**: Non-blocking, instant browsing even with large collections.
- **Smart Caching**: Memory cache (Caffeine) with configurable size and expiration for fast access.
- **Batch File Management**: Select, copy, rename, and delete multiple files at once.
- **Responsive UI**: JavaFX-based interface with real-time feedback, smooth scaling, and drag-to-move support.
- **Multi-format Support**: Supports JPEG, PNG, GIF, TIFF, WebP, and more (via Java ImageIO & TwelveMonkeys).
- **Slideshow Mode**: Automatic photo playback with customizable interval.
- **Rich Metadata Display**: Knowing all things about the image with first glance.
- **Customizable**: Easily adjust cache size, thread pool, and timeout settings.

---

## Requirements

- OpenJDK / Java SE 21 or higher
- Maven 3.8.5 or higher

### Dependencies

- JavaFX (controls, fxml, swing, graphics)
- ControlsFX
- Ikonli (MaterialDesign2, Material2)
- JFoenix
- Caffeine
- TwelveMonkeys ImageIO (core, metadata, jpeg, tiff, webp)

All dependencies are managed via Maven. See [`pom.xml`](pom.xml) for details.

**Note**: *This project uses the **DEFAULT** Maven Central repository. If you encounter slow downloads or build failures, you may need to configure a [**Maven mirror**](https://maven.apache.org/guides/mini/guide-mirror-settings.html) (such as Aliyun, Huawei, or Tsinghua mirrors) or [**proxy** ](https://maven.apache.org/guides/mini/guide-proxies.html)in your* [`settings.xml`](https://maven.apache.org/settings.html).

---

## Build & Run

```bash
git clone https://github.com/VelVenn/PhotoHub.git
cd PhotoHub
mvn clean javafx:run
```

---

## Screenshots

![](src\main\resources\io\loraine\photohub\Default_Resources\screenshot.png)

------

## License

PhotoHub is distributed under the [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.html).

---

## Contribution

Contributions are welcome! Please submit pull requests or issues for bug reports, feature requests, or improvements.
