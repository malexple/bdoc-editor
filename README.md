# BDoc Editor 📚✨

`BDoc Editor` is an open-source, extensible desktop publishing (DTP) environment and visual layout editor specially designed for book restoration, layout reconstruction, and advanced digital typesetting.

The project introduces the **BDoc (Book Document)** format specification—a clean, modern alternative to legacy binary polymorphic formats (like IDML, INDD, or Scribus SLA). It is architected from the ground up to support high-fidelity print preparation, multi-layered canvas renderings, and AI-assisted layout recovery.

![BDoc Editor Interface Overview](assets/screenshots/img-full.png)

---

## 🚀 Key Features & Architecture

* **Strict Model/UI Separation:** The core data model (`bdoc-model`) consists of pure POJO classes completely decoupled from any UI framework (JavaFX/Canvas). This ensures headless document validation, easy CLI processing, and seamless third-party plugin scripting.
* **Extensible SPI Plugin Architecture:** Inspired by the IntelliJ Platform, the UI dynamically registers utilities, custom toolbars, and filters via modular configurations.
* **Prepress & Professional DTP Capabilities (In Development):** High-precision unit systems (points, millimeters, picas), multi-threaded text frames (Paragraph Composer based on the Knuth-Plass algorithm), full CMYK/Color Management integration, and rich geometry objects.
* **Book Restoration Sandbox:** Dedicated data structures to hold dirty scan overlays, AI-OCR background text tracks, and structural metadata without violating core document schemas.

### 📸 Application Previews

<p align="center">
  <img src="assets/screenshots/img-1.png" width="30%" alt="Preview 1" />
  <img src="assets/screenshots/img-3.png" width="30%" alt="Preview 2" />
  <img src="assets/screenshots/img-4.png" width="30%" alt="Preview 3" />
</p>

---

## 🛠 Project Structure

The repository is organized into highly decoupled modules:

* `bdoc-core` — Core application logic and execution engine.
* `bdoc-model` — Pure, framework-agnostic document POJOs (JSON/CBOR serializable).
* `bdoc-ui` — Desktop visual editor application built with JavaFX.
* `bdoc-spi` — Service Provider Interfaces and extension points for plugin developers.
* `bdoc-io` — Input/Output drivers for format serialization and document importing.
* `bdoc-plugin-demo-toolbar` — Reference implementation of a decoupled UI toolbar plugin.

---

## 🗺 Roadmap & Current Status

The project is moving through a structured development cycle:

1. **Stage 1: Document Structure & Core Geometry**
    * [ ] Mastering Pages, Margin Models, and Layout Guides.
    * [ ] ReadingOrder Segments and text wrap boundaries.
    * [ ] Extended vector geometry (BezierPaths, CompoundPaths) and strict multi-unit conversion.
    * [ ] Text threading (story flow across frames) and style hierarchies.
2. **Stage 2: Professional DTP & Preflight Functionality**
    * [ ] Visual Master Page overriding.
    * [ ] Integrated Preflight validation (Effective DPI check, CMYK profile matching, bleed/trim safety zones).
    * [ ] Advanced DTP tools (Gap tool, Scissors/Pen nodes editing, Eyedropper style copying).
    * [ ] Advanced Typography (Justify with Knuth-Plass algorithm, OpenType ligatures).
3. **Stage 3: Rendering Optimization & SDK Ecosystem**
    * [ ] Render Cache (Bitmap buffering for fluid 1000+ page scrolling on lower-end hardware).
    * [ ] Declarative plugin distribution marketplace.

---

## 🏗 Getting Started

### Prerequisites
* **JDK 17** or higher
* **Gradle** (Wrapper included)

### Build and Run
Clone the repository and build the project using Gradle:

```bash
git clone https://github.com
cd bdoc-editor
./gradlew build
```

To launch the desktop interface, run the target application module subtask:
```bash
./gradlew :bdoc-ui:run
```

---

## 🤝 Contributing & Community

We are building a toolchain for preservationists, typographers, and software engineers alike. Contributions to the specifications, text layout engines, or importing filters are highly welcome!

Feel free to open an Issue, submit a Pull Request, or check out our specifications draft inside the `preliminary-open-document-spec-v0.1.md` file.

## 📄 License
This project is licensed under the MIT License.
