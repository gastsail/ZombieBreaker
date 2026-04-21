<img width="1364" height="965" alt="Screenshot 2026-04-21 at 12 40 42 PM" src="https://github.com/user-attachments/assets/93ea6790-7da6-4d10-b8f6-6d3a9bbf1b16" />


# ZombieBreaker 🧟‍♂️🛡️

**ZombieBreaker** es un plugin para Android Studio diseñado para detener la práctica del "Copy-Paste" ciego. Su objetivo es asegurar que cada desarrollador entienda exactamente qué código está integrando en su proyecto antes de que este llegue al editor.

---

## 📝 El Concepto: "Entender antes de Pegar"

A diferencia de otros asistentes de IA, **ZombieBreaker** actúa como un guardián en tu portapapeles. Cuando intentas pegar código proveniente de una fuente externa (StackOverflow, documentación, ChatGPT, etc.), el plugin intercepta la acción y te desafía:

1.  **Contexto Inteligente:** El plugin analiza el archivo actual donde intentas pegar el código para entender el entorno (clases, variables y lógica existente).
2.  **Análisis Local:** Utiliza IA local para procesar el código que intentas pegar.
3.  **Validación de Comprensión:** Debes explicar con tus propias palabras qué hace ese código. 
4.  **Aprobación:** El plugin compara tu explicación con el análisis real del código. Si tu comprensión es correcta, el código se pega. Si no, te ayuda a entenderlo mejor antes de permitir la inserción.

---

## 🚀 Requisitos Previos

Para que ZombieBreaker pueda analizar el código localmente, necesitas configurar **Ollama**:

1.  **Instalar Ollama:** Descárgalo en [https://ollama.com/](https://ollama.com/).
2.  **Descargar el Modelo Gemma 4:** Abre una terminal y ejecuta el siguiente comando:
    ```bash
    ollama run gemma4
    ```
    *(Nota: Mantén Ollama ejecutándose en segundo plano mientras programas en Android Studio).*

---

## 🛠️ Instalación

Actualmente, el plugin se distribuye como un archivo de release para instalación manual:

1.  **Descarga el archivo:** Ve a la pestaña de [Releases](https://github.com/tu-usuario/zombiebreaker/releases) y baja el archivo `.zip`.
2.  **Instalación en Android Studio:**
    * Abre **Android Studio**.
    * Ve a `File` > `Settings` (en macOS: `Android Studio` > `Settings`).
    * Selecciona la sección **Plugins**.
    * Haz clic en el icono del engranaje (⚙️) y selecciona **Install Plugin from Disk...**.
    * Busca y selecciona el archivo `.zip` descargado.
3.  **Reiniciar:** Reinicia el IDE para activar la protección de ZombieBreaker.

---

## 📖 Cómo usarlo

Una vez instalado, el plugin monitorea la acción de "Pegar" (`Ctrl+V` o `Cmd+V`):

1.  Copia cualquier código externo.
2.  Al intentar pegarlo en tu archivo de Android Studio, aparecerá una ventana de **ZombieBreaker**.
3.  Lee el análisis del contexto que el plugin ha realizado.
4.  Escribe una breve explicación de lo que crees que hace ese código.
5.  Si la validación es positiva, el código se integrará en tu archivo.

---

## 🔒 Privacidad y Seguridad Total

ZombieBreaker está diseñado para entornos profesionales donde la seguridad es crítica:

* **Procesamiento 100% Local:** Gracias a **Ollama** y **Gemma 4**, ni el código de tu proyecto ni el código que pegas salen nunca de tu máquina.
* **Sin Nube:** No requiere conexión a internet ni suscripciones a servicios externos.
* **Privacidad por Diseño:** Ideal para trabajar en proyectos confidenciales o corporativos.

---
*ZombieBreaker - Porque programar es entender, no solo copiar.*


## [EN]
# ZombieBreaker 🧟‍♂️🛡️

**ZombieBreaker** is an Android Studio plugin designed to put an end to "blind copy-pasting." Its mission is to ensure that every developer fully understands the code they are integrating into their project before it even hits the editor.

---

## 📝 The Concept: "Understand Before You Paste"

Unlike traditional AI assistants, **ZombieBreaker** acts as a guardian for your clipboard. When you attempt to paste code from an external source (StackOverflow, documentation, LLMs, etc.), the plugin intercepts the action and challenges you:

1.  **Smart Context Awareness:** The plugin analyzes the current file where you intend to paste the code to understand the environment (existing classes, variables, and logic).
2.  **Local AI Analysis:** It uses local power to process the code sitting in your clipboard.
3.  **Comprehension Check:** You are required to explain in your own words what that code does.
4.  **Verification:** The plugin compares your explanation with its own real-time analysis. If your understanding is correct, the code is pasted. If not, it helps you understand it better before allowing the insertion.

---

## 🚀 Prerequisites

To enable local code analysis, you must set up **Ollama** on your machine:

1.  **Install Ollama:** Download it from [https://ollama.com/](https://ollama.com/).
2.  **Download Gemma 4:** Open your terminal and run the following command:
    ```bash
    ollama run gemma4
    ```
    *(Note: Ensure Ollama is running in the background while you are using Android Studio).*

---

## 🛠️ Installation

Currently, the plugin is distributed as a release file for manual installation:

1.  **Download the Release:** Go to the [Releases](https://github.com/your-username/zombiebreaker/releases) tab and download the latest `.zip` file.
2.  **Install in Android Studio:**
    * Open **Android Studio**.
    * Go to `File` > `Settings` (on macOS: `Android Studio` > `Settings`).
    * Select the **Plugins** section.
    * Click the gear icon (⚙️) and select **Install Plugin from Disk...**.
    * Locate and select the downloaded `.zip` file.
3.  **Restart:** Restart the IDE to activate the ZombieBreaker protection.

---

## 📖 How to Use

Once installed, the plugin monitors the "Paste" action (`Ctrl+V` or `Cmd+V`):

1.  Copy any external code snippet.
2.  When you try to paste it into your Android Studio file, the **ZombieBreaker** window will appear.
3.  Review the context analysis performed by the plugin.
4.  Write a brief explanation of what you think the code does.
5.  Once validated, the code will be successfully integrated into your file.

---

## 🔒 Privacy & Total Security

ZombieBreaker is built for professional environments where security is paramount:

* **100% Local Processing:** Thanks to **Ollama** and **Gemma 4**, neither your project's code nor the code you paste ever leaves your machine.
* **No Cloud:** It requires no internet connection or third-party subscriptions.
* **Privacy by Design:** Perfect for working on confidential or corporate projects where data leakage is not an option.

---
*ZombieBreaker - Because programming is about understanding, not just copying.*
