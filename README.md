# SmartParking
Intelligent Parking lOT Management System (BUPT/QMUL Internet of Things Engineering Practices) (Group 10)

## Project Overview

This project is an **Intelligent Parking IoT Management System** developed by **Group 10** for the IoT Engineering Practice course. It integrates **Wireless Sensor Networks (WSN)** and **RFID technology** to create a smart parking solution that handles real-time monitoring, automated entry/exit billing, and security risk control.

The system features a **PC-side Java Swing application** that acts as the edge computing node, processing sensor data locally to ensure reliability even in offline network conditions. Additionally, it integrates **Generative AI (DeepSeek)** and Weather APIs to provide intelligent travel advice to drivers upon departure.

## Key Features

### 1. Smart Parking Management

* **Real-time Visualization:** A graphical dashboard displays the status of all parking slots in real-time.
* **Three-State Logic:** Slots are categorized as **Free** (Green), **Occupied** (Yellow), or **Offline** (Gray) based on sensor heartbeat and illuminance thresholds.
* **Hardware Integration:** Uses WSN nodes (Lux sensors) to detect vehicle presence and UHF RFID readers for vehicle identification.



### 2. Dual-Source Anti-Ticket Evasion

* **Consistency Check:** The system compares physical occupancy (WSN data) against digital entry records (RFID data).
* **Automatic Alarming:** If the number of physically occupied spots exceeds the number of valid RFID entries, the system triggers a "Ticket Evasion/Illegal Parking" alarm.



### 3. Offline-First Billing (In-Card Data Loop)

* **Resilient Architecture:** Critical billing data (Entry Time, Fees) is written directly into the **USER memory** of the RFID card.


* **No Database Dependency:** The exit workflow reads the card to calculate fees and write back exit timestamps, allowing the system to function even if the backend server or network is down.



### 4. AI Travel Concierge

* **Context-Aware Advice:** Upon exit payment, the system fetches real-time weather (via **Amap/Gaode API**) and sends a prompt to the **DeepSeek LLM**.
* **Smart Interactions:** Provides drivers with personalized safety tips and greetings based on current weather conditions (e.g., "It's raining, drive carefully").



---

## System Architecture

The project follows a four-layer architecture:

1. **Perception Layer:** WSN Light Sensors & UHF RFID Readers.
2. **Transmission Layer:** Serial Communication (ZigBee Coordinator to PC).
3. **Platform Layer (Edge):** Java Application handling state machines, billing logic, and risk control.
4. **Application Layer:** Java Swing UI for operators.

---

## Technology Stack

* **Language:** Java (JDK 8+)
* **GUI Framework:** Java Swing
* **Hardware Communication:** `gnu.io` (RXTX) for Serial Port Management.


* **External APIs:**
* **DeepSeek API** (LLM for travel advice)
* **Amap (Gaode) API** (Weather data)


* **Hardware:** UHF RFID Reader/Writer, ZigBee WSN Nodes.

---

## Getting Started

### Prerequisites

* **Java Development Kit (JDK):** Version 8 or higher.
* **RXTX Library:** You must have the `rxtxParallel.dll` and `rxtxSerial.dll` (Windows) installed in your Java library path to communicate with COM ports.
* **Hardware:**
* Connect the WSN Coordinator to a USB port.
* Connect the UHF RFID Reader to a USB port.



### Configuration

Before running the code, update the `MainFrame.java` file with your specific environment settings:

1. **API Keys:**
Replace the placeholder keys with your valid API credentials:
```java
// In MainFrame.java
private static final String DEEPSEEK_API_KEY = "YOUR_DEEPSEEK_KEY";
private static final String AMAP_KEY = "YOUR_AMAP_KEY";

```


2. **MAC Addresses:**
The system maps specific hardware MAC addresses to UI slots. Update `initSlots()` with your device MACs:
```java
// In MainFrame.java
macList[0] = "58D1E107004B1200"; // Replace with your Sensor MAC

```


3. **Business Parameters:**
You can adjust pricing and thresholds:
```java
private static final double LUX_THRESHOLD = 500; // Light sensitivity
private static final double PRICE_PER_MIN = 0.5; // Fee per minute

```



### How to Run

1. Compile the project ensuring `RXTXcomm.jar` is in your classpath.
2. Run `MainFrame.java` as the entry point.
3. **In the UI:**
* Select the correct **COM Port** for the WSN Coordinator and click "Open Serial Port".
* Select the correct **COM Port** for the RFID Reader and click "Connect RFID".
* The system will now display real-time slot status and accept RFID card swipes for Entry/Exit.



---

## Project Structure

* `MainFrame.java`: The core controller. Handles the UI lifecycle, serial port events, API calls, and business logic (Anti-evasion, Billing).


* `ParkingLotPanel.java`: A custom Swing component that draws the parking grid and handles the color-coded status visualization (Green/Yellow/Gray).


* `UhfReaderService.java`: Service class for interacting with the UHF RFID hardware (Init, Read, Write).


* `ReadResult.java`: Data model for RFID read operations.

---

## Contributors (Group 10)

* **Guo YiXuan** (2022213459)
* **Chen Wei** (2022213461)
* **Sun HaoRan** (2022213462)

------

## License

This project is licensed under the GNU General Public License v3.0 (GPL-3.0).
See the [LICENSE](./LICENSE) file for details.

------

## Contact

- **Email:** [jp2022213462@qmul.ac.uk](mailto:jp2022213462@qmul.ac.uk)
- **Phone:** +86 157 2662 1095
- **Website:** https://github.com/Keeper0824/EBU6304_Group58

------

*Beijing University of Posts and Telecommunications (BUPT) - December 2025*.
