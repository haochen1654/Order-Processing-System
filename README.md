# Order Processing System - Simulation Challenge

A high-performance Java simulation designed to model a kitchen's order placement and pickup lifecycle. This system utilizes concurrent scheduled executors to manage real-time event spacing and staggered courier arrivals.

## Features
* **Rate-Limited Placement:** Uses a `SingleThreadScheduledExecutor` to ensure orders are placed at a strict, predictable cadence.
* **Parallel Pickup Simulation:** Leverages a `ScheduledThreadPool` to simulate multiple couriers arriving independently at random intervals.
* **Thread Safety:** Implements `CountDownLatch` for precise synchronization, ensuring the final report is generated only after all asynchronous tasks complete.
* **JSON Integration:** Robust parsing and serialization using Jackson Databind.

## Prerequisites
* **Java 17** or higher
* **Maven** (for dependency management)
* **Curl** (for API submission)

## Installation & Setup
1. Clone the repository:
    ```bash
   git clone [https://github.com/haochen1654/Order-Processing-System.git](https://github.com/haochen1654/Order-Processing-System.git)
   cd Order-Processing-System

2. Build the project:
    ```bash
    mvn clean package
## How to Run
1. Generates a seeded-random test problem.
    ```bash
    https://api.cloudkitchens.com/interview/challenge/new?auth=<your token>&seed=<random int>
2. Store the output in a new `orders.json` file
3. Get `x-test-id` from the Response Header
4. Run the JAR with parameters The JAR accepts four parameters: `RATE` (microseconds), `MIN_PICKUP`(microseconds), `MAX_PICKUP`(microseconds), and the `INPUT_FILE`.
    ```bash
    java -jar target\OrderProcessingSystem-1.0.0-jar-with-dependencies.jar 500000 4000000 8000000 orders.json
5. Simulation output will be written in the file data.json
6. Send output to Challenge server and validate.
    ```bash
    curl -X POST "https://api.cloudkitchens.com/interview/challenge/solve?auth=<your token>&seed=<random int>" -H "Content-Type: application/json" -H "x-test-id: <test id>" --data "@data.json"