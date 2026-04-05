# LLD Interview Notes: Elevator System (Java)

[Prettier Notes](https://htmlpreview.github.io/?https://github.com/shivangisehgal/resources/blob/main/LLD/Problems/01-Elevator-System-Design/prettier.html)

## 0. The Golden Rule

**START THINKING FROM THE CLIENT'S PERSPECTIVE.**

Don't jump into classes. First ask: *what does the user actually do?* Trace a user story end-to-end. Every class you design should exist because a user action demanded it.

**User flow:**
```
User presses UP/DOWN on floor 3 (outside elevator)
  → External Request (direction, src floor)
  → System picks which elevator to send
  → User enters, presses floor 7
  → Internal Request (dest floor, elevator ID)
  → Elevator moves to floor 7
```

**Two types of requests — this split drives the whole design:**
- **External Request**: from *outside* the elevator. Needs: `(direction, srcFloor)`
- **Internal Request**: from *inside* the elevator. Needs: `(destFloor, elevatorId)`

---

## I. Requirements to Clarify First

Always spend 2–3 minutes asking these before designing anything:

1. Multiple elevators — how many? Are they identical?
2. Who are the customers — building system, simulation, API?
3. Kinds of elevators — Passenger, Service, Private?
4. Capacity — weight or person limit?
5. Range of floors — basement? zone restrictions?
6. Movement algorithm — LOOK, SCAN, FCFS, Shortest Seek Time? Swappable?
7. Status tracking needed — IDLE, MOVING, NOT_WORKING?

> **Note from class:** Things like fan, light, buttons inside the elevator are out of scope here. Focus on movement logic and request routing.

---

## II. Start from `main()`

Write a fake `main()` before touching any internals. It forces you to define your public API top-down.

```java
public static void main(String[] args) {
    // 1. Get the singleton
    ElevatorSystem system = ElevatorSystem.getElevatorSystem();

    // 2. Initialize
    system.initialiseElevatorSystem(10, 3); // 10 floors, 3 elevators

    // 3. Set elevator selection algorithm
    system.setElevatorSelectionStrategy(new OddEvenElevatorSelStrategy());

    // 4. External request: someone on floor 3 wants to go UP
    system.sendExternalRequest(ELEVATOR_DIRECTION.UP, 3);

    // 5. Internal request: inside elevator #2, go to floor 7
    system.sendInternalRequest(7, 2);
}
```

From this alone you now know: ElevatorSystem is a Singleton, it has an initialise method, a setAlgo method, and two send-request methods — without designing a single internal class yet.

---

## III. ENUMs — Always Use Them in LLD

Never use raw strings or ints for fixed states.

```java
enum ELEVATOR_DIRECTION { UP, DOWN }
enum ELEVATOR_STATUS    { IDLE, MOVING, NOT_WORKING }
```

---

## IV. Classes and What They Own

### ElevatorSystem *(Singleton — the façade)*
- Fields: `elevatorSystemInstance`, `extReqProcessor`, `intReqProcessor`
- Methods: `getElevatorSystem()`, `initialiseElevatorSystem()`, `setElevatorSelectionStrategy()`, `sendExternalRequest()`, `sendInternalRequest()`
- The client talks ONLY to this class. It delegates everything to processors.

### ElevatorMgr *(Singleton — stores all elevators)*
- Fields: `elevatorMgrInstance`, `Map<ID, Elevator> elevators`
- Methods: `getElevatorMgr()`, `initialiseElevators(n)`, `getElevator(id)`
- Answers the question: *"where do all the elevators live?"* — NOT in ElevatorSystem. ElevatorSystem has an ElevatorMgr. Elevators are added to the map during `initialiseElevators()`.

### ExternalRequestsProcessor
- Fields: `ElevatorSelectionStrategy elevatorSelectionStrategy`
- Methods: `processExternalRequest(ExternalRequest req)`
- Receives (direction, srcFloor). Uses its strategy to pick *which* elevator. Tells that elevator to move to srcFloor.

### InternalRequestsProcessor
- Methods: `processInternalRequest(InternalRequest req)`
- Receives (destFloor, elevatorId). Finds that exact elevator, tells it to move.

### ExternalRequest / InternalRequest *(dumb data containers)*
- ExternalRequest: `ELEVATOR_DIRECTION directionToGo`, `int srcFloor`
- InternalRequest: `int destFloor`, `int srcElevatorId`

### Elevator *(the physical box)*
- Fields: `elevatorId`, `capacity`, `ElevatorController controller`, `List<InternalDisplay> displays`
- Elevator does NOT decide where to go — it delegates to ElevatorController. Elevator = the box. Controller = the brain.

### ElevatorController *(one per elevator — the movement brain)*
- Fields: `ElevatorControlStrategy controlStrategy`, `ElevatorCurrState state`
- Methods: `moveElevatorToFloor(int floorNum)`
- Responsible for the elevator's movement. Tells hardware to move. Uses its strategy to figure out the next stop.

### ElevatorState *(snapshot of current condition)*
- Fields: `ELEVATOR_DIRECTION currDirection`, `ELEVATOR_STATUS currStatus`, `int currFloor`
- Used by strategies to make decisions.

### Sensor *(Observer pattern — detects floor changes)*
- Fields: `List<Elevator> elevators`, `int floorNum`
- Methods: `detectFloor()`, `addElevator()`, `removeElevator()`, `notifyElevators(floorNum)`

---

## V. The Two Strategy Layers

This is the most important design decision. Two completely separate Strategy patterns exist.

### Layer 1: Which elevator should respond? → `ElevatorSelectionStrategy`

Held by `ExternalRequestsProcessor`. Called when an external request arrives.

```
interface ElevatorSelectionStrategy {
    int selectElevator(ExternalRequest req);
}
```

Implementations:
- `OddEvenElevatorSelStrategy` — odd src floor → odd elevator, even → even
- `ZoneElevatorSelStrategy` — floors 1–5 → zone A elevator, 6–10 → zone B

### Layer 2: What floor to stop at next? → `ElevatorControlStrategy`

Held by `ElevatorController`. Called when the elevator is moving.

```
interface ElevatorControlStrategy {
    int determineNextStop(int floorNum);
}
```

Implementations:
- `LookAlgoElevatorControlStrategy` — go in one direction until no more requests, then reverse
- `ScanAlgorithmElevatorControlStrategy` — go end-to-end like a disk scan
- `ShortestSeekTimeElevatorControlStrategy` — go to whichever pending floor is closest
- `FirstComeFirstServeElevatorControlStrategy` — process requests in arrival order

> From your notes: *"Option to choose — you can change it whenever you want, once a year, month, etc."* — this is exactly why Strategy pattern fits. Both layers are swappable at runtime.

---

## VI. Elevator Inheritance

```
Elevator (abstract)
    ↑
    ├── PassengerElevator
    ├── ServiceElevator
    └── PrivateElevator
```

> **Factory pattern?** Your notes flag this as "overkill" for this scope. Unless the interviewer asks, just instantiate the right subclass inside `initialiseElevators()`. Don't over-engineer.

---

## VII. Design Patterns Summary

| Pattern | Where Used | Why |
|---|---|---|
| **Singleton** | ElevatorSystem, ElevatorMgr | Only one building-level system should exist |
| **Strategy** | ElevatorSelectionStrategy | Which elevator to pick — swappable per building policy |
| **Strategy** | ElevatorControlStrategy | Which floor next — swappable per scheduling algo |
| **Observer** | Sensor → InternalDisplay | Floor changes need to notify all displays |
| **State** | ElevatorState | Encapsulate current condition separately from behavior |

---

## VIII. End-to-End Request Flow

### External Request (button pressed outside elevator)
1. User presses UP on floor 3 → `ExternalRequest(UP, srcFloor=3)` created
2. `ElevatorSystem.sendExternalRequest()` → delegates to `ExternalRequestsProcessor`
3. ERP calls `elevatorSelectionStrategy.selectElevator(req)` → returns elevator ID 2
4. ERP asks `ElevatorMgr.getElevator(2)` → gets Elevator #2
5. Calls `elevator.controller.moveElevatorToFloor(3)` → controller uses its `ElevatorControlStrategy`

### Internal Request (button pressed inside elevator)
1. User inside elevator #2 presses floor 7 → `InternalRequest(destFloor=7, elevatorId=2)`
2. `ElevatorSystem.sendInternalRequest()` → delegates to `InternalRequestsProcessor`
3. IRP gets elevator #2 from ElevatorMgr → calls `controller.moveElevatorToFloor(7)`

---

## IX. Interview Execution Tips

- **Clarify requirements first** — spend 2–3 minutes. Interviewers reward this.
- **Trace user flow before classes** — say "a user presses UP on floor 3, let me trace what happens."
- **Write fake main() early** — defines your public API before internals. Shows top-down thinking.
- **Name patterns out loud** — "I'm using Strategy here because the algorithm needs to be swappable." Pattern awareness is a green flag.
- **Know when NOT to add a pattern** — "Factory could work but it's overkill here." Deliberate simplicity shows seniority.
- **Always use ENUMs** for fixed states. Never magic strings or ints.
- **Separate the two strategy concerns** — selection (which elevator) vs control (which floor next) are different responsibilities. Don't merge them.
- **Justify Singleton** — "Two instances of ElevatorSystem would cause inconsistent state across the building." Always explain *why*, not just *what*.

---

## X. Cheat Sheet

**Classes:** ElevatorSystem · ElevatorMgr · ExternalRequestsProcessor · InternalRequestsProcessor · ExternalRequest · InternalRequest · Elevator · ElevatorController · ElevatorState · ElevatorSelectionStrategy · ElevatorControlStrategy · Sensor

**5-step playbook:**
1. Clarify requirements (2 min)
2. Trace user flow (1 min)
3. Write fake main() (2 min)
4. Identify classes + patterns (5 min)
5. Code the most important classes (remaining time)
