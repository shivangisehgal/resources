# Low Level Design (LLD) — Interview Approach Notes

### Types of Interview Rounds

| Type | Focus |
|---|---|
| 1. Whiteboard / Pen-Paper | UML diagrams, Relationships, Design Patterns (no actual code) |
| 2. a) Broader systems (e.g., Swiggy, Uber, Ola) | Focus more on Requirements gathering + UML |
| 2. b) Std. probelms (Elevator, Chess, Tic-Tac-Toe type problems) | Focus more on Sure shot working code, don't waste much time reqs.

### Time Breakdown (Typical 1-hour Interview)
- **5–10 min** → Requirements / Module clarification
- **20 min** → UML, Classes
- **30 min** → Code (1 code → run)
- **10–15 min** → Testing / Review / Revision

> For shorter sessions (~1hr total):
> - 5 min → Requirements
> - ~35–40 min → Classes + Code

---

## 3. LLD Steps / Process

```
Requirements → UML (Classes, Attributes) → SOLID Principles → Design Patterns → Code → Algo/DSA → Test Cases
```

### Requirements Phase
- Identify **Actors / Users**
- Map **Sequence of Events / Actions**
- May include a **Diagram** (use-case level)

---

## 4. Use Case 1: Swiggy / Zomato (Food Delivery System)

### Requirement Gathering — Actors

| Actor | Role |
|---|---|
| Client | Customer placing orders |
| Delivery Partner (DP) | Picks up and delivers food |
| Restaurant Owner (RO) | Manages menu and orders |
| Admin / System | Platform management |
| Support | Customer/partner support |

### Common (tell interviewer you are not focussing on this)
- Login / Auth
- Logging & Monitoring
- Notifications - User ← SMS / Email / In-App (triggered from system)

### Client Flow (Sequence of Actions)
1. **Search** — by Cuisine / Restaurant / Dish / Location
2. **Select & Add to Cart**
3. **Payment**
4. **Track Order**

### Order Status (Restaurant Side)
- Accepted → Preparing → Ready → Pickup → Arrived

### Delivery Partner (DP) Flow (Sequence of Actions)
1. Accept Order
2. Go to Restaurant (Pick up)
3. Arrive / Pickup / On the way (Change Status)
4. Reach / Deliver

### Restaurant Owner (RO) Flow (Sequence of Actions)
- Add Menu — Dish, Price
- Cuisine — Veg / Non-Veg
- Address
- Status — Preparing / Ready / Accept


### System Architecture (High-Level Mental Model)

```
Client
  └──→ [ Swiggy System ]
              ├── Delivery Mgr  ──→ [DB]
              │     └── (Swiggy System internal)
              └── Order Mgr    ──→ [DB]
```

Design patterns involved (noted by instructor):
- **Singleton**
- **Strategy**

<img width="554" height="605" center alt="image" src="https://github.com/user-attachments/assets/ff0e64a7-8558-4a29-9e05-1dfeee37999c" />

---

## 5. Use Case 2: Zerodha / Stock Broker System

### Actors
- **Customer** — places buy/sell orders
- **Stock Broker System** — processes trades
- **BSE / NSE** — external exchange

### Customer Actions
- View **Watchlist** (List of stocks)
- **Buy** (Qty, Rate)
- **Sell** (Qty, Rate)
- View **Fav Stocks**

### System Flow (Mental Model)

```
Customer
  ├── Watchlist / Fav Stocks  ──→ [ Broker System ]  ──→ BSE/NSE
  ├── Buy (Qty, Rate)         ──→
  └── Sell (Qty, Rate)        ──→
```

<img width="534" height="219" alt="image" src="https://github.com/user-attachments/assets/23cb85fc-9d93-4068-8a53-2ca0eae9973e" />
