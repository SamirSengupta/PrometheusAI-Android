# PrometheusAI-Android

Welcome to the official repository for **PrometheusAI-Android**. This project allows you to run a powerful AI model locally on your Android device using ONNX Runtime.

## 🚀 Features
- **Local Inference**: Runs entirely on-device without internet access.
- **Privacy Focused**: No data leaves your device.
- **Customizable**: Built on open-source technologies.

## 🛠️ Prerequisites
Before you begin, ensure you have the following installed:
- **[Android Studio](https://developer.android.com/studio)** (Latest version recommended)
- **Git** installed on your machine.
- A decent Android device (or emulator) with at least **4GB - 8GB of RAM** for smooth performance.

## 📥 Installation & Setup

Follow these steps to set up the project locally:

### 1. Clone the Repository
Open your terminal or command prompt and run:
```bash
git clone https://github.com/SamirSengupta/PrometheusAI-Android.git
cd PrometheusAI-Android
```

### 2. Download Model Files (CRITICAL STEP)
The AI model files (`model.onnx` and `tokenizer.json`) are too large to be included in this repository. You must download them manually and place them in the correct folder for the app to work.

1.  **Download the files** from the link below:
[tokenizer.json](https://drive.google.com/file/d/1sYVcolegyOXzjnms05VrBqPIl5hOaIHN/view?usp=drive_link)
[model.onnx](https://drive.google.com/file/d/1ghA5bCvENKlXa__bjSPBhqp5fSal5IWB/view?usp=drive_link)

3.  **Locate the assets folder** in the project directory:
    `app/src/main/assets/`

4.  **Move the downloaded files** into that folder.
    *   Ensure the file names are exactly `model.onnx` and `tokenizer.json`.
    *   The final path should look like: 
        *   `.../PrometheusAI-Android/app/src/main/assets/model.onnx`
        *   `.../PrometheusAI-Android/app/src/main/assets/tokenizer.json`

### 3. Open in Android Studio
1.  Launch **Android Studio**.
2.  Select **File > Open**.
3.  Navigate to the cloned `PrometheusAI-Android` folder and select it.
4.  Wait for Gradle to sync (this may take a few minutes).

## 🏗️ How to Build Your Own PrometheusAI

Once the project is set up, you can build and run it on your device.

### Running on a Device
1.  Connect your Android device via USB.
2.  Enable **Developer Options** and **USB Debugging** on your phone.
3.  In Android Studio, select your device from the dropdown menu in the toolbar.
4.  Click the green **Run** button (▶️) or press `Shift + F10`.

### Building an APK
If you want to share the app or install it manually:
1.  Go to **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
2.  Once finished, a notification will appear. Click **locate** to find the `app-debug.apk` file.
3.  Transfer this file to your phone and install it.

## 🔧 Customization
Want to make it your own?
- **Change the Model**: You can replace `model.onnx` with other ONNX-compatible LLMs (ensure you update the code to handle different architectures if needed).
- **Modify UI**: Edit the files in `app/src/main/res/layout` to change the look and feel.
- **Logic**: The main logic resides in `MainActivity.kt`.

## ❓ Troubleshooting
- **Build Failed (OOM Error)**: If the build fails due to memory, try increasing the heap size in `gradle.properties`.
- **App Crashes on Start**: Ensure you have successfully placed `model.onnx` and `tokenizer.json` in the `assets` folder. The app cannot run without them.

- [PrometheusAI APK](https://drive.google.com/file/d/1ElTjFRNZVgJEdiwbOcX_4HYhWzHp_YoH/view?usp=drive_link)

---
*Built with ❤️ by [Samir Sengupta](https://github.com/SamirSengupta)*


